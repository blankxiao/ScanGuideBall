package cn.szu.blankxiao.scanguide.guideball.camera

/**
 * 相机视角数据源（最高优先级：由宿主/AR/相机管线提供，与 OpenGL 渲染共用同一套约定）。
 *
 * 世界坐标系为右手系，与典型 OpenGL 一致；球心在原点。
 */
fun interface CameraViewProvider {

	/**
	 * 每帧在 GL 线程调用，写出当前相机姿态。
	 *
	 * @param eyeOut 相机位置 (world)，长度 3
	 * @param forwardOut 单位向量：从相机指向观察目标（球心），即视线方向，长度 3
	 * @param upOut 单位向量：相机上方向（world），长度 3
	 */
	fun getCameraFrame(eyeOut: FloatArray, forwardOut: FloatArray, upOut: FloatArray)
}
