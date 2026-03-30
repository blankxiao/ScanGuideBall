package cn.szu.blankxiao.scanguide.guideball.cg.egl

import android.opengl.GLES20

/**
 * 全局配置常量
 * 集中管理所有模块共享的配置参数
 */
object GuideBallConfig {

    // ==================== 相机/视锥体参数 ====================
    const val FOV_DEGREES = 60f
    const val Z_NEAR = 0.1f
    const val Z_FAR = 100f

    // ==================== 扫描网格参数 ====================
    // ⚠️ 修改这些值会影响扫描点总数和着色器渲染
    const val GRID_COLS = 18      // 经度方向列数
    const val GRID_ROWS = 16      // 纬度方向行数
    const val DOT_RADIUS = 0.024f // 点的基础大小

    // ==================== 球体几何参数 ====================
    const val SPHERE_RADIUS = 1f
    const val SPHERE_WIDTH_SEGMENTS = 48
    const val SPHERE_HEIGHT_SEGMENTS = 24

    // ==================== OpenGL常量 ====================
    const val BYTES_FLOAT = 4

    // ==================== Shader文件路径 ====================
    const val VERTEX_SHADER_PATH = "raw/sphere_vertex.glsl"
    const val FRAGMENT_SHADER_PATH = "raw/sphere_fragment.glsl"

    /**
     * 计算总点数
     */
    val TOTAL_POINT_COUNT: Int = GRID_COLS * GRID_ROWS

    /**
     * 初始化OpenGL ES状态
     */
    fun initGLState() {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
    }
}
