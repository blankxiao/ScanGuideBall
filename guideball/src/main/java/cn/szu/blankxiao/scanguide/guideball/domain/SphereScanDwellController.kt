package cn.szu.blankxiao.scanguide.guideball.domain

/**
 * 扫描停留控制器
 * 控制扫描时的停留时间和采样频率
 */
class SphereScanDwellController(
    private val scanState: SphereScanState
) {
    // 采样间隔 (ms)
    private val sampleIntervalMs = 66L

    // 最后采样时间
    private var lastSampleTime = 0L

    // 在目标点的停留时间 (ms)
    private val dwellTimeMs = 340L

    // 开始停留的时间
    private var dwellStartTime = 0L

    // 当前聚焦点索引
    private var focusedIndex = -1

    /**
     * 更新控制器状态，由渲染循环调用
     */
    fun update(currentTimeMs: Long, currentFocusedIndex: Int) {
        if (scanState.isPaused()) {
            focusedIndex = -1
            scanState.updateCollecting(-1, 0f)
            return
        }
        if (currentTimeMs - lastSampleTime < sampleIntervalMs) {
            return
        }
        lastSampleTime = currentTimeMs

        if (currentFocusedIndex !in 0 until scanState.getTotalPointCount()) {
            focusedIndex = -1
            scanState.updateCollecting(-1, 0f)
            return
        }
        if (scanState.isCollected(currentFocusedIndex)) {
            focusedIndex = -1
            scanState.updateCollecting(-1, 0f)
            return
        }

        if (focusedIndex != currentFocusedIndex && !isInEffectiveRange(focusedIndex, currentFocusedIndex)) {
            focusedIndex = currentFocusedIndex
            dwellStartTime = currentTimeMs
            scanState.updateCollecting(focusedIndex, 0f)
            return
        }
        // 范围内漂移不重置计时，只更新可视焦点
        focusedIndex = currentFocusedIndex

        val progress = ((currentTimeMs - dwellStartTime).toFloat() / dwellTimeMs).coerceIn(0f, 1f)
        scanState.updateCollecting(focusedIndex, progress)
        if (progress >= 1f) {
            scanState.markCollected(focusedIndex)
            focusedIndex = -1
        }
    }

    /**
     * 获取当前进度 (0.0 ~ 1.0)
     */
    fun getProgress(): Float = scanState.collectingProgress

    /**
     * 是否正在停留
     */
    fun isDwelling(): Boolean = scanState.collectingIndex >= 0

    /**
     * 获取当前聚焦点索引
     */
    fun getFocusedIndex(): Int = focusedIndex

    /**
     * 重置控制器
     */
    fun reset() {
        focusedIndex = -1
        dwellStartTime = 0L
        lastSampleTime = 0L
        scanState.updateCollecting(-1, 0f)
    }

    private fun isInEffectiveRange(baseIndex: Int, currentIndex: Int): Boolean {
        if (baseIndex < 0 || currentIndex < 0) return false
        val cols = scanState.getGridCols()
        val rows = scanState.getGridRows()
        val baseRow = baseIndex / cols
        val baseCol = baseIndex % cols
        val currentRow = currentIndex / cols
        val currentCol = currentIndex % cols
        val rowDelta = kotlin.math.abs(baseRow - currentRow)
        val rawColDelta = kotlin.math.abs(baseCol - currentCol)
        val colDelta = minOf(rawColDelta, cols - rawColDelta)
        if (baseRow !in 0 until rows || currentRow !in 0 until rows) return false
        // 圆形有效范围：网格平面距离落在半径内才连续计时
        val radius = 2
        return rowDelta * rowDelta + colDelta * colDelta <= radius * radius
    }
}
