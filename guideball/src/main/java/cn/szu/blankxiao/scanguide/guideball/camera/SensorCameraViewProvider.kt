package cn.szu.blankxiao.scanguide.guideball.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * 无宿主时的默认方案：用 [Sensor.TYPE_ROTATION_VECTOR] 将设备后摄光轴（设备系 -Z）映射到世界系，
 * 使引导球视角随手机姿态变化（近似「相机朝向」；与真实 Camera2/AR 外参仍有偏差，生产环境请用 [HostCameraFrameProvider]）。
 */
class SensorCameraViewProvider(
	context: Context,
	private val orbitRadius: Float = StaticCameraViewProvider.DEFAULT_ORBIT_RADIUS
) : CameraViewProvider {

	private val appContext = context.applicationContext
	private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

	private val lock = Any()
	private val r9 = FloatArray(9)
	private val forwardScratch = FloatArray(3)
	private val upScratch = FloatArray(3)
	private val mat4 = FloatArray(16)
	private var alignR: FloatArray? = null

	private var listening = false

	private val listener = object : SensorEventListener {
		override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

		override fun onSensorChanged(event: SensorEvent?) {
			if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
			SensorManager.getRotationMatrixFromVector(r9, event.values)

			synchronized(lock) {
				// R 行主序（SensorManager）：v_device = R * v_world → v_world = R^T * v_device
				// 后摄视线为设备 -Z；上方向为设备 +Y
				val fx = -r9[6]
				val fy = -r9[7]
				val fz = -r9[8]
				normalizeInPlace(fx, fy, fz, forwardScratch)

				val ux = r9[3]
				val uy = r9[4]
				val uz = r9[5]
				normalizeInPlace(ux, uy, uz, upScratch)
				orthogonalizeUpToForward(forwardScratch, upScratch)

				if (alignR == null) {
					alignR = rotationMatrixAlignForwardToZMinus(forwardScratch)
				}
				val a = alignR!!
				multiplyMat3Vec(a, forwardScratch, forwardScratch)
				multiplyMat3Vec(a, upScratch, upScratch)
				normalizeInPlace(forwardScratch[0], forwardScratch[1], forwardScratch[2], forwardScratch)
				orthogonalizeUpToForward(forwardScratch, upScratch)
				normalizeInPlace(upScratch[0], upScratch[1], upScratch[2], upScratch)
			}
		}
	}

	fun onAttachedToWindow() {
		if (listening || rotationSensor == null) return
		sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
		listening = true
	}

	fun onDetachedFromWindow() {
		if (!listening) return
		sensorManager.unregisterListener(listener)
		listening = false
		synchronized(lock) {
			alignR = null
		}
	}

	override fun getCameraFrame(eyeOut: FloatArray, forwardOut: FloatArray, upOut: FloatArray) {
		synchronized(lock) {
			forwardOut[0] = forwardScratch[0]
			forwardOut[1] = forwardScratch[1]
			forwardOut[2] = forwardScratch[2]
			upOut[0] = upScratch[0]
			upOut[1] = upScratch[1]
			upOut[2] = upScratch[2]
		}
		val fx = forwardOut[0]
		val fy = forwardOut[1]
		val fz = forwardOut[2]
		val flen = sqrt((fx * fx + fy * fy + fz * fz).toDouble()).toFloat()
		if (flen < 1e-5f) {
			StaticCameraViewProvider().getCameraFrame(eyeOut, forwardOut, upOut)
			return
		}
		eyeOut[0] = -fx / flen * orbitRadius
		eyeOut[1] = -fy / flen * orbitRadius
		eyeOut[2] = -fz / flen * orbitRadius
	}

	private fun normalizeInPlace(x: Float, y: Float, z: Float, out: FloatArray) {
		val len = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
		if (len < 1e-8f) {
			out[0] = 0f
			out[1] = 0f
			out[2] = -1f
			return
		}
		out[0] = x / len
		out[1] = y / len
		out[2] = z / len
	}

	/** 列主序 3×3 与 vec 相乘 */
	private fun multiplyMat3Vec(m: FloatArray, vInOut: FloatArray, out: FloatArray) {
		val x = m[0] * vInOut[0] + m[3] * vInOut[1] + m[6] * vInOut[2]
		val y = m[1] * vInOut[0] + m[4] * vInOut[1] + m[7] * vInOut[2]
		val z = m[2] * vInOut[0] + m[5] * vInOut[1] + m[8] * vInOut[2]
		out[0] = x
		out[1] = y
		out[2] = z
	}

	private fun orthogonalizeUpToForward(forward: FloatArray, up: FloatArray) {
		val dot = up[0] * forward[0] + up[1] * forward[1] + up[2] * forward[2]
		up[0] -= dot * forward[0]
		up[1] -= dot * forward[1]
		up[2] -= dot * forward[2]
	}

	/** 将单位向量 from 旋转到 (0,0,-1)，返回列主序 3×3（与 [multiplyMat3Vec] 一致） */
	private fun rotationMatrixAlignForwardToZMinus(from: FloatArray): FloatArray {
		val tx = 0f
		val ty = 0f
		val tz = -1f
		val fx = from[0]
		val fy = from[1]
		val fz = from[2]
		var cx = fy * tz - fz * ty
		var cy = fz * tx - fx * tz
		var cz = fx * ty - fy * tx
		val s = sqrt((cx * cx + cy * cy + cz * cz).toDouble()).toFloat()
		val dot = (fx * tx + fy * ty + fz * tz).coerceIn(-1f, 1f)
		if (s < 1e-6f) {
			return if (dot > 0f) {
				floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
			} else {
				Matrix.setRotateM(mat4, 0, 180f, 0f, 1f, 0f)
				extractRot3FromMat4(mat4)
			}
		}
		cx /= s
		cy /= s
		cz /= s
		val angleDeg = (acos(dot) * 180.0 / Math.PI).toFloat()
		Matrix.setRotateM(mat4, 0, angleDeg, cx, cy, cz)
		return extractRot3FromMat4(mat4)
	}

	private fun extractRot3FromMat4(m: FloatArray): FloatArray {
		return floatArrayOf(
			m[0], m[1], m[2],
			m[4], m[5], m[6],
			m[8], m[9], m[10]
		)
	}
}
