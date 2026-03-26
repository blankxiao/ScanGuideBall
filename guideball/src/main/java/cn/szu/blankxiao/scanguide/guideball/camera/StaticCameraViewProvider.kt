package cn.szu.blankxiao.scanguide.guideball.camera

/**
 * 固定视角（无陀螺仪）：相机在 +Y 外、朝向原点。
 *
 * 世界坐标系定义：
 * - +X = 右手边
 * - +Y = 人脸面向的方向（相机 forward 方向，看向 -Y 方向球心）
 * - +Z = 与重力相反方向（向上）
 *
 * 宿主接入后应换用 [HostCameraFrameProvider] 或自实现 [CameraViewProvider]。
 */
class StaticCameraViewProvider : CameraViewProvider {

	override fun getCameraFrame(eyeOut: FloatArray, forwardOut: FloatArray, upOut: FloatArray) {
		// 相机位置：在 +Y 方向，距离原点 DEFAULT_ORBIT_RADIUS
		eyeOut[0] = 0f
		eyeOut[1] = DEFAULT_ORBIT_RADIUS
		eyeOut[2] = 0f
		// 相机 forward：看向 -Y 方向（球心）
		forwardOut[0] = 0f
		forwardOut[1] = -1f
		forwardOut[2] = 0f
		// 相机 up：+Z 方向（向上）
		upOut[0] = 0f
		upOut[1] = 0f
		upOut[2] = 1f
	}

	companion object {
		const val DEFAULT_ORBIT_RADIUS = 6.8f
	}
}
