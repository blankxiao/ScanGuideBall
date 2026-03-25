package cn.szu.blankxiao.scanguide.guideball.helper

import android.graphics.SurfaceTexture
import android.view.TextureView
import cn.szu.blankxiao.scanguide.guideball.renderer.GuideBallRenderSession

/**
 * 将 [TextureView] 的 [SurfaceTexture] 生命周期交给 [GuideBallRenderSession]。
 */
internal class GuideBallSurfaceTextureCoordinator(
	private val renderSession: GuideBallRenderSession
) : TextureView.SurfaceTextureListener {

	override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
		if (!renderSession.isReady()) {
			renderSession.init(surface, width, height) { }
		} else {
			renderSession.resume(surface) { }
		}
	}

	override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
		renderSession.resize(width, height)
	}

	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
		renderSession.release()
		return true
	}

	override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}
