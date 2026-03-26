package cn.szu.blankxiao.scanguide.guideball.cg.egl

import android.opengl.GLES20

/**
 * EGL初始化配置封装类
 * 封装OpenGL ES固定的配置参数和初始化代码
 */
object EglConfig {

    // 视锥体参数
    const val FOV_DEGREES = 60f
    const val Z_NEAR = 0.1f
    const val Z_FAR = 100f

    // 网格参数 - 调整这些值来改变点阵密度和清晰度
    const val GRID_COLS = 18      // 经度方向列数
    const val GRID_ROWS = 16      // 纬度方向行数
    const val DOT_RADIUS = 0.024f // 点的基础大小

    // 球体几何参数
    const val SPHERE_RADIUS = 1f
    const val SPHERE_WIDTH_SEGMENTS = 48
    const val SPHERE_HEIGHT_SEGMENTS = 24

    // OpenGL常量
    const val BYTES_FLOAT = 4

    // Shader文件路径
    const val VERTEX_SHADER_PATH = "raw/sphere_vertex.glsl"
    const val FRAGMENT_SHADER_PATH = "raw/sphere_fragment.glsl"

    /**
     * 初始化OpenGL ES状态
     * 包括清屏颜色、深度测试、面剔除等固定配置
     */
    fun initGLState() {
        GLES20.glClearColor(0.12f, 0.12f, 0.14f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
    }

    /**
     * 获取网格参数（用于uniform上传）
     */
    data class GridParams(
        val cols: Int = GRID_COLS,
        val rows: Int = GRID_ROWS,
        val dotRadius: Float = DOT_RADIUS
    )
}
