package cn.szu.blankxiao.scanguide.guideball.view

import android.content.Context
import android.view.TextureView
import android.widget.FrameLayout
import cn.szu.blankxiao.scanguide.guideball.camera.AccelOnlyOrientationProvider
import cn.szu.blankxiao.scanguide.guideball.controller.GuideBallRotationController
import cn.szu.blankxiao.scanguide.guideball.domain.SphereScanState
import cn.szu.blankxiao.scanguide.guideball.helper.GuideBallSurfaceTextureCoordinator
import cn.szu.blankxiao.scanguide.guideball.renderer.GuideBallRenderSession
import cn.szu.blankxiao.scanguide.guideball.renderer.SphereGuideRenderer

/**
 * GuideBall GL 视图
 * 参考 MyPanorama 的架构：
 * - OrientationProvider 提供传感器数据
 * - RotationController 管理旋转逻辑
 * - Renderer 持有 Camera 和 Controller
 */
class GuideBallGlView(
	context: Context,
	onCompletenessChanged: ((Float) -> Unit)? = null,
	private val onPausedChanged: ((Boolean) -> Unit)? = null
) : FrameLayout(context) {

	val scanState: SphereScanState = SphereScanState(onCompletenessChanged)

	// 方向提供者（类似 MyPanorama 的 GyroOrientationProvider）
	private val orientationProvider = AccelOnlyOrientationProvider(context)

	// 旋转控制器（类似 MyPanorama 的 RotationController）
	private val rotationController = GuideBallRotationController(orientationProvider)

	// 渲染器（类似 MyPanorama 的 Renderer）
	private val renderer = SphereGuideRenderer(rotationController, scanState).apply {
		setContext(context)
	}

	private val renderSession = GuideBallRenderSession(renderer)
	private val coordinator = GuideBallSurfaceTextureCoordinator(renderSession) {
		if (scanState.isPaused()) {
			renderSession.setRenderingPaused(true)
		}
	}

	private val textureView = TextureView(context).apply {
		isOpaque = false
		setOnTouchListener { _, _ -> false }
		surfaceTextureListener = coordinator
	}

	init {
		addView(
			textureView,
			LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
		)
	}

	fun togglePause(): Boolean {
		scanState.togglePause()
		onPausedChanged?.invoke(scanState.isPaused())
		if (renderSession.isReady()) {
			renderSession.setRenderingPaused(scanState.isPaused())
		}
		return scanState.isPaused()
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		// 启动传感器监听（类似 MyPanorama 的 renderer.onAttached()）
		rotationController.onAttached()
	}

	override fun onDetachedFromWindow() {
		// 停止传感器监听
		rotationController.onDetached()
		renderSession.release()
		super.onDetachedFromWindow()
	}
}
