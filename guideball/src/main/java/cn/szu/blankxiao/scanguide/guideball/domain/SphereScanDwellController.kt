package cn.szu.blankxiao.scanguide.guideball.domain

import android.os.SystemClock
import cn.szu.blankxiao.scanguide.guideball.bridge.OrbitDirectionBridge
import kotlin.math.min

/**
 * 角速度低于阈值时累计停留时间，达到 [dwellHoldMs] 后在当前视线方向打点。
 */
internal class SphereScanDwellController(
	private val scanState: SphereScanState,
	private val directionBridge: OrbitDirectionBridge
) {

	private var lastElapsedMs: Long = SystemClock.elapsedRealtime()
	private var dwellAccumSec: Float = 0f
	private val scratchDir = FloatArray(3)

	fun reset() {
		dwellAccumSec = 0f
		lastElapsedMs = SystemClock.elapsedRealtime()
	}

	/**
	 * @param angularSpeedRadPerSec 陀螺仪角速度模长
	 */
	fun onAngularSpeed(angularSpeedRadPerSec: Float) {
		if (scanState.isPaused) {
			dwellAccumSec = 0f
			lastElapsedMs = SystemClock.elapsedRealtime()
			return
		}
		val now = SystemClock.elapsedRealtime()
		val dtSec = min(0.2f, (now - lastElapsedMs) / 1000f).coerceAtLeast(0f)
		lastElapsedMs = now

		if (angularSpeedRadPerSec < STILLNESS_THRESHOLD_RAD_PER_SEC) {
			dwellAccumSec += dtSec
			if (dwellAccumSec >= dwellHoldMs / 1000f) {
				directionBridge.copyDirectionInto(scratchDir)
				scanState.tryAddPoint(scratchDir)
				dwellAccumSec = 0f
			}
		} else {
			dwellAccumSec = 0f
		}
	}

	companion object {
		/** 低于此值认为「相对静止」，可调参 */
		const val STILLNESS_THRESHOLD_RAD_PER_SEC = 0.35f

		/** 停留多久触发一次打点（毫秒） */
		const val dwellHoldMs: Long = 1100L
	}
}
