package cn.szu.blankxiao.scanguide.guideball.bridge

/**
 * GL 线程写入当前视线方向（球心→相机，单位向量），传感器/停留逻辑读取快照。
 */
class OrbitDirectionBridge {

	private val lock = Any()
	private val dir = FloatArray(3).also {
		it[0] = 0f
		it[1] = 0f
		it[2] = 1f
	}

	fun setDirectionUnit(x: Float, y: Float, z: Float) {
		synchronized(lock) {
			dir[0] = x
			dir[1] = y
			dir[2] = z
		}
	}

	fun copyDirectionInto(out: FloatArray) {
		synchronized(lock) {
			out[0] = dir[0]
			out[1] = dir[1]
			out[2] = dir[2]
		}
	}
}
