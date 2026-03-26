package cn.szu.blankxiao.scanguide.guideball.hint

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import cn.szu.blankxiao.scanguide.guideball.R

/**
 * 使用 Toast 提示方位角过大的默认实现
 * 可整体替换为 OrientationHintCallback 的其它实现
 *
 * @param context 上下文
 * @param messageProvider 消息提供函数，用于自定义提示内容
 */
class ToastOrientationHint(
	private val context: Context,
	private val messageProvider: (azimuthRadians: Float, thresholdRadians: Float) -> String = { _, _ ->
		context.applicationContext.getString(R.string.guideball_azimuth_exceeds_hint)
	}
) : OrientationHintCallback {

	private val appContext = context.applicationContext
	private val mainHandler = Handler(Looper.getMainLooper())

	override fun onAzimuthExceedsThreshold(azimuthRadians: Float, thresholdRadians: Float) {
		val text = messageProvider(azimuthRadians, thresholdRadians)
		mainHandler.post {
			Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
		}
	}
}
