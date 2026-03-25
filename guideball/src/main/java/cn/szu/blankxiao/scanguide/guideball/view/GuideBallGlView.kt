package cn.szu.blankxiao.scanguide.guideball.view

import android.content.Context
import android.view.TextureView
import android.widget.FrameLayout
import cn.szu.blankxiao.scanguide.guideball.helper.GuideBallSurfaceTextureCoordinator
import cn.szu.blankxiao.scanguide.guideball.orientation.GyroOrientationProvider
import cn.szu.blankxiao.scanguide.guideball.renderer.GuideBallRenderSession
import cn.szu.blankxiao.scanguide.guideball.renderer.SphereGuideRenderer

/**
 * 底部区域全屏拉伸的 GL 承载：TextureView + 独立 GL 线程 + 球体与陀螺仪视角。
 */
class GuideBallGlView(
	context: Context
) : FrameLayout(context) {

	private val orientationProvider = GyroOrientationProvider(context)
	private val renderer = SphereGuideRenderer(orientationProvider)
	private val renderSession = GuideBallRenderSession(renderer)
	private val coordinator = GuideBallSurfaceTextureCoordinator(renderSession)

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

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		orientationProvider.onAttached()
	}

	override fun onDetachedFromWindow() {
		orientationProvider.onDetached()
		renderSession.release()
		super.onDetachedFromWindow()
	}
}
