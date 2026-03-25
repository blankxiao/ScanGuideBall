package cn.szu.blankxiao.scanguide.guideball.domain

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

/**
 * 球面采样点与完成度；与 GL、停留逻辑共享，读多写少用 [CopyOnWriteArrayList]。
 */
class SphereScanState(
	private val onCompletenessChanged: ((Float) -> Unit)? = null
) {

	val points: CopyOnWriteArrayList<ScanPoint> = CopyOnWriteArrayList()

	@Volatile
	var completeness: Float = 0f
		private set

	@Volatile
	var isPaused: Boolean = false
		private set

	/** 暂停时冻结生长动画的参考时刻（毫秒） */
	@Volatile
	var frozenTimeMs: Long? = null
		private set

	fun effectiveNowMs(): Long =
		frozenTimeMs ?: System.currentTimeMillis()

	fun togglePause() {
		if (!isPaused) {
			isPaused = true
			frozenTimeMs = System.currentTimeMillis()
		} else {
			isPaused = false
			frozenTimeMs = null
		}
	}

	/**
	 * @return 是否新增成功（去重失败则为 false）
	 */
	fun tryAddPoint(direction: FloatArray): Boolean {
		if (isPaused) return false
		synchronized(mergeLock) {
			val nx = direction[0]
			val ny = direction[1]
			val nz = direction[2]
			val len = kotlin.math.sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat().coerceAtLeast(1e-6f)
			val d0 = nx / len
			val d1 = ny / len
			val d2 = nz / len
			for (p in points) {
				val dot = d0 * p.dir[0] + d1 * p.dir[1] + d2 * p.dir[2]
				if (dot >= DEDUP_DOT_THRESHOLD) return false
			}
			if (points.size >= MAX_POINTS) {
				points.removeAt(0)
			}
			points.add(ScanPoint(floatArrayOf(d0, d1, d2), System.currentTimeMillis()))
			completeness = min(1f, completeness + COMPLETENESS_DELTA_PER_POINT)
			onCompletenessChanged?.invoke(completeness)
			return true
		}
	}

	fun clearPointsForDebug() {
		points.clear()
		completeness = 0f
		onCompletenessChanged?.invoke(0f)
	}

	private val mergeLock = Any()

	data class ScanPoint(
		val dir: FloatArray,
		val createdAtMs: Long
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as ScanPoint
			return createdAtMs == other.createdAtMs && dir.contentEquals(other.dir)
		}

		override fun hashCode(): Int = 31 * createdAtMs.hashCode() + dir.contentHashCode()
	}

	companion object {
		const val MAX_POINTS = 16
		private const val DEDUP_DOT_THRESHOLD = 0.94f
		private const val COMPLETENESS_DELTA_PER_POINT = 0.08f
	}
}
