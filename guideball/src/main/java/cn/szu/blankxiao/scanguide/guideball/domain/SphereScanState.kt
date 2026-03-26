package cn.szu.blankxiao.scanguide.guideball.domain

/**
 * 球面扫描状态管理
 * 管理扫描点的收集和完整性计算
 */
class SphereScanState(
    private val onCompletenessChanged: ((Float) -> Unit)? = null
) {
    // 扫描方向集合 (用Triple存储x,y,z，避免引入新类)
    private val scannedDirections = mutableSetOf<Triple<Int, Int, Int>>()

    // 是否暂停扫描
    private var isPaused = false

    // 当前完整性 (0.0 ~ 1.0)
    var completeness: Float = 0f
        private set

    // 总目标点数（用于计算完整性）
    private val targetPointCount = 100

    // 量化精度（用于将浮点方向量化为整数）
    private val quantizeFactor = 100

    /**
     * 添加方向向量作为扫描点
     */
    fun addDirection(x: Float, y: Float, z: Float) {
        if (isPaused) return

        // 量化为整数存储
        val key = Triple(
            (x * quantizeFactor).toInt(),
            (y * quantizeFactor).toInt(),
            (z * quantizeFactor).toInt()
        )
        scannedDirections.add(key)
        updateCompleteness()
    }

    /**
     * 切换暂停状态
     */
    fun togglePause() {
        isPaused = !isPaused
    }

    /**
     * 重置扫描状态
     */
    fun reset() {
        scannedDirections.clear()
        updateCompleteness()
    }

    /**
     * 获取已扫描的点数
     */
    fun getScannedCount(): Int = scannedDirections.size

    /**
     * 是否已暂停
     */
    fun isPaused(): Boolean = isPaused

    private fun updateCompleteness() {
        completeness = (scannedDirections.size.toFloat() / targetPointCount).coerceIn(0f, 1f)
        onCompletenessChanged?.invoke(completeness)
    }

    companion object {
        private const val TAG = "SphereScanState"
    }
}
