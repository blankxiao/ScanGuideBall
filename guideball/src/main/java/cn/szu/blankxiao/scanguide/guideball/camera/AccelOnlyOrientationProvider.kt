package cn.szu.blankxiao.scanguide.guideball.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import kotlin.math.sqrt

/**
 * 纯加速度方案：无磁场，无陀螺仪。
 * 参考 MyPanorama 的 GyroOrientationProvider 实现：
 * - 传感器回调直接更新 rotationMatrix
 */
class AccelOnlyOrientationProvider(
	context: Context,
	private val orbitRadius: Float = StaticCameraViewProvider.DEFAULT_ORBIT_RADIUS
) : CameraViewProvider {

	private val appContext = context.applicationContext
	private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

	private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

	// 旋转矩阵（传感器回调直接更新，渲染线程直接读取）
	private val rotationMatrix = FloatArray(16)

	// 偏移矩阵（用于初始姿态校正）
	private val biasMatrix = FloatArray(16)

	// 是否需要初始化偏移矩阵
	private var isFirstFrame = true

	// 是否有有效数据
	private var hasValidData = false

	// 档位定义（俯仰角，弧度）：上、上中、水平、下中、下
	private val PITCH_LEVELS = floatArrayOf(
		1.0f,    // ~57° 向上看天
		0.5f,    // ~29° 向上看
		0.0f,    // 0° 水平
		-0.5f,   // ~-29° 向下看
		-1.0f    // ~-57° 向下看地
	)

	// 滞后阈值
	private val HYSTERESIS = 0.15f  // 约8.6°

	// 当前档位索引
	private var currentLevelIndex = 2  // 默认水平

	// 档位变化回调
	private var onLevelChanged: (() -> Unit)? = null

	private val listener = object : SensorEventListener {
		override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

		override fun onSensorChanged(event: SensorEvent?) {
			if (event == null) return

			if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
				processSensorData(event.values[0], event.values[1], event.values[2])
			}
		}
	}

	/**
	 * 设置档位变化回调
	 */
	fun setOnLevelChanged(callback: () -> Unit) {
		onLevelChanged = callback
	}

	/**
	 * 核心：传感器回调中直接计算并更新 rotationMatrix
	 * 参考 MyPanorama 的 GyroOrientationProvider.onSensorChanged
	 */
	private fun processSensorData(ax: Float, ay: Float, az: Float) {
		// 重力向量归一化
		val gLen = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
		val gx = ax / gLen
		val gy = ay / gLen
		val gz = az / gLen

		// 从重力向量计算旋转矩阵
		// 重力方向是 -Y，我们需要构建一个从设备坐标系到世界坐标系的旋转
		computeRotationMatrixFromGravity(gx, gy, gz, rotationMatrix)

		// 初始化偏移矩阵（第一帧时将当前姿态设为基准）
		if (isFirstFrame) {
			isFirstFrame = false
			Matrix.invertM(biasMatrix, 0, rotationMatrix, 0)
			hasValidData = true
		}

		// 档位逻辑（仅用于触发回调，不影响矩阵计算）
		val horizontalLen = sqrt((gx * gx + gz * gz).toDouble()).toFloat()
		val rawPitch = kotlin.math.atan2(-gy, horizontalLen)
		val oldLevel = currentLevelIndex
		updatePitchLevel(rawPitch)
		if (currentLevelIndex != oldLevel) {
			onLevelChanged?.invoke()
		}
	}

	/**
	 * 从重力向量计算旋转矩阵
	 * 参考 Android SensorManager.getRotationMatrix 的逻辑
	 * 
	 * 世界坐标系：Y轴向上，Z轴向前（负方向看向球心）
	 * 手机平放时（重力向下 Y-），相机看向 Z-（前方）
	 * 手机向上倾斜时（俯仰增加），相机向上看（看向 Y+）
	 */
	private fun computeRotationMatrixFromGravity(gx: Float, gy: Float, gz: Float, outMatrix: FloatArray) {
		// 重力向量 = (gx, gy, gz)，指向地心
		// 世界坐标系中，-Y 是重力方向，所以我们需要一个旋转矩阵
		// 将设备的加速度坐标系与世界坐标系对齐
		
		// 归一化重力向量
		val gLen = sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat()
		val ax = gx / gLen
		val ay = gy / gLen
		val az = gz / gLen
		
		// 重力向量在世界坐标系中是 (0, -1, 0)
		// 我们需要找到设备的朝向
		// 
		// 策略：假设设备没有绕重力轴的旋转（即假设 roll=0）
		// 这样可以构建一个有效的旋转矩阵
		//
		// 设备的 Y 轴（up）应该是 -gravity
		// 设备的 Z 轴（forward）应该与 Y 轴垂直，且在水平面内
		// 设备的 X 轴（right）由 Y × Z 得到
		
		// 简化的 getRotationMatrix：假设没有磁场，roll=0
		// 参考 https://developer.android.com/reference/android/hardware/SensorManager#getRotationMatrix(float[],%20float[],%20float[],%20float[])
		
		var Hx = ay
		var Hy = -ax
		val Hz = 0f
		val invH = 1.0f / sqrt((Hx * Hx + Hy * Hy).toDouble()).toFloat()
		Hx *= invH
		Hy *= invH
		
		val Mx = ay * Hz - az * Hy
		val My = az * Hx - ax * Hz
		val Mz = ax * Hy - ay * Hx
		
		// 列主序的旋转矩阵
		// 列 0: right (East)
		outMatrix[0] = Hx
		outMatrix[1] = Hy
		outMatrix[2] = Hz
		outMatrix[3] = 0f
		
		// 列 1: up (gravity inverse)
		outMatrix[4] = -ax
		outMatrix[5] = -ay
		outMatrix[6] = -az
		outMatrix[7] = 0f
		
		// 列 2: forward (North)
		outMatrix[8] = Mx
		outMatrix[9] = My
		outMatrix[10] = Mz
		outMatrix[11] = 0f
		
		// 列 3: translation
		outMatrix[12] = 0f
		outMatrix[13] = 0f
		outMatrix[14] = 0f
		outMatrix[15] = 1f
	}

	fun onAttachedToWindow() {
		accelerometer?.let {
			sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
		}
	}

	fun onDetachedFromWindow() {
		sensorManager.unregisterListener(listener)
		hasValidData = false
		isFirstFrame = true
	}

	/**
	 * 渲染线程调用：直接返回 rotationMatrix 引用（参考 MyPanorama）
	 */
	override fun getCameraFrame(eyeOut: FloatArray, forwardOut: FloatArray, upOut: FloatArray) {
		if (!hasValidData) {
			StaticCameraViewProvider().getCameraFrame(eyeOut, forwardOut, upOut)
			return
		}

		// 应用偏移矩阵：finalRotation = rotationMatrix * biasMatrix
		val finalRotation = FloatArray(16)
		Matrix.multiplyMM(finalRotation, 0, rotationMatrix, 0, biasMatrix, 0)

		// 从旋转矩阵提取方向向量
		// 列主序：0,1,2 = 第一列 = rightX, rightY, rightZ
		//        4,5,6 = 第二列 = upX, upY, upZ
		//        8,9,10 = 第三列 = forwardX, forwardY, forwardZ
		val upX = finalRotation[4]
		val upY = finalRotation[5]
		val upZ = finalRotation[6]

		val forwardX = finalRotation[8]
		val forwardY = finalRotation[9]
		val forwardZ = finalRotation[10]

		// 设置输出
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

	/**
	 * 获取旋转矩阵（参考 MyPanorama 的 getRotationMatrix）
	 */
	fun getRotationMatrix(): FloatArray = rotationMatrix

	fun getBiasMatrix(): FloatArray = biasMatrix

	/**
	 * 重置初始姿态（参考 MyPanorama 的 reCenter）
	 */
	fun resetOrientation() {
		if (hasValidData) {
			Matrix.invertM(biasMatrix, 0, rotationMatrix, 0)
		}
	}

	/**
	 * 档位更新逻辑
	 */
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
