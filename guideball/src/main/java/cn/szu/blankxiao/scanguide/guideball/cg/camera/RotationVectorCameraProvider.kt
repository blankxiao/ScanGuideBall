package cn.szu.blankxiao.scanguide.guideball.cg.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix

/**
 * 使用 [SensorManager.getRotationMatrix] / [SensorManager.getRotationMatrixFromVector] 获取设备姿态，
 * 不再手写重力投影、叉积等旋转矩阵构造。
 *
 * 优先级：[TYPE_ROTATION_VECTOR] → [TYPE_GAME_ROTATION_VECTOR] → [TYPE_ACCELEROMETER] + [TYPE_MAGNETIC_FIELD]。
 * 均无则 [getCameraFrame] 回退 [StaticCameraViewProvider]。
 *
 * [SensorManager.getRotationMatrix] 输出的 4×4 为列主序（与 OpenGL 一致）：
 * 列 0/1/2 分别为设备 X/Y/Z 轴在世界坐标系中的单位向量。
 * 相机光轴取 **-Z_device**（屏幕朝里），上方向取 **+Y_device**（屏幕向上）。
 */
class RotationVectorCameraProvider(
	context: Context,
	private val orbitRadius: Float = 4.8f
) : CameraViewProvider {

	private val appContext = context.applicationContext
	private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

	// 旋转向量 受陀螺仪/加速度/磁力影响
	private val rotationVectorSensor: Sensor? =
		sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

	// finalRotation = rotation * bias
	// 旋转矩阵
	private val rotationMatrix = FloatArray(16)
	// 偏移矩阵
	private val noAzimuthAngles = FloatArray(3)
	private val noRollDegRotation = FloatArray(16)

	private var isFirstFrame = true

	private val listener = object : SensorEventListener {
		override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

		override fun onSensorChanged(event: SensorEvent?) {
			if (event == null) return
			when (event.sensor.type) {
				Sensor.TYPE_ROTATION_VECTOR  -> {
					// 将旋转矢量传感器转换为旋转矩阵
					SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
				}
			}
		}
	}


	fun onAttachedToWindow() {
		when {
			rotationVectorSensor != null -> {
				sensorManager.registerListener(
					listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME
				)
			}
		}
	}

	fun onDetachedFromWindow() {
		sensorManager.unregisterListener(listener)
		isFirstFrame = true
	}

	/**
	 * 列主序 R：device X=(R0,R4,R8)，Y=(R1,R5,R9)，Z=(R2,R6,R10)。
	 * 光轴 = -Z_device，上方向 = +Y_device。
	 */
	override fun getCameraFrame(eyeOut: FloatArray, forwardOut: FloatArray, upOut: FloatArray) {
		// finalRotation = rotationMatrix * biasMatrix
		SensorManager.getOrientation(rotationMatrix, noAzimuthAngles)

		val azimuth = Math.toDegrees(noAzimuthAngles[0].toDouble()).toFloat()
		val pitchDeg = Math.toDegrees(noAzimuthAngles[1].toDouble()).toFloat()
		val rollDeg = Math.toDegrees(noAzimuthAngles[2].toDouble()).toFloat()

		Matrix.setIdentityM(noRollDegRotation, 0)
		Matrix.rotateM(noRollDegRotation, 0, azimuth, 0f, 1f, 0f)
		Matrix.rotateM(noRollDegRotation, 0, 90f - pitchDeg, 1f, 0f, 0f)

		val forwardX = -noRollDegRotation[8]
		val forwardY = -noRollDegRotation[9]
		val forwardZ = -noRollDegRotation[10]

		val upX = noRollDegRotation[4]
		val upY = noRollDegRotation[5]
		val upZ = noRollDegRotation[6]

		forwardOut[0] = forwardX
		forwardOut[1] = forwardY
		forwardOut[2] = forwardZ

		upOut[0] = upX
		upOut[1] = upY
		upOut[2] = upZ

		eyeOut[0] = -forwardX * orbitRadius
		eyeOut[1] = -forwardY * orbitRadius
		eyeOut[2] = -forwardZ * orbitRadius
	}
}
