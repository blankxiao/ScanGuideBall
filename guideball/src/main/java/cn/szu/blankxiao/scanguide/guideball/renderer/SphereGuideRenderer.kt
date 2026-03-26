package cn.szu.blankxiao.scanguide.guideball.renderer

import android.content.Context
import android.opengl.GLES20
import cn.szu.blankxiao.scanguide.guideball.cg.GuideBallCamera
import cn.szu.blankxiao.scanguide.guideball.controller.GuideBallRotationController
import cn.szu.blankxiao.scanguide.guideball.cg.render.SphereRenderPipeline

/**
 * 球体引导渲染器
 * 参考 MyPanorama 的 Renderer 实现：
 * - 持有 Camera 和 RotationController
 * - 每帧调用 controller.updateCameraView(camera)
 * - 从 camera 获取 MVP 矩阵
 */
internal class SphereGuideRenderer(
	private val rotationController: GuideBallRotationController
) : GuideBallRenderer {

	private lateinit var camera: GuideBallCamera
	private var pipeline: SphereRenderPipeline? = null

	private var width: Int = 1
	private var height: Int = 1

	override fun onGLContextAvailable() {
		pipeline = SphereRenderPipeline(context)
	}

	override fun onSurfaceChanged(width: Int, height: Int) {
		this.width = width.coerceAtLeast(1)
		this.height = height.coerceAtLeast(1)

		// 设置视口
		GLES20.glViewport(0, 0, this.width, this.height)

		// 创建相机（参考 MyPanorama）
		val ratio = width.toFloat() / height.toFloat()
		camera = GuideBallCamera(ratio)
	}

	override fun onDrawFrame() {
		val p = pipeline ?: return

		// 清空缓冲区
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

		// 更新相机视图（参考 MyPanorama：rotationController.updateCameraView(camera)）
		rotationController.updateCameraView(camera)

		// 渲染（使用相机提供的 MVP 矩阵）
		p.render(camera.getMVPMatrix())
	}

	// 延迟初始化 context
	private lateinit var context: Context

	fun setContext(context: Context) {
		this.context = context.applicationContext
	}

	/**
	 * 获取相机（供外部访问）
	 */
	fun getCamera(): GuideBallCamera = camera
}
