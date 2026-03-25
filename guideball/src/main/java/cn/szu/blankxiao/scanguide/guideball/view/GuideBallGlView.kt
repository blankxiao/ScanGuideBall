package cn.szu.blankxiao.scanguide.guideball.view

import android.content.Context
import android.view.TextureView
import android.widget.FrameLayout
import cn.szu.blankxiao.scanguide.guideball.bridge.OrbitDirectionBridge
import cn.szu.blankxiao.scanguide.guideball.camera.CameraViewProvider
import cn.szu.blankxiao.scanguide.guideball.camera.SensorCameraViewProvider
import cn.szu.blankxiao.scanguide.guideball.domain.SphereScanDwellController
import cn.szu.blankxiao.scanguide.guideball.domain.SphereScanState
import cn.szu.blankxiao.scanguide.guideball.helper.GuideBallSurfaceTextureCoordinator
import cn.szu.blankxiao.scanguide.guideball.renderer.GuideBallRenderSession
import cn.szu.blankxiao.scanguide.guideball.renderer.SphereGuideRenderer
import cn.szu.blankxiao.scanguide.guideball.sensor.GyroscopeAngularVelocityTracker

/**
 * TextureView + GL；**视角**由 [CameraViewProvider] 提供（默认 [SensorCameraViewProvider] 随设备姿态；
 * 宿主/真实相机请换 [cn.szu.blankxiao.scanguide.guideball.camera.HostCameraFrameProvider]）。
 * 停留打点仍使用陀螺仪角速度（与视角解耦）。
 */
class GuideBallGlView(
	context: Context,
	onCompletenessChanged: ((Float) -> Unit)? = null,
	cameraViewProvider: CameraViewProvider = SensorCameraViewProvider(context)
) : FrameLayout(context) {

	val scanState: SphereScanState = SphereScanState(onCompletenessChanged)

	val cameraView: CameraViewProvider = cameraViewProvider

	private val directionBridge = OrbitDirectionBridge()
	private val dwellController = SphereScanDwellController(scanState, directionBridge)
	private val gyroTracker = GyroscopeAngularVelocityTracker(context) { mag ->
		dwellController.onAngularSpeed(mag)
	}

	private val renderer = SphereGuideRenderer(cameraViewProvider, scanState, directionBridge)
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

	fun togglePause() {
		scanState.togglePause()
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		(cameraView as? SensorCameraViewProvider)?.onAttachedToWindow()
		gyroTracker.onAttached()
	}

	override fun onDetachedFromWindow() {
		(cameraView as? SensorCameraViewProvider)?.onDetachedFromWindow()
		gyroTracker.onDetached()
		dwellController.reset()
		renderSession.release()
		super.onDetachedFromWindow()
	}
}
