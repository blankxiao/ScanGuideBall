package cn.szu.blankxiao.scanguide.guideball.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix

/**
 * [Sensor.TYPE_ROTATION_VECTOR] 更新设备旋转矩阵，与 Panorama [GyroOrientationProvider] 同构；
 * 首帧建立 [biasMatrix] 用于「相对初始姿态」。
 */
class GyroOrientationProvider(
	context: Context
) : SensorEventListener {

	private val appContext = context.applicationContext
	private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val sensor: Sensor? =
		sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
			?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

	private val lock = Any()
	private var rotationMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
	private var biasMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
	private var isFirstFrame = true
	private var registered = false

	/**
	 * 将 R×bias 写入 [out]（相对首帧姿态），供 GL 线程调用，与传感器回调互斥避免撕裂。
	 */
	fun copyRelativeRotation(out: FloatArray, outOffset: Int = 0) {
		synchronized(lock) {
			Matrix.multiplyMM(out, outOffset, rotationMatrix, 0, biasMatrix, 0)
		}
	}

	fun onAttached() {
		if (sensor == null || registered) return
		sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
		registered = true
		isFirstFrame = true
	}

	fun onDetached() {
		if (!registered) return
		sensorManager.unregisterListener(this)
		registered = false
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

	override fun onSensorChanged(event: SensorEvent?) {
		if (event == null) return
		if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
			event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR
		) {
			return
		}
		val values = event.values.copyOf()
		synchronized(lock) {
			if (isFirstFrame) {
				isFirstFrame = false
				val orientationMatrix = FloatArray(16)
				SensorManager.getRotationMatrixFromVector(orientationMatrix, values)
				rotationMatrix = orientationMatrix
				val invertMatrix = FloatArray(16)
				Matrix.invertM(invertMatrix, 0, orientationMatrix, 0)
				biasMatrix = invertMatrix
				return
			}
			SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
		}
	}
}
