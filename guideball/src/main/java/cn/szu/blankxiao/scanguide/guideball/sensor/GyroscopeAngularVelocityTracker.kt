package cn.szu.blankxiao.scanguide.guideball.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * 陀螺仪角速度模长（rad/s），供停留判定使用。
 */
internal class GyroscopeAngularVelocityTracker(
	context: Context,
	private val onAngularSpeedRadPerSec: (Float) -> Unit
) : SensorEventListener {

	private val appContext = context.applicationContext
	private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val gyro: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
	private var registered = false

	fun onAttached() {
		if (gyro == null || registered) return
		sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
		registered = true
	}

	fun onDetached() {
		if (!registered) return
		sensorManager.unregisterListener(this)
		registered = false
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

	override fun onSensorChanged(event: SensorEvent?) {
		if (event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return
		val ax = event.values[0]
		val ay = event.values[1]
		val az = event.values[2]
		val mag = kotlin.math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
		onAngularSpeedRadPerSec(mag)
	}
}
