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
		// eye: 相机位置（球外侧，由 forward 反向推导）
		// forward: 从球心向外的方向向量
		// up: 世界坐标系上方向
		orientationProvider.getCameraFrame(eye, forward, up)

		// 相机应该看向球心 (0,0,0)
		// 因为 eye 已经在 -forward * radius 的位置
		// 所以看向球心就是看向原点
		val target = floatArrayOf(0f, 0f, 0f)

		// 重建视图矩阵：从 eye 看向球心
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
