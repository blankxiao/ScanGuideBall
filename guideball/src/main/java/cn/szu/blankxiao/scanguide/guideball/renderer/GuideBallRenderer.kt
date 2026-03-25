package cn.szu.blankxiao.scanguide.guideball.renderer

/**
 * GLES 渲染回调，均在 [cn.szu.blankxiao.scanguide.guideball.gl.GLProducerThread] 中调用。
 */
interface GuideBallRenderer {

	fun onGLContextAvailable()

	fun onSurfaceChanged(width: Int, height: Int)

	fun onDrawFrame()
}
