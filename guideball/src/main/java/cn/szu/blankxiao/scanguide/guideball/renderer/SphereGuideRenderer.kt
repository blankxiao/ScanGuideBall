package cn.szu.blankxiao.scanguide.guideball.renderer

import android.content.Context
import android.opengl.GLES20
import cn.szu.blankxiao.scanguide.guideball.cg.GuideBallCamera
import cn.szu.blankxiao.scanguide.guideball.cg.egl.EglConfig
import cn.szu.blankxiao.scanguide.guideball.controller.GuideBallRotationController
import cn.szu.blankxiao.scanguide.guideball.cg.render.SphereRenderPipeline
import cn.szu.blankxiao.scanguide.guideball.domain.SphereScanDwellController
import cn.szu.blankxiao.scanguide.guideball.domain.SphereScanState
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * 球体引导渲染器
 * 参考 MyPanorama 的 Renderer 实现：
 * - 持有 Camera 和 RotationController
 * - 每帧调用 controller.updateCameraView(camera)
 * - 从 camera 获取 MVP 矩阵
 */
internal class SphereGuideRenderer(
	private val rotationController: GuideBallRotationController,
	private val scanState: SphereScanState
) : GuideBallRenderer {

	private lateinit var camera: GuideBallCamera
	private var pipeline: SphereRenderPipeline? = null

	private var width: Int = 1
	private var height: Int = 1
	private val currentForward = FloatArray(3)
	private val currentViewNormal = FloatArray(3)
	private val dwellController = SphereScanDwellController(scanState)

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
		rotationController.getLatestForward(currentForward)
		// 屏幕中心对应的是相机朝向球面的法线，方向应为 -forward
		currentViewNormal[0] = -currentForward[0]
		currentViewNormal[1] = -currentForward[1]
		currentViewNormal[2] = -currentForward[2]
		dwellController.update(System.currentTimeMillis(), findFocusedGridIndex(currentViewNormal))
		p.updateScanVisualState(
			collectedMask = scanState.getCollectedMask(),
			collectingIndex = scanState.collectingIndex,
			collectProgress = scanState.collectingProgress
		)

		// 渲染（使用相机提供的 MVP 矩阵）
		p.render(camera.getMVPMatrix())
	}

	private fun findFocusedGridIndex(forward: FloatArray): Int {
		val x = forward[0]
		val y = forward[1].coerceIn(-1f, 1f)
		val z = forward[2]
		val lon = wrapLongitude(atan2(x, z).toFloat())
		val lat = asin(y.toDouble()).toFloat()
		val lonStep = (2f * PI.toFloat()) / EglConfig.GRID_COLS
		val latStep = PI.toFloat() / (EglConfig.GRID_ROWS + 1)
		val latStart = -0.5f * PI.toFloat() + latStep
		val row = ((lat - latStart) / latStep).roundToInt().coerceIn(0, EglConfig.GRID_ROWS - 1)
		val col = (lon / lonStep).roundToInt().mod(EglConfig.GRID_COLS)
		return row * EglConfig.GRID_COLS + col
	}

	private fun wrapLongitude(value: Float): Float {
		var lon = value
		val twoPi = 2f * PI.toFloat()
		while (lon < 0f) lon += twoPi
		while (lon >= twoPi) lon -= twoPi
		return lon
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
