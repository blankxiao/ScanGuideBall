package cn.szu.blankxiao.scanguide.guideball.gl

import android.graphics.SurfaceTexture
import android.util.Log
import cn.szu.blankxiao.scanguide.guideball.renderer.GuideBallRenderer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 专用渲染线程：EGL + 事件队列 + 帧循环（约 60fps：每帧 sleep 16ms）。
 */
internal class GLProducerThread(
	private val surfaceTexture: SurfaceTexture,
	private val renderer: GuideBallRenderer,
	private val shouldRender: AtomicBoolean
) : Thread("GuideBall-GL") {

	private val egl = EglCore()
	val eventHandler = GLEventHandler()

	fun enqueueEvent(runnable: Runnable?) {
		eventHandler.enqueueEvent(runnable)
	}

	fun refreshSurfaceTexture(surfaceTexture: SurfaceTexture) {
		enqueueEvent {
			egl.refreshSurfaceTexture(surfaceTexture)
		}
	}

	override fun run() {
		try {
			egl.init(surfaceTexture)
			renderer.onGLContextAvailable()
			while (shouldRender.get()) {
				eventHandler.dequeueEventAndRun()
				if (!shouldRender.get()) break
				renderer.onDrawFrame()
				egl.swapBuffers()
				try {
					Thread.sleep(FRAME_INTERVAL_MS)
				} catch (_: InterruptedException) {
					Thread.currentThread().interrupt()
					break
				}
			}
		} catch (e: Exception) {
			Log.e(TAG, "GL thread error", e)
		} finally {
			egl.releaseGl()
		}
	}

	companion object {
		private const val TAG = "GuideBall-GL"
		private const val FRAME_INTERVAL_MS = 16L
	}
}
