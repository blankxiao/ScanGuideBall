package cn.szu.blankxiao.scanguide.guideball.renderer

import android.opengl.GLES20
import android.opengl.Matrix
import cn.szu.blankxiao.scanguide.guideball.bridge.OrbitDirectionBridge
import cn.szu.blankxiao.scanguide.guideball.camera.CameraViewProvider
import cn.szu.blankxiao.scanguide.guideball.domain.SphereScanState
import cn.szu.blankxiao.scanguide.guideball.gl.ShaderUtil
import cn.szu.blankxiao.scanguide.guideball.gl.SphereGeometry
import kotlin.math.sqrt

/**
 * 透视 + [CameraViewProvider]；恒定底色 + 数学计算的球面网格点阵。
 * 直接在片元着色器中用数学方式计算网格点位置，无纹理扭曲，整齐排列。
 */
internal class SphereGuideRenderer(
	private val cameraViewProvider: CameraViewProvider,
	private val scanState: SphereScanState,
	private val directionBridge: OrbitDirectionBridge
) : GuideBallRenderer {

	private var width = 1
	private var height = 1
	private var program = 0
	private var mesh: SphereGeometry.Mesh? = null

	private val projectionMatrix = FloatArray(16)
	private val viewMatrix = FloatArray(16)
	private val modelMatrix = FloatArray(16)
	private val mvpMatrix = FloatArray(16)
	private val eyePos = FloatArray(3)
	private val forwardWorld = FloatArray(3)
	private val upWorld = FloatArray(3)
	private val centerToCamera = FloatArray(3)

	override fun onGLContextAvailable() {
		GLES20.glClearColor(0.12f, 0.12f, 0.14f, 1f)
		GLES20.glEnable(GLES20.GL_DEPTH_TEST)
		GLES20.glDepthFunc(GLES20.GL_LEQUAL)
		GLES20.glEnable(GLES20.GL_CULL_FACE)
		GLES20.glCullFace(GLES20.GL_BACK)

		program = ShaderUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
		mesh = SphereGeometry.build(radius = 1f, widthSegments = 64, heightSegments = 32)
	}

	override fun onSurfaceChanged(width: Int, height: Int) {
		this.width = width.coerceAtLeast(1)
		this.height = height.coerceAtLeast(1)
		val ratio = this.width.toFloat() / this.height
		Matrix.perspectiveM(projectionMatrix, 0, FOV_DEGREES, ratio, Z_NEAR, Z_FAR)
	}

	override fun onDrawFrame() {
		GLES20.glViewport(0, 0, width, height)
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

		val m = mesh ?: return
		GLES20.glUseProgram(program)

		Matrix.setIdentityM(modelMatrix, 0)

		cameraViewProvider.getCameraFrame(eyePos, forwardWorld, upWorld)
		normalize3(forwardWorld)
		normalize3(upWorld)

		val elen = length3(eyePos)
		if (elen > 1e-5f) {
			centerToCamera[0] = eyePos[0] / elen
			centerToCamera[1] = eyePos[1] / elen
			centerToCamera[2] = eyePos[2] / elen
		} else {
			centerToCamera[0] = 0f
			centerToCamera[1] = 0f
			centerToCamera[2] = 1f
		}
		directionBridge.setDirectionUnit(centerToCamera[0], centerToCamera[1], centerToCamera[2])

		Matrix.setLookAtM(
			viewMatrix, 0,
			eyePos[0], eyePos[1], eyePos[2],
			0f, 0f, 0f,
			upWorld[0], upWorld[1], upWorld[2]
		)

		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

		// 上传矩阵
		val uMvp = GLES20.glGetUniformLocation(program, "u_mvpMatrix")
		val uModel = GLES20.glGetUniformLocation(program, "u_modelMatrix")
		val uView = GLES20.glGetUniformLocation(program, "u_viewMatrix")
		GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
		GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
		GLES20.glUniformMatrix4fv(uView, 1, false, viewMatrix, 0)

		// 网格参数
		val uGridCols = GLES20.glGetUniformLocation(program, "u_gridCols")
		val uGridRows = GLES20.glGetUniformLocation(program, "u_gridRows")
		val uDotRadius = GLES20.glGetUniformLocation(program, "u_dotRadius")
		GLES20.glUniform1i(uGridCols, GRID_COLS)
		GLES20.glUniform1i(uGridRows, GRID_ROWS)
		GLES20.glUniform1f(uDotRadius, DOT_RADIUS)

		// 绘制
		val stride = m.floatsPerVertex * BYTES_FLOAT
		m.vertexBuffer.position(0)
		GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, stride, m.vertexBuffer)
		GLES20.glEnableVertexAttribArray(0)
		m.vertexBuffer.position(3)
		GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, stride, m.vertexBuffer)
		GLES20.glEnableVertexAttribArray(1)

		m.indexBuffer.position(0)
		GLES20.glDrawElements(
			GLES20.GL_TRIANGLES,
			m.indexCount,
			GLES20.GL_UNSIGNED_SHORT,
			m.indexBuffer
		)

		GLES20.glDisableVertexAttribArray(0)
		GLES20.glDisableVertexAttribArray(1)
	}

	private fun normalize3(v: FloatArray) {
		val len = length3(v)
		if (len > 1e-8f) {
			v[0] /= len
			v[1] /= len
			v[2] /= len
		}
	}

	private fun length3(v: FloatArray): Float {
		return sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()).toFloat()
	}


	companion object {
		private const val FOV_DEGREES = 36f
		private const val Z_NEAR = 0.1f
		private const val Z_FAR = 100f
		private const val BYTES_FLOAT = 4

		// 网格参数 - 调整这些值来改变点阵密度和清晰度
		private const val GRID_COLS = 14     // 经度方向列数
		private const val GRID_ROWS = 12      // 纬度方向行数
		private const val DOT_RADIUS = 0.028f // 点的大小（适当减小避免重叠）

		private const val VERTEX_SHADER = """
			uniform mat4 u_mvpMatrix;
			uniform mat4 u_modelMatrix;
			uniform mat4 u_viewMatrix;
			attribute vec4 a_position;
			attribute vec3 a_normal;
			varying vec3 v_nWorld;
			varying vec3 v_worldPos;
			varying vec3 v_viewDir;
			void main() {
				vec4 worldPos = u_modelMatrix * a_position;
				v_worldPos = worldPos.xyz;
				vec3 nw = normalize(mat3(u_modelMatrix) * a_normal);
				v_nWorld = nw;
				// 视线方向（从相机指向顶点）
				vec4 camPos = vec4(0.0, 0.0, 0.0, 1.0);
				v_viewDir = normalize(v_worldPos - camPos.xyz);
				gl_Position = u_mvpMatrix * a_position;
			}
		"""

		private const val FRAGMENT_SHADER = """
			precision mediump float;
			uniform int u_gridCols;
			uniform int u_gridRows;
			uniform float u_dotRadius;
			varying vec3 v_nWorld;
			varying vec3 v_worldPos;
			varying vec3 v_viewDir;

			// 将球面坐标转换为 3D 单位向量
			vec3 sphericalToCartesian(float lon, float lat) {
				float cosLat = cos(lat);
				return vec3(
					cosLat * sin(lon),
					sin(lat),
					cosLat * cos(lon)
				);
			}

			// 找到最近的网格点并返回其在球面上的法线
			vec3 findNearestGridPoint(vec3 normal, int cols, int rows) {
				// 从法线计算经纬度
				float lon = atan(normal.x, normal.z);
				float lat = asin(clamp(normal.y, -1.0, 1.0));

				// 计算网格步长
				float lonStep = 2.0 * 3.14159265 / float(cols);
				float latStep = 3.14159265 / float(rows + 1);

				// 纬度起始偏移（避开两极）
				float latStart = -0.5 * 3.14159265 + latStep;

				// 找到最近的网格点
				float minAngleDist = 1000.0;
				vec3 nearestPoint = vec3(0.0, 1.0, 0.0);

				// 只在纬度范围内搜索
				for (int row = 0; row < 20; row++) {
					if (row >= rows) break;

					float gridLat = latStart + float(row) * latStep;
					float cosLat = cos(gridLat);
					if (cosLat < 0.01) continue;

					for (int col = 0; col < 20; col++) {
						if (col >= cols) break;

						float gridLon = float(col) * lonStep;
						vec3 gridPoint = sphericalToCartesian(gridLon, gridLat);

						float angleDist = acos(clamp(dot(normal, gridPoint), -1.0, 1.0));
						if (angleDist < minAngleDist) {
							minAngleDist = angleDist;
							nearestPoint = gridPoint;
						}
					}
				}

				return nearestPoint;
			}

			// 计算在网格点切平面上的投影距离（屏幕空间一致的圆）
			float calcTangentPlaneDistance(vec3 worldPos, vec3 gridNormal, float radius) {
				// 网格点位置（球面上半径=1）
				vec3 gridPos = gridNormal;

				// 从网格点指向当前片元的向量
				vec3 toFragment = worldPos - gridPos;

				// 将向量投影到切平面（减去沿法线的分量）
				vec3 tangentProj = toFragment - dot(toFragment, gridNormal) * gridNormal;

				// 计算切平面上的距离（弧度制，与radius单位一致）
				float dist = length(tangentProj);

				return dist;
			}

			void main() {
				vec3 nw = normalize(v_nWorld);

				// 基础底色
				float base = 0.55;
				float brightness = 0.0;

				// 找到最近的网格点
				vec3 nearestGrid = findNearestGridPoint(nw, u_gridCols, u_gridRows);

				// 使用切线空间距离计算圆形（屏幕一致的圆）
				float tangentDist = calcTangentPlaneDistance(v_worldPos, nearestGrid, u_dotRadius);

				// 绘制圆点
				if (tangentDist < u_dotRadius) {
					float t = tangentDist / u_dotRadius;
					// 锐利边缘的圆
					brightness = pow(1.0 - t, 2.0) * 0.65;

					// 中心高光
					if (t < 0.4) {
						brightness += 0.25 * (1.0 - t / 0.4);
					}
				}

				float d = clamp(base + brightness, 0.0, 1.0);
				gl_FragColor = vec4(vec3(d), 1.0);
			}
		"""
	}
}
