package cn.szu.blankxiao.scanguide.guideball.camera

/**
 * 宿主注入：在 **相机位姿可用的线程** 每帧调用 [setCameraFrame]，与真实预览/AR 相机一致。
 * GL 线程仅做拷贝，避免与写入并发撕裂。
 */
class HostCameraFrameProvider : CameraViewProvider {

	private val lock = Any()
	private val eye = FloatArray(3)
	private val forward = FloatArray(3)
	private val up = FloatArray(3)

	init {
		StaticCameraViewProvider().getCameraFrame(eye, forward, up)
	}

	/**
	 * 由宿主传入与真实相机一致的帧数据（建议使用世界系、与宿主渲染相同约定）。
	 */
	fun setCameraFrame(eyeX: Float, eyeY: Float, eyeZ: Float, fwX: Float, fwY: Float, fwZ: Float, upX: Float, upY: Float, upZ: Float) {
		synchronized(lock) {
			eye[0] = eyeX
			eye[1] = eyeY
			eye[2] = eyeZ
			forward[0] = fwX
			forward[1] = fwY
			forward[2] = fwZ
			up[0] = upX
			up[1] = upY
			up[2] = upZ
		}
	}

	/**
	 * 数组拷贝版本，避免宿主多次装箱。
	 */
	fun setCameraFrame(eyeSrc: FloatArray, forwardSrc: FloatArray, upSrc: FloatArray) {
		synchronized(lock) {
			eye[0] = eyeSrc[0]
			eye[1] = eyeSrc[1]
			eye[2] = eyeSrc[2]
			forward[0] = forwardSrc[0]
			forward[1] = forwardSrc[1]
			forward[2] = forwardSrc[2]
			up[0] = upSrc[0]
			up[1] = upSrc[1]
			up[2] = upSrc[2]
		}
	}

	override fun getCameraFrame(eyeOut: FloatArray, forwardOut: FloatArray, upOut: FloatArray) {
		synchronized(lock) {
			eye.copyInto(eyeOut)
			forward.copyInto(forwardOut)
			up.copyInto(upOut)
		}
	}
}
