package cn.szu.blankxiao.scanguide.guideball.domain

/**
 * 扫描停留控制器
 * 控制扫描时的停留时间和采样频率
 */
class SphereScanDwellController(
    private val scanState: SphereScanState
) {
    // 采样间隔 (ms)
    private val sampleIntervalMs = 100L

    // 最后采样时间
    private var lastSampleTime = 0L

    // 当前目标方向
    private var targetX = 0f
    private var targetY = 0f
    private var targetZ = 1f

    // 在目标点的停留时间 (ms)
    private val dwellTimeMs = 500L

    // 开始停留的时间
    private var dwellStartTime = 0L

    // 是否正在停留
    private var isDwelling = false

    // 角度阈值（判断是否对准目标）
    private val angleThreshold = 0.15f  // 约8.6度

    /**
     * 设置当前目标点
     */
    fun setTarget(x: Float, y: Float, z: Float) {
        targetX = x
        targetY = y
        targetZ = z
        isDwelling = false
    }

    /**
     * 更新控制器状态，由渲染循环调用
     */
    fun update(currentTimeMs: Long, currentDirection: FloatArray) {
        if (currentTimeMs - lastSampleTime < sampleIntervalMs) {
            return
        }
        lastSampleTime = currentTimeMs

        val cx = currentDirection[0]
        val cy = currentDirection[1]
        val cz = currentDirection[2]

        // 计算与目标的点积（即cos角度）
        val dot = cx * targetX + cy * targetY + cz * targetZ

        // 检查是否对准目标（点积接近1表示角度很小）
        if (dot > (1f - angleThreshold)) {
            if (!isDwelling) {
                // 开始停留
                isDwelling = true
                dwellStartTime = currentTimeMs
            } else if (currentTimeMs - dwellStartTime >= dwellTimeMs) {
                // 停留完成，记录扫描点
                scanState.addDirection(targetX, targetY, targetZ)
                isDwelling = false
            }
        } else {
            // 偏离目标，取消停留
            isDwelling = false
        }
    }

    /**
     * 获取当前进度 (0.0 ~ 1.0)
     */
    fun getProgress(): Float {
        if (!isDwelling) return 0f
        val elapsed = System.currentTimeMillis() - dwellStartTime
        return (elapsed.toFloat() / dwellTimeMs).coerceIn(0f, 1f)
    }

    /**
     * 是否正在停留
     */
    fun isDwelling(): Boolean = isDwelling

    /**
     * 重置控制器
     */
    fun reset() {
        isDwelling = false
        targetX = 0f
        targetY = 0f
        targetZ = 1f
        lastSampleTime = 0L
    }
}
