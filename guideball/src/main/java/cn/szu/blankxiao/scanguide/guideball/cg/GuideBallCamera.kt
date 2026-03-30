package cn.szu.blankxiao.scanguide.guideball.cg

import android.opengl.Matrix
import kotlin.math.tan

/**
 * GuideBall 相机类
 * 参考 MyPanorama 的 Camera 实现
 * 封装投影矩阵和视图矩阵
 */
class GuideBallCamera(val screenRatio: Float) {

	// MVP 矩阵
	private val mvpMatrix = FloatArray(16)

	// 投影矩阵
	private val projectionMatrix = FloatArray(16)

	// 视图矩阵
	private val viewMatrix = FloatArray(16)

	// 当前 FOV 角度（度）
	var currentFov: Float = DEFAULT_FOV
		private set

	init {
		updateProjectionMatrix(DEFAULT_FOV)
	}

	/**
	 * 更新投影矩阵
	 * @param fovDegrees 视场角（度）
	 */
	fun updateProjectionMatrix(fovDegrees: Float) {
		currentFov = fovDegrees.coerceIn(MIN_FOV, MAX_FOV)
		val top = (tan(currentFov * Math.PI / 360.0) * Z_NEAR).toFloat()
		val bottom = -top
		val left = screenRatio * bottom
		val right = screenRatio * top
		Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, Z_NEAR, Z_FAR)
	}

	/**
	 * 重建视图矩阵（使用 lookAt）
	 * @param eye 相机位置
	 * @param center 目标位置
	 * @param up 上方向
	 */
	fun rebuildViewMatrix(eye: FloatArray, center: FloatArray, up: FloatArray) {
		Matrix.setLookAtM(
			viewMatrix, 0,
			eye[0], eye[1], eye[2],
			center[0], center[1], center[2],
			up[0], up[1], up[2]
		)
	}


	/**
	 * 获取 MVP 矩阵（Projection × View）
	 */
	fun getMVPMatrix(): FloatArray {
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
		return mvpMatrix
	}

	companion object {
		private const val Z_NEAR = 0.1f
		private const val Z_FAR = 100.0f
		const val DEFAULT_FOV = 20.0f      // 适度减小视场角，提升球体有效显示占比
		const val MIN_FOV = 30.0f
		const val MAX_FOV = 120.0f
		const val DEFAULT_ORBIT_RADIUS = 6.0f  // 增大轨道半径，相机离球体更远
	}
}
