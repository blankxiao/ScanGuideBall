package cn.szu.blankxiao.scanguide.guideball

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * 计算两个角度之间的最小有向角差（度）
 * 范围约 (−180°, 180°]
 *
 * @param currentDeg 当前角度（度）
 * @param referenceDeg 参考角度（度）
 * @return 最小有向角差（度）
 */
private fun shortestSignedAngleDiffDeg(currentDeg: Float, referenceDeg: Float): Float {
	var d = currentDeg - referenceDeg
	while (d > 180f) d -= 360f
	while (d < -180f) d += 360f
	return d
}

/**
 * 在 Compose 中单独注册旋转向量监听，与 GuideBallGlView / AccelOnlyOrientationProvider 解耦
 *
 * 优先使用 Sensor.TYPE_ROTATION_VECTOR，若无则使用 Sensor.TYPE_GAME_ROTATION_VECTOR
 *
 * @param enabled 是否启用传感器监听
 * @param azimuthDeg 方位角状态（度），范围 [0, 360)
 * @param pitchDeg 俯仰角状态（度），范围 [-90, 90]
 * @param rollDeg 横滚角状态（度），范围 [-180, 180]
 * @param azimuthToastThresholdDeg 相对本 Effect 首次采样方位角的水平偏转（绝对值）超过该度数时 Toast 提示一次；null 表示关闭提示
 * @param azimuthToastHysteresisDeg 方位角提示的滞回值（度），用于防止抖动
 */
@Composable
internal fun OrientationAnglesSensorEffect(
	enabled: Boolean,
	azimuthDeg: MutableFloatState,
	pitchDeg: MutableFloatState,
	rollDeg: MutableFloatState,
	azimuthToastThresholdDeg: Float? = 30f,
	azimuthToastHysteresisDeg: Float = 5f
) {
	val appContext = LocalContext.current.applicationContext
	val mainHandler = remember { Handler(Looper.getMainLooper()) }

	DisposableEffect(enabled, appContext, azimuthToastThresholdDeg, azimuthToastHysteresisDeg) {
		if (!enabled) {
			onDispose { }
		} else {
			val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
			val sensor: Sensor? =
				sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
					?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
			if (sensor == null) {
				onDispose { }
			} else {
				val rotationMatrix = FloatArray(16)
				val orientationRad = FloatArray(3)
				var azimuthToastLatched = false
				var referenceAzimuthDeg: Float? = null
				val listener = object : SensorEventListener {
					override fun onSensorChanged(event: SensorEvent?) {
						if (event == null) return
						SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
						SensorManager.getOrientation(rotationMatrix, orientationRad)
						val azimuth = orientationRad[0]
						val pitch = orientationRad[1]
						val roll = orientationRad[2]
						mainHandler.post {
							val azimuthDegValue = Math.toDegrees(azimuth.toDouble()).toFloat()
							azimuthDeg.floatValue = azimuthDegValue
							pitchDeg.floatValue = Math.toDegrees(pitch.toDouble()).toFloat()
							rollDeg.floatValue = Math.toDegrees(roll.toDouble()).toFloat()
							if (referenceAzimuthDeg == null) {
								referenceAzimuthDeg = azimuthDegValue
							}
							val refDeg = referenceAzimuthDeg!!
							val thr = azimuthToastThresholdDeg
							if (thr != null) {
								val clear = (thr - azimuthToastHysteresisDeg).coerceAtLeast(0f)
								val deltaDeg = abs(shortestSignedAngleDiffDeg(azimuthDegValue, refDeg))
								if (deltaDeg > thr) {
									if (!azimuthToastLatched) {
										azimuthToastLatched = true
										Toast.makeText(
											appContext,
											R.string.guideball_azimuth_exceeds_hint,
											Toast.LENGTH_SHORT
										).show()
									}
								} else if (deltaDeg < clear) {
									azimuthToastLatched = false
								}
							}
						}
					}

					override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
				}
				sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
				onDispose {
					sensorManager.unregisterListener(listener)
				}
			}
		}
	}
}
