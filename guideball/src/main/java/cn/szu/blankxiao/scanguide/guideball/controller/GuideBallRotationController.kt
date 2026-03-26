package cn.szu.blankxiao.scanguide.guideball.controller

import cn.szu.blankxiao.scanguide.guideball.camera.AccelOnlyOrientationProvider
import cn.szu.blankxiao.scanguide.guideball.cg.GuideBallCamera

/**
 * GuideBall 旋转控制器
 * 负责将传感器数据应用到相机视图矩阵
 */
class GuideBallRotationController(
	private val orientationProvider: AccelOnlyOrientationProvider
) {

	// 临时数组，避免每帧分配内存
	private val eye = FloatArray(3)
	private val forward = FloatArray(3)
	private val up = FloatArray(3)

	/**
	 * 更新相机视图
	 * 每帧调用，从传感器获取数据并应用到相机
	 */
	fun updateCameraView(camera: GuideBallCamera) {
		// 获取传感器数据
		orientationProvider.getCameraFrame(eye, forward, up)

		// 目标点 = eye + forward * distance
		val target = FloatArray(3)
		target[0] = eye[0] + forward[0]
		target[1] = eye[1] + forward[1]
		target[2] = eye[2] + forward[2]

		// 重建视图矩阵
		camera.rebuildViewMatrix(eye, target, up)
	}

	/**
	 * 生命周期：开始监听传感器
	 */
	fun onAttached() {
		orientationProvider.onAttachedToWindow()
	}

	/**
	 * 生命周期：停止监听传感器
	 */
	fun onDetached() {
		orientationProvider.onDetachedFromWindow()
	}
}
