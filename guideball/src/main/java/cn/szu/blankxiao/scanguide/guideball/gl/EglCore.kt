package cn.szu.blankxiao.scanguide.guideball.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface

/**
 * TextureView 的 [SurfaceTexture] 上建立 GLES2 上下文；不在此释放 [SurfaceTexture]（由 TextureView 管理）。
 */
internal class EglCore {

	private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
	private var context: EGLContext = EGL14.EGL_NO_CONTEXT
	private var surface: EGLSurface = EGL14.EGL_NO_SURFACE
	private var config: EGLConfig? = null

	fun init(surfaceTexture: SurfaceTexture) {
		display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
		if (display == EGL14.EGL_NO_DISPLAY) {
			throw RuntimeException("eglGetDisplay failed")
		}
		val version = IntArray(2)
		if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
			throw RuntimeException("eglInitialize failed")
		}
		config = chooseConfig(display)
		val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
		context = EGL14.eglCreateContext(display, config!!, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
		if (context == EGL14.EGL_NO_CONTEXT) {
			throw RuntimeException("eglCreateContext failed")
		}
		val surfAttribs = intArrayOf(EGL14.EGL_NONE)
		surface = EGL14.eglCreateWindowSurface(display, config!!, surfaceTexture, surfAttribs, 0)
		if (surface == EGL14.EGL_NO_SURFACE) {
			throw RuntimeException("eglCreateWindowSurface failed")
		}
		if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
			throw RuntimeException("eglMakeCurrent failed")
		}
	}

	/**
	 * 从后台返回时可能更换 [SurfaceTexture]，仅重建 surface，保留 context。
	 */
	fun refreshSurfaceTexture(surfaceTexture: SurfaceTexture) {
		if (display == EGL14.EGL_NO_DISPLAY) return
		EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
		if (surface != EGL14.EGL_NO_SURFACE) {
			EGL14.eglDestroySurface(display, surface)
			surface = EGL14.EGL_NO_SURFACE
		}
		val surfAttribs = intArrayOf(EGL14.EGL_NONE)
		surface = EGL14.eglCreateWindowSurface(display, config!!, surfaceTexture, surfAttribs, 0)
		if (surface == EGL14.EGL_NO_SURFACE) {
			throw RuntimeException("eglCreateWindowSurface (refresh) failed")
		}
		if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
			throw RuntimeException("eglMakeCurrent (refresh) failed")
		}
	}

	fun swapBuffers() {
		if (display != EGL14.EGL_NO_DISPLAY && surface != EGL14.EGL_NO_SURFACE) {
			EGL14.eglSwapBuffers(display, surface)
		}
	}

	fun releaseGl() {
		if (display == EGL14.EGL_NO_DISPLAY) return
		EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
		if (surface != EGL14.EGL_NO_SURFACE) {
			EGL14.eglDestroySurface(display, surface)
			surface = EGL14.EGL_NO_SURFACE
		}
		if (context != EGL14.EGL_NO_CONTEXT) {
			EGL14.eglDestroyContext(display, context)
			context = EGL14.EGL_NO_CONTEXT
		}
		EGL14.eglReleaseThread()
		EGL14.eglTerminate(display)
		display = EGL14.EGL_NO_DISPLAY
	}

	private fun chooseConfig(d: EGLDisplay): EGLConfig {
		val attribs = intArrayOf(
			EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
			EGL14.EGL_RED_SIZE, 8,
			EGL14.EGL_GREEN_SIZE, 8,
			EGL14.EGL_BLUE_SIZE, 8,
			EGL14.EGL_ALPHA_SIZE, 8,
			EGL14.EGL_DEPTH_SIZE, 16,
			EGL14.EGL_STENCIL_SIZE, 0,
			EGL14.EGL_NONE
		)
		val configs = arrayOfNulls<EGLConfig>(1)
		val num = IntArray(1)
		if (!EGL14.eglChooseConfig(d, attribs, 0, configs, 0, 1, num, 0) || num[0] <= 0) {
			throw RuntimeException("eglChooseConfig failed")
		}
		return configs[0]!!
	}
}
