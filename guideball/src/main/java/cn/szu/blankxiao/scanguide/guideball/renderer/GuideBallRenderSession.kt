package cn.szu.blankxiao.scanguide.guideball.renderer

import android.graphics.SurfaceTexture
import cn.szu.blankxiao.scanguide.guideball.cg.gl.GLProducerThread
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 管理 GL 线程生命周期与 [post] 投递（与 Panorama [RenderSession] 同构）。
 */
internal class GuideBallRenderSession(
	private val renderer: GuideBallRenderer
) {

	private var initialized = false
	private lateinit var producerThread: GLProducerThread
	private val shouldRender = AtomicBoolean(true)

	fun isReady(): Boolean = initialized

	fun init(surface: SurfaceTexture, width: Int, height: Int, onReady: () -> Unit) {
		shouldRender.set(true)
		producerThread = GLProducerThread(surface, renderer, shouldRender)
		producerThread.start()
		post { renderer.onSurfaceChanged(width, height) }
		initialized = true
		onReady()
	}

	fun resume(surface: SurfaceTexture, onResume: () -> Unit) {
		producerThread.refreshSurfaceTexture(surface)
		onResume()
	}

	fun resize(width: Int, height: Int) {
		post { renderer.onSurfaceChanged(width, height) }
	}

	fun release() {
		if (!initialized) return
		shouldRender.set(false)
		try {
			producerThread.join(5000)
		} catch (_: InterruptedException) {
			Thread.currentThread().interrupt()
		}
		initialized = false
	}

	fun post(task: () -> Unit) {
		if (::producerThread.isInitialized) {
			producerThread.enqueueEvent(Runnable { task() })
		}
	}
}
