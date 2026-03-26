package cn.szu.blankxiao.scanguide.guideball.cg.shader

import android.opengl.GLES20

internal object ShaderUtil {

	fun loadShader(type: Int, source: String): Int {
		val shader = GLES20.glCreateShader(type)
		GLES20.glShaderSource(shader, source)
		GLES20.glCompileShader(shader)
		val compiled = IntArray(1)
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
		if (compiled[0] == 0) {
			val log = GLES20.glGetShaderInfoLog(shader)
			GLES20.glDeleteShader(shader)
			throw RuntimeException("Shader compile failed: $log")
		}
		return shader
	}

	fun createProgram(vertexSource: String, fragmentSource: String): Int {
		val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
		val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
		val program = GLES20.glCreateProgram()
		GLES20.glAttachShader(program, vs)
		GLES20.glAttachShader(program, fs)
		GLES20.glBindAttribLocation(program, 0, "a_position")
		GLES20.glBindAttribLocation(program, 1, "a_normal")
		GLES20.glLinkProgram(program)
		val linked = IntArray(1)
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
		GLES20.glDeleteShader(vs)
		GLES20.glDeleteShader(fs)
		if (linked[0] == 0) {
			val log = GLES20.glGetProgramInfoLog(program)
			GLES20.glDeleteProgram(program)
			throw RuntimeException("Program link failed: $log")
		}
		return program
	}
}
