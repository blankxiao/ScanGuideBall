package cn.szu.blankxiao.scanguide.guideball.domain

import cn.szu.blankxiao.scanguide.guideball.cg.egl.GuideBallConfig

/**
 * 球面扫描状态管理
 * 管理扫描点的收集和完整性计算
 */
class SphereScanState(
    private val onCompletenessChanged: ((Float) -> Unit)? = null
) {
    private val gridCols = GuideBallConfig.GRID_COLS
    private val gridRows = GuideBallConfig.GRID_ROWS
    private val totalPointCount = GuideBallConfig.TOTAL_POINT_COUNT
    private val targetPointCount = totalPointCount

    private val collectedMask = FloatArray(totalPointCount)

    // 是否暂停扫描
    private var isPaused = false

    // 当前完整性 (0.0 ~ 1.0)
    var completeness: Float = 0f
        private set

    // 当前正在收集的点索引，-1 表示无
    var collectingIndex: Int = -1
        private set

    // 当前正在收集点的进度 (0.0 ~ 1.0)
    var collectingProgress: Float = 0f
        private set

    /**
     * 标记网格点已完成收集
     */
    fun markCollected(index: Int) {
        if (isPaused) return
        if (index !in 0 until totalPointCount) return
        // 范围采样：收集中心点与周围区域（圆形半径）
        markCollectedRange(index, radius = 2)
        collectingIndex = -1
        collectingProgress = 0f
        updateCompleteness()
    }

    /**
     * 更新当前收集中点与收集进度
     */
    fun updateCollecting(index: Int, progress: Float) {
        if (isPaused) {
            collectingIndex = -1
            collectingProgress = 0f
            return
        }
        if (index !in 0 until totalPointCount || collectedMask[index] >= 0.5f) {
            collectingIndex = -1
            collectingProgress = 0f
            return
        }
        collectingIndex = index
        collectingProgress = progress.coerceIn(0f, 1f)
    }

    /**
     * 切换暂停状态
     */
    fun togglePause() {
        isPaused = !isPaused
    }

    /**
     * 获取已扫描的点数
     */
    fun getScannedCount(): Int = collectedMask.count { it >= 0.5f }

    /**
     * 返回用于着色器上传的收集掩码
     */
    fun getCollectedMask(): FloatArray = collectedMask.copyOf()

    /**
     * 当前点是否已被收集
     */
    fun isCollected(index: Int): Boolean {
        if (index !in 0 until totalPointCount) return false
        return collectedMask[index] >= 0.5f
    }

    /**
     * 网格点总数
     */
    fun getTotalPointCount(): Int = totalPointCount

    fun getGridCols(): Int = gridCols

    fun getGridRows(): Int = gridRows

    /**
     * 是否已暂停
     */
    fun isPaused(): Boolean = isPaused

    private fun updateCompleteness() {
        completeness = (getScannedCount().toFloat() / targetPointCount).coerceIn(0f, 1f)
        onCompletenessChanged?.invoke(completeness)
    }

    /**
     *
     */
    private fun markCollectedRange(centerIndex: Int, radius: Int) {
        val centerRow = centerIndex / gridCols
        val centerCol = centerIndex % gridCols
        for (dr in -radius..radius) {
            val row = centerRow + dr
            if (row !in 0 until gridRows) continue
            for (dc in -radius..radius) {
                val distance2 = dr * dr + dc * dc
                if (distance2 > radius * radius) continue
                val col = (centerCol + dc + gridCols) % gridCols
                val index = row * gridCols + col
                markIndexCollected(index)
            }
        }
    }

    private fun markIndexCollected(index: Int) {
        if (index !in 0 until totalPointCount) return
        if (collectedMask[index] < 0.5f) {
            collectedMask[index] = 1f
        }
    }
}
