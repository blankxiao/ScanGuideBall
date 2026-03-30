package cn.szu.blankxiao.scanguide.guideball.cg.mesh

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 经纬球：顶点位置 + 法线（单位球法线即归一化坐标），三角索引。
 */
internal object SphereGeometry {

	data class Mesh(
		val vertexBuffer: FloatBuffer,
		val indexBuffer: ShortBuffer,
		val indexCount: Int,
		val floatsPerVertex: Int // position 3 + normal 3
	)

	fun build(
		radius: Float = 1f,
		widthSegments: Int = 36,
		heightSegments: Int = 18
	): Mesh {
		val positions = ArrayList<FloatArray>()
		val vertices = ArrayList<ArrayList<Int>>()
		var index = 0
		// 计算位置和法向量 填充positions
		for (y in 0..heightSegments) {
			val v = y / heightSegments.toFloat()
			val row = ArrayList<Int>()
			for (x in 0..widthSegments) {
				val u = x / widthSegments.toFloat()
				val phi = u * 2 * PI
				val theta = v * PI
				val px = (radius * cos(phi) * sin(theta)).toFloat()
				val py = (radius * cos(theta)).toFloat()
				val pz = (radius * sin(phi) * sin(theta)).toFloat()
				// 法向量计算
				val len = kotlin.math.sqrt((px * px + py * py + pz * pz).toDouble()).toFloat().coerceAtLeast(1e-4f)
				val nx = px / len
				val ny = py / len
				val nz = pz / len
				positions.add(floatArrayOf(px, py, pz, nx, ny, nz))
				row.add(index++)
			}
			vertices.add(row)
		}
		val indices = ArrayList<Short>()
		// 计算索引坐标 填充vertices
		for (y in 0 until heightSegments) {
			for (x in 0 until widthSegments) {
				val v1 = vertices[y][x + 1]
				val v2 = vertices[y][x]
				val v3 = vertices[y + 1][x]
				val v4 = vertices[y + 1][x + 1]
				if (y != 0) {
					indices.add(v1.toShort())
					indices.add(v4.toShort())
					indices.add(v2.toShort())
				}
				if (y != heightSegments - 1) {
					indices.add(v2.toShort())
					indices.add(v4.toShort())
					indices.add(v3.toShort())
				}
			}
		}
		val interleaved = FloatArray(positions.size * 6)
		var o = 0
		for (p in positions) {
			interleaved[o++] = p[0]
			interleaved[o++] = p[1]
			interleaved[o++] = p[2]
			interleaved[o++] = p[3]
			interleaved[o++] = p[4]
			interleaved[o++] = p[5]
		}
		// VBO 顶点buffer
		val vb = ByteBuffer.allocateDirect(interleaved.size * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer()
		vb.put(interleaved)
		vb.position(0)
		val idx = indices.toShortArray()
		// IBO 索引buffer
		val ib = ByteBuffer.allocateDirect(idx.size * 2)
			.order(ByteOrder.nativeOrder())
			.asShortBuffer()
		ib.put(idx)
		ib.position(0)
		return Mesh(vb, ib, idx.size, 6)
	}
}
