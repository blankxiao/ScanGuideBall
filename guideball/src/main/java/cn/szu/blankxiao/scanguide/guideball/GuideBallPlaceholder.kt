package cn.szu.blankxiao.scanguide.guideball

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.szu.blankxiao.scanguide.guideball.view.GuideBallGlView
import java.util.Locale

/**
 * GuideBall 组件占位符
 *
 * @param showOrientationAngles 是否在左上角叠加显示角度信息（方位角/俯仰角/横滚角）
 * @param azimuthToastThresholdDeg 相对首次采样水平朝向的偏转超过该度数时 Toast 提示；null 表示不提示
 */
@Composable
fun GuideBallPlaceholder(
	modifier: Modifier = Modifier,
	showOrientationAngles: Boolean = true,
	azimuthToastThresholdDeg: Float? = 30f
) {
	var completeness by remember { mutableFloatStateOf(0f) }
	var glView by remember { mutableStateOf<GuideBallGlView?>(null) }
	val azimuthState = remember { mutableFloatStateOf(0f) }
	val pitchState = remember { mutableFloatStateOf(0f) }
	val rollState = remember { mutableFloatStateOf(0f) }

	OrientationAnglesSensorEffect(
		enabled = showOrientationAngles,
		azimuthDeg = azimuthState,
		pitchDeg = pitchState,
		rollDeg = rollState,
		azimuthToastThresholdDeg = azimuthToastThresholdDeg
	)

	Box(modifier = modifier.fillMaxSize()) {
		AndroidView(
			factory = { ctx ->
				GuideBallGlView(ctx) { completeness = it }.also { glView = it }
			},
			modifier = Modifier.fillMaxSize()
		)
		if (showOrientationAngles) {
			Text(
				text = String.format(
					Locale.US,
					"方位角%.0f° 俯仰角%.0f° 横滚角%.0f°",
					azimuthState.floatValue,
					pitchState.floatValue,
					rollState.floatValue
				),
				modifier = Modifier
					.align(Alignment.TopStart)
					.padding(8.dp)
					.clip(RoundedCornerShape(6.dp))
					.background(Color(0x88000000))
					.padding(horizontal = 8.dp, vertical = 4.dp),
				color = Color.White,
				style = MaterialTheme.typography.labelSmall
			)
		}
		Box(
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.padding(bottom = 40.dp)
				.size(72.dp)
				.clickable { glView?.togglePause() },
			contentAlignment = Alignment.Center
		) {
			CircularProgressIndicator(
				progress = { completeness },
				modifier = Modifier.size(56.dp)
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun GuideBallPlaceholderPreview() {
	GuideBallPlaceholder()
}
