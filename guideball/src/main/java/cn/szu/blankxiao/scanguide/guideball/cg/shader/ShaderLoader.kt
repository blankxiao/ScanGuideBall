package cn.szu.blankxiao.scanguide.guideball.cg.shader

import android.content.Context
import android.opengl.GLES20
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 从assets目录加载Shader代码的工具类
 */
object ShaderLoader {

    /**
     * 从assets文件加载shader代码
     */
    fun loadShaderFromAssets(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load shader from assets: $fileName", e)
        }
    }

    /**
     * 创建shader程序（从assets文件）
     */
    fun createProgramFromAssets(
        context: Context,
        vertexShaderPath: String,
        fragmentShaderPath: String
    ): Int {
        val vertexShaderCode = loadShaderFromAssets(context, vertexShaderPath)
        val fragmentShaderCode = loadShaderFromAssets(context, fragmentShaderPath)
        return ShaderUtil.createProgram(vertexShaderCode, fragmentShaderCode)
    }
}
