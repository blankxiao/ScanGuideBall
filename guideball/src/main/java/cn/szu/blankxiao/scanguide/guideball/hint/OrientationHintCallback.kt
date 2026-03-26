package cn.szu.blankxiao.scanguide.guideball.hint

/**
 * 姿态相关提示回调接口
 * 由宿主实现或替换为 Snackbar / 语音等提示方式
 */
fun interface OrientationHintCallback {

	/**
	 * 当方位角绝对值超过阈值时调用（实现侧通常带滞回）
	 *
	 * @param azimuthRadians 当前方位角（弧度），与 SensorManager.getOrientation 的返回值[0]一致
	 * @param thresholdRadians 当前生效的阈值（弧度）
	 */
	fun onAzimuthExceedsThreshold(azimuthRadians: Float, thresholdRadians: Float)
}
