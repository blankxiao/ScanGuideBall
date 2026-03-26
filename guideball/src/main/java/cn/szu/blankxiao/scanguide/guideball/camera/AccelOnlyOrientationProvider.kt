package cn.szu.blankxiao.scanguide.guideball.camera

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
class AccelOnlyOrientationProvider(
	context: Context,
	private val orbitRadius: Float = StaticCameraViewProvider.DEFAULT_ORBIT_RADIUS
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
	private val biasMatrix = FloatArray(16)
	// 方位角/俯仰角/弧度
	private val orientationAngles = FloatArray(3)
	// 存在加速度数据
	private var hasGravitySample = false
	// 存在磁力数据
	private var hasMagSample = false

	private var isFirstFrame = true
	private var hasValidData = false

	private val PITCH_LEVELS = floatArrayOf(
		1.0f,
		0.5f,
		0.0f,
		-0.5f,
		-1.0f
	)
	private val HYSTERESIS = 0.15f
	private var currentLevelIndex = 2
	private var onLevelChanged: (() -> Unit)? = null

	private val listener = object : SensorEventListener {
		override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

		override fun onSensorChanged(event: SensorEvent?) {
			if (event == null) return
			when (event.sensor.type) {
				Sensor.TYPE_ROTATION_VECTOR  -> {
					// 将旋转矢量传感器转换为旋转矩阵
					SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
					onRotationMatrixUpdated()
				}
			}
		}
	}

	private fun onRotationMatrixUpdated() {
		if (isFirstFrame) {
			isFirstFrame = false
			// 初始视角
			Matrix.invertM(biasMatrix, 0, rotationMatrix, 0)
			hasValidData = true
		}
		// rotationMatrix 经过SensorManager.getRotationMatrixFromVector处理 由旋转矢量传感器的向量转换过来
		// 解析出 方位角/俯仰角/横滚
		SensorManager.getOrientation(rotationMatrix, orientationAngles)
		val rawPitch = orientationAngles[1]
		val oldLevel = currentLevelIndex
		updatePitchLevel(rawPitch)
		if (currentLevelIndex != oldLevel) {
			onLevelChanged?.invoke()
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
		hasValidData = false
		isFirstFrame = true
		hasGravitySample = false
		hasMagSample = false
	}

	/**
	 * 列主序 R：device X=(R0,R4,R8)，Y=(R1,R5,R9)，Z=(R2,R6,R10)。
	 * 光轴 = -Z_device，上方向 = +Y_device。
	 */
	override fun getCameraFrame(eyeOut: FloatArray, forwardOut: FloatArray, upOut: FloatArray) {
		if (!hasValidData) {
			StaticCameraViewProvider().getCameraFrame(eyeOut, forwardOut, upOut)
			return
		}

		val finalRotation = FloatArray(16)
		Matrix.multiplyMM(finalRotation, 0, rotationMatrix, 0, biasMatrix, 0)

		val forwardX = -finalRotation[2]
		val forwardY = -finalRotation[6]
		val forwardZ = -finalRotation[10]

		val upX = finalRotation[1]
		val upY = finalRotation[5]
		val upZ = finalRotation[9]

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

	private fun updatePitchLevel(rawPitch: Float) {
		val currentLevel = PITCH_LEVELS[currentLevelIndex]

		if (currentLevelIndex > 0) {
			val upperLevel = PITCH_LEVELS[currentLevelIndex - 1]
			val upperThreshold = (currentLevel + upperLevel) / 2 + HYSTERESIS
			if (rawPitch > upperThreshold) {
				currentLevelIndex--
				return
			}
		}

		if (currentLevelIndex < PITCH_LEVELS.size - 1) {
			val lowerLevel = PITCH_LEVELS[currentLevelIndex + 1]
			val lowerThreshold = (currentLevel + lowerLevel) / 2 - HYSTERESIS
			if (rawPitch < lowerThreshold) {
				currentLevelIndex++
				return
			}
		}
	}
}
