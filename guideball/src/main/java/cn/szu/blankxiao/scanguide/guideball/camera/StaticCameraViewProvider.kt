package cn.szu.blankxiao.scanguide.guideball.camera

/**
 * 固定视角（无传感器）：与 [AccelOnlyOrientationProvider] 中从 [android.hardware.SensorManager] 矩阵提取的约定一致，光轴 **-Z**、up 为屏幕 **+Y**。
 *
 * 宿主接入后应换用 [HostCameraFrameProvider] 或自实现 [CameraViewProvider]。
 */
class StaticCameraViewProvider : CameraViewProvider {

	override fun getCameraFrame(eyeOut: FloatArray, forwardOut: FloatArray, upOut: FloatArray) {
		eyeOut[0] = 0f
		eyeOut[1] = 0f
		eyeOut[2] = DEFAULT_ORBIT_RADIUS
		forwardOut[0] = 0f
		forwardOut[1] = 0f
		forwardOut[2] = -1f
		upOut[0] = 0f
		upOut[1] = 1f
		upOut[2] = 0f
	}

	companion object {
		const val DEFAULT_ORBIT_RADIUS = 6.8f
	}
}
