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

	// 返回沿着x/y/z轴的加速度
	private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

	// 旋转矩阵（传感器回调直接更新，渲染线程直接读取）
	private val rotationMatrix = FloatArray(16)

	// 偏移矩阵（用于初始姿态校正）
	private val biasMatrix = FloatArray(16)

	// 是否需要初始化偏移矩阵
	private var isFirstFrame = true

	// 是否有有效数据
	private var hasValidData = false

	// EMA滤波状态（滤波后的重力向量）
	private var gxFiltered = 0f
	private var gyFiltered = 0f
	private var gzFiltered = 1f  // 默认指向Z轴

	// EMA滤波系数（0.15 = 保留15%新数据，85%历史）
	private val EMA_ALPHA = 0.15f

	// 死区阈值（约0.5度，归一化向量长度变化）
	private val DEAD_ZONE_THRESHOLD = 0.01f

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
	 * 
	 * 改进：EMA低通滤波 + 死区检查，消除传感器噪声导致的抽动
	 */
	private fun processSensorData(ax: Float, ay: Float, az: Float) {
		// 重力向量归一化
		val gLen = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
		val gxRaw = ax / gLen
		val gyRaw = ay / gLen
		val gzRaw = az / gLen

		// EMA低通滤波：平滑重力向量
		// gxFiltered = α * gxRaw + (1-α) * gxFiltered
		if (isFirstFrame) {
			// 第一帧直接赋值，无历史数据
			gxFiltered = gxRaw
			gyFiltered = gyRaw
			gzFiltered = gzRaw
		} else {
			gxFiltered = EMA_ALPHA * gxRaw + (1 - EMA_ALPHA) * gxFiltered
			gyFiltered = EMA_ALPHA * gyRaw + (1 - EMA_ALPHA) * gyFiltered
			gzFiltered = EMA_ALPHA * gzRaw + (1 - EMA_ALPHA) * gzFiltered
		}

		// 死区检查：如果变化小于阈值，跳过更新（消除静止时的微抖动）
		val delta = sqrt(
			(gxFiltered - gxRaw) * (gxFiltered - gxRaw) +
			(gyFiltered - gyRaw) * (gyFiltered - gyRaw) +
			(gzFiltered - gzRaw) * (gzFiltered - gzRaw)
		)
		if (!isFirstFrame && delta < DEAD_ZONE_THRESHOLD) {
			// 变化太小，使用滤波后的值继续，但不重新计算矩阵
			// 注意：这里仍然使用滤波后的值计算档位
		} else {
			// 变化足够大，重新计算旋转矩阵
			computeRotationMatrixFromGravity(gxFiltered, gyFiltered, gzFiltered, rotationMatrix)
		}

		// 初始化偏移矩阵（第一帧时将当前姿态设为基准）
		if (isFirstFrame) {
			isFirstFrame = false
			Matrix.invertM(biasMatrix, 0, rotationMatrix, 0)
			hasValidData = true
		}

		// 档位逻辑（使用滤波后的值，仅用于触发回调）
		// 新坐标系：俯仰角是设备绕 X 轴的旋转
		// - 水平时，重力沿 -Z（设备 Z 轴向上），gyFiltered 接近 0，gzFiltered 接近 -1
		// - 向上倾斜时，设备 Y 轴指向上方，gyFiltered 变为负数
		// - 向下倾斜时，设备 Y 轴指向下方，gyFiltered 变为正数
		// 俯仰角 = atan2(重力在 Y 轴的分量, 重力在 XZ 平面的分量)
		// 由于 +Z 是向上，-gyFiltered 表示俯仰（负 = 向上看，正 = 向下看）
		val horizontalLen = sqrt((gxFiltered * gxFiltered + gzFiltered * gzFiltered).toDouble()).toFloat()
		val rawPitch = kotlin.math.atan2(-gyFiltered, horizontalLen)
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
	 * 世界坐标系定义（符合 todo.md）：
	 * - +X = 右手边
	 * - +Y = 人脸面向的方向（手机前方）
	 * - +Z = 与重力相反方向（向上）
	 *
	 * 手机正常竖直持握（屏幕面向人脸）：
	 * - 重力沿设备 Y 轴向下
	 * - 相机应该看向 +Y（人脸面向的方向，即球体赤道区域）
	 * - 向上倾斜看到球顶（+Z），向下倾斜看到球底（-Z）
	 * - 向左倾斜看到左边（-X），向右倾斜看到右边（+X）
	 */
	private fun computeRotationMatrixFromGravity(gx: Float, gy: Float, gz: Float, outMatrix: FloatArray) {
		// 重力向量 = (gx, gy, gz)，指向地心
		// 世界坐标系中，-Z 是重力方向（因为 +Z 是与重力相反）
		
		// 归一化重力向量
		val gLen = sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat()
		val ax = gx / gLen
		val ay = gy / gLen
		val az = gz / gLen
		
		// 构建旋转矩阵：将设备坐标系映射到世界坐标系
		// 
		// 策略：假设设备没有绕重力轴的旋转（即假设 roll=0）
		// 世界坐标系定义：
		// - 设备 Y 轴（屏幕上方）-> 世界 Y 轴（forward，人脸面向）
		// - 设备 Z 轴（屏幕向后）-> 世界 -Z 轴（与重力相反，向上）
		// - 设备 X 轴（屏幕向右）-> 世界 X 轴（right）
		
		// 世界坐标系中：
		// - up（与世界 Z 对齐）= -gravity = (-ax, -ay, -az)
		// - 但我们需要设备 Z 指向世界 -Z，所以调整
		
		// 设备的 up 方向（与重力相反）
		val upX = -ax
		val upY = -ay
		val upZ = -az
		
		// 设备 forward 方向：假设 forward 是 Y 轴（屏幕上方指向人脸）
		// 我们需要找到一个与 up 垂直的向量作为 forward
		// 假设 roll=0，即设备的 XZ 平面保持水平
		
		// 计算 East 向量（right）：与 up 和 forward 都垂直
		// 简化计算：假设 forward 在水平面内，我们先计算 right，再得到 forward
		
		// 当重力主要沿 Y 轴时（手机竖直），right 应该沿 X 轴
		// 我们需要构建一个正交基
		
		// 方法：使用重力向量计算 right，然后 right × up = forward
		// right 应该与重力和 Z 轴都垂直
		
		// 简化的正交基构建（假设 roll=0）
		// 参考 getRotationMatrix 但调整坐标轴映射
		
		// 重力在世界坐标系中指向 -Z，所以设备坐标到世界坐标的映射：
		// - 设备 Y 轴 -> 世界 Y 轴（forward）
		// - 设备 -Z 轴 -> 世界 Z 轴（up，与重力相反）
		// - 设备 X 轴 -> 世界 X 轴（right）
		
		// 首先归一化重力
		val invG = 1.0f / gLen
		val gxNorm = gx * invG
		val gyNorm = gy * invG
		val gzNorm = gz * invG
		
		// 世界坐标系中的 Up = -gravity = (-gxNorm, -gyNorm, -gzNorm)
		// 但在这个世界坐标系中，Up 对应的是 +Z
		
		// 构建旋转矩阵（列主序）：
		// 列 0 = right (X轴)
		// 列 1 = forward (Y轴)
		// 列 2 = up (Z轴)
		
		// 由于重力主要检测俯仰，我们需要假设 roll=0
		// 即：设备的 X 轴保持水平（与世界 X 对齐）
		
		// 当手机竖直（重力沿 Y 轴向下）：
		// - right = X 轴 = (1, 0, 0)
		// - up = -gravity = (0, 1, 0) 但这不是世界的 Z...
		
		// 重新理解：我们希望相机看向哪里
		// 手机正常竖直时，相机应该看向 Y+（人脸方向）
		// 手机向上倾斜时，相机应该看向 Y+Z+（向上）
		// 这意味着相机的 forward 应该与重力向量有关
		
		// 解决方案：相机的 forward = 水平面的方向 + 垂直分量
		// 水平面内的 forward 由设备 Y 轴投影到水平面决定
		
		// 计算水平面的法线（重力方向）
		val gnx = gxNorm
		val gny = gyNorm
		val gnz = gzNorm
		
		// 设备 Y 轴在世界坐标系中的方向（假设 roll=0）
		// 我们需要找到与重力垂直的平面内的 forward 方向
		
		// 简化方案：使用与 getRotationMatrix 类似的方法
		// 但交换 Y 和 Z 轴的角色
		
		// 磁场向量近似（指向东，与重力和北都垂直）
		// 假设北是 -Y（设备前方在世界中的投影）
		var Hx = gny  // 使用重力 Y 和 Z 分量
		var Hy = -gnx
		val Hz = 0f
		val invH = 1.0f / sqrt((Hx * Hx + Hy * Hy).toDouble()).toFloat()
		if (invH.isFinite() && invH > 0) {
			Hx *= invH
			Hy *= invH
		}
		
		// 北向量（与重力和东都垂直）
		val Ax = gny * Hz - gnz * Hy
		val Ay = gnz * Hx - gnx * Hz
		val Az = gnx * Hy - gny * Hx
		
		// 现在我们需要映射坐标轴：
		// - 列 0 (right) = East = H
		// - 列 1 (forward) = North = A（但指向设备前方，即 -Y）
		// - 列 2 (up) = -gravity
		
		// 注意：A 指向北，但我们需要它指向设备前方（南 = -北）
		// 所以 forward = -A
		
		// 列主序旋转矩阵
		// 列 0: right (X)
		outMatrix[0] = Hx
		outMatrix[1] = Hy
		outMatrix[2] = Hz
		outMatrix[3] = 0f
		
		// 列 1: forward (Y) = -North = -A，这样看向设备前方
		outMatrix[4] = -Ax
		outMatrix[5] = -Ay
		outMatrix[6] = -Az
		outMatrix[7] = 0f
		
		// 列 2: up (Z) = -gravity（与世界 Z 对齐）
		outMatrix[8] = -gnx
		outMatrix[9] = -gny
		outMatrix[10] = -gnz
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
		// 重置滤波状态
		gxFiltered = 0f
		gyFiltered = 0f
		gzFiltered = 1f
	}

	/**
	 * 渲染线程调用：直接返回 rotationMatrix 引用（参考 MyPanorama）
	 *
	 * 世界坐标系：
	 * - +X = 右手边
	 * - +Y = 人脸面向的方向（相机 forward 方向）
	 * - +Z = 与重力相反方向（向上）
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
		// 列主序（新坐标系定义）：
		// - 列 0 (0,1,2): right (X轴)
		// - 列 1 (4,5,6): forward (Y轴，人脸面向的方向)
		// - 列 2 (8,9,10): up (Z轴，与重力相反)
		val rightX = finalRotation[0]
		val rightY = finalRotation[1]
		val rightZ = finalRotation[2]

		val forwardX = finalRotation[4]
		val forwardY = finalRotation[5]
		val forwardZ = finalRotation[6]

		val upX = finalRotation[8]
		val upY = finalRotation[9]
		val upZ = finalRotation[10]

		// 设置输出 - forward 指向 Y 轴（人脸面向的方向）
		forwardOut[0] = forwardX
		forwardOut[1] = forwardY
		forwardOut[2] = forwardZ

		// up 指向 Z 轴（与重力相反，向上）
		upOut[0] = upX
		upOut[1] = upY
		upOut[2] = upZ

		// eye 在 -forward * radius 处（相机位置）
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
