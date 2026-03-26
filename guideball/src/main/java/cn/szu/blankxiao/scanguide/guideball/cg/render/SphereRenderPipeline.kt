package cn.szu.blankxiao.scanguide.guideball.cg.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import cn.szu.blankxiao.scanguide.guideball.cg.egl.EglConfig
import cn.szu.blankxiao.scanguide.guideball.cg.mesh.SphereGeometry
import cn.szu.blankxiao.scanguide.guideball.cg.shader.ShaderLoader

/**
 * 球体渲染管线封装类
 * 封装固定传入数据的代码，包括：
 * - Shader加载和程序创建
 * - 几何体构建
 * - Uniform上传（矩阵、网格参数）
 * - 顶点属性设置
 * - 绘制调用
 */
internal class SphereRenderPipeline(context: Context) {

    private var program: Int = 0
    private var mesh: SphereGeometry.Mesh? = null

    // Uniform位置缓存
    private var uMvpMatrix: Int = -1
    private var uModelMatrix: Int = -1
    private var uViewMatrix: Int = -1
    private var uGridCols: Int = -1
    private var uGridRows: Int = -1
    private var uDotRadius: Int = -1
    private var uCollectedMask: Int = -1
    private var uCollectingIndex: Int = -1
    private var uCollectProgress: Int = -1

    // 固定矩阵：Model恒等，View/Projection在渲染器中计算
    private val identityMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
    private val collectedMaskBuffer = FloatArray(MAX_SCAN_POINTS)
    private var collectingIndex: Int = -1
    private var collectProgress: Float = 0f

    init {
        // 初始化GL状态
        EglConfig.initGLState()

        // 加载Shader并创建程序
        program = ShaderLoader.createProgramFromAssets(
            context,
            EglConfig.VERTEX_SHADER_PATH,
            EglConfig.FRAGMENT_SHADER_PATH
        )

        // 获取uniform位置
        cacheUniformLocations()

        // 构建球体几何
        mesh = SphereGeometry.build(
            radius = EglConfig.SPHERE_RADIUS,
            widthSegments = EglConfig.SPHERE_WIDTH_SEGMENTS,
            heightSegments = EglConfig.SPHERE_HEIGHT_SEGMENTS
        )
    }

    private fun cacheUniformLocations() {
        uMvpMatrix = GLES20.glGetUniformLocation(program, "u_mvpMatrix")
        uModelMatrix = GLES20.glGetUniformLocation(program, "u_modelMatrix")
        uViewMatrix = GLES20.glGetUniformLocation(program, "u_viewMatrix")
        uGridCols = GLES20.glGetUniformLocation(program, "u_gridCols")
        uGridRows = GLES20.glGetUniformLocation(program, "u_gridRows")
        uDotRadius = GLES20.glGetUniformLocation(program, "u_dotRadius")
        uCollectedMask = GLES20.glGetUniformLocation(program, "u_collectedMask")
        uCollectingIndex = GLES20.glGetUniformLocation(program, "u_collectingIndex")
        uCollectProgress = GLES20.glGetUniformLocation(program, "u_collectProgress")
    }

    fun updateScanVisualState(collectedMask: FloatArray, collectingIndex: Int, collectProgress: Float) {
        val count = collectedMask.size.coerceAtMost(MAX_SCAN_POINTS)
        for (i in 0 until count) {
            collectedMaskBuffer[i] = collectedMask[i]
        }
        for (i in count until MAX_SCAN_POINTS) {
            collectedMaskBuffer[i] = 0f
        }
        this.collectingIndex = collectingIndex
        this.collectProgress = collectProgress.coerceIn(0f, 1f)
    }

    /**
     * 设置视口
     * @param width 视口宽度
     * @param height 视口高度
     */
    fun setupViewport(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width.coerceAtLeast(1), height.coerceAtLeast(1))
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    }

    /**
     * 渲染一帧
     * @param mvpMatrix 预计算的MVP矩阵（Projection × View × Model）
     */
    fun render(mvpMatrix: FloatArray) {
        val m = mesh ?: return

        GLES20.glUseProgram(program)

        // 上传MVP矩阵
        GLES20.glUniformMatrix4fv(uMvpMatrix, 1, false, mvpMatrix, 0)

        // 上传固定Model矩阵（恒等）
        GLES20.glUniformMatrix4fv(uModelMatrix, 1, false, identityMatrix, 0)

        // 上传固定View矩阵（用于world-space计算，使用恒等占位，
        // 实际View变换已包含在MVP中）
        GLES20.glUniformMatrix4fv(uViewMatrix, 1, false, identityMatrix, 0)

        // 上传网格参数
        GLES20.glUniform1i(uGridCols, EglConfig.GRID_COLS)
        GLES20.glUniform1i(uGridRows, EglConfig.GRID_ROWS)
        GLES20.glUniform1f(uDotRadius, EglConfig.DOT_RADIUS)
        GLES20.glUniform1fv(uCollectedMask, MAX_SCAN_POINTS, collectedMaskBuffer, 0)
        GLES20.glUniform1i(uCollectingIndex, collectingIndex)
        GLES20.glUniform1f(uCollectProgress, collectProgress)

        // 设置顶点属性
        val stride = m.floatsPerVertex * EglConfig.BYTES_FLOAT

        m.vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, stride, m.vertexBuffer)
        GLES20.glEnableVertexAttribArray(0)

        m.vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, stride, m.vertexBuffer)
        GLES20.glEnableVertexAttribArray(1)

        // 绘制
        m.indexBuffer.position(0)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            m.indexCount,
            GLES20.GL_UNSIGNED_SHORT,
            m.indexBuffer
        )

        // 清理
        GLES20.glDisableVertexAttribArray(0)
        GLES20.glDisableVertexAttribArray(1)
    }

    /**
     * 释放资源
     */
    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    companion object {
        private const val MAX_SCAN_POINTS = EglConfig.GRID_COLS * EglConfig.GRID_ROWS
    }
}
