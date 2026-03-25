package cn.szu.blankxiao.scanguide.guideball.renderer

import android.opengl.GLES20
import android.opengl.Matrix
import cn.szu.blankxiao.scanguide.guideball.gl.ShaderUtil
import cn.szu.blankxiao.scanguide.guideball.gl.SphereGeometry
import cn.szu.blankxiao.scanguide.guideball.orientation.GyroOrientationProvider
import kotlin.math.sqrt

/**
 * 透视 + 球体固定于原点；陀螺仪驱动**相机绕球轨道**（相对首帧姿态），轮廓与明暗随视角明显变化。
 */
internal class SphereGuideRenderer(
	private val orientation: GyroOrientationProvider
) : GuideBallRenderer {

	private var width = 1
	private var height = 1
	private var program = 0
	private var mesh: SphereGeometry.Mesh? = null

	private val projectionMatrix = FloatArray(16)
	private val viewMatrix = FloatArray(16)
	private val modelMatrix = FloatArray(16)
	private val mvpMatrix = FloatArray(16)
	private val relMatrix = FloatArray(16)
	private val eyeHomogeneous = FloatArray(4)
	private val upHomogeneous = FloatArray(4)

	override fun onGLContextAvailable() {
		GLES20.glClearColor(0.12f, 0.12f, 0.14f, 1f)
		GLES20.glEnable(GLES20.GL_DEPTH_TEST)
		GLES20.glDepthFunc(GLES20.GL_LEQUAL)
		GLES20.glEnable(GLES20.GL_CULL_FACE)
		GLES20.glCullFace(GLES20.GL_BACK)

		program = ShaderUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
		mesh = SphereGeometry.build(radius = 1f, widthSegments = 36, heightSegments = 18)
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
		orientation.copyRelativeRotation(relMatrix)

		// 默认相机在设备「背后」+Z，姿态矩阵把该偏移旋到世界系 → 绕球观察
		val eyeLocal = floatArrayOf(0f, 0f, ORBIT_RADIUS, 1f)
		Matrix.multiplyMV(eyeHomogeneous, 0, relMatrix, 0, eyeLocal, 0)
		val iw = eyeHomogeneous[3].let { if (kotlin.math.abs(it) > 1e-6f) it else 1f }
		val ex = eyeHomogeneous[0] / iw
		val ey = eyeHomogeneous[1] / iw
		val ez = eyeHomogeneous[2] / iw

		val upLocal = floatArrayOf(0f, 1f, 0f, 0f)
		Matrix.multiplyMV(upHomogeneous, 0, relMatrix, 0, upLocal, 0)
		val ulen = sqrt(
			(upHomogeneous[0] * upHomogeneous[0] +
				upHomogeneous[1] * upHomogeneous[1] +
				upHomogeneous[2] * upHomogeneous[2]).toDouble()
		).toFloat().coerceAtLeast(1e-6f)
		val ux = upHomogeneous[0] / ulen
		val uy = upHomogeneous[1] / ulen
		val uz = upHomogeneous[2] / ulen

		Matrix.setLookAtM(viewMatrix, 0, ex, ey, ez, 0f, 0f, 0f, ux, uy, uz)

		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

		val uMvp = GLES20.glGetUniformLocation(program, "u_mvpMatrix")
		val uModel = GLES20.glGetUniformLocation(program, "u_modelMatrix")
		GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
		GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)

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

	companion object {
		private const val FOV_DEGREES = 36f
		private const val Z_NEAR = 0.1f
		private const val Z_FAR = 100f
		/** 相机到球心距离（球半径 1），略大以缩小屏上占比 */
		private const val ORBIT_RADIUS = 6.8f
		private const val BYTES_FLOAT = 4

		private const val VERTEX_SHADER = """
			uniform mat4 u_mvpMatrix;
			uniform mat4 u_modelMatrix;
			attribute vec4 a_position;
			attribute vec3 a_normal;
			varying vec3 v_n;
			void main() {
				vec3 nw = mat3(u_modelMatrix) * a_normal;
				v_n = normalize(nw);
				gl_Position = u_mvpMatrix * a_position;
			}
		"""

		private const val FRAGMENT_SHADER = """
			precision mediump float;
			varying vec3 v_n;
			void main() {
				vec3 L = normalize(vec3(0.35, 0.55, 0.75));
				float d = max(dot(normalize(v_n), L), 0.0) * 0.55 + 0.38;
				float theta = atan(v_n.x, v_n.z);
				float lines = abs(sin(theta * 20.0));
				float accent = smoothstep(0.82, 1.0, lines) * 0.18;
				d = clamp(d + accent, 0.0, 1.0);
				gl_FragColor = vec4(vec3(d), 1.0);
			}
		"""
	}
}
