package cn.szu.blankxiao.scanguide.guideball

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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

@Composable
fun GuideBallPlaceholder(
	modifier: Modifier = Modifier,
	showOrientationAngles: Boolean = true,
	azimuthToastThresholdDeg: Float? = 30f,
	sphereSizeDp: Int = 192,
	progressContainerSizeDp: Int = 64
) {
	var completeness by remember { mutableFloatStateOf(0f) }
	var isPaused by remember { mutableStateOf(false) }
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

	Column(
		modifier = modifier
			.fillMaxWidth()
			.wrapContentHeight()
			.padding(horizontal = 24.dp)
			.padding(bottom = 24.dp)
	) {
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
					.padding(bottom = 6.dp)
					.clip(RoundedCornerShape(6.dp))
					.background(Color(0x88000000))
					.padding(horizontal = 8.dp, vertical = 4.dp),
				color = Color.White,
				style = MaterialTheme.typography.labelSmall
			)
		}

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.wrapContentHeight()
		) {
			AndroidView(
				factory = { ctx ->
					GuideBallGlView(
						context = ctx,
						onCompletenessChanged = { completeness = it },
						onPausedChanged = { isPaused = it }
					).also { glView = it }
				},
				modifier = Modifier
					.align(Alignment.Center)
					.size(sphereSizeDp.dp)
					.clip(RoundedCornerShape(24.dp))
					.border(
						width = 1.5.dp,
						color = Color.White,
						shape = RoundedCornerShape(24.dp)
					)
			)
			Box(
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(end = 6.dp)
					.size(progressContainerSizeDp.dp)
					.clickable { isPaused = glView?.togglePause() ?: isPaused },
				contentAlignment = Alignment.BottomCenter
			) {
				CircularProgressIndicator(
					progress = { completeness },
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.size((progressContainerSizeDp).dp),
					color = if (isPaused) Color(0xFF9AA0A6) else MaterialTheme.colorScheme.primary,
					trackColor = Color(0x44FFFFFF)
				)
				Text(
					text = String.format(Locale.US, "%.0f%%", completeness * 100f),
					modifier = Modifier.align(Alignment.Center),
					color = Color.White,
					style = MaterialTheme.typography.labelSmall
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun GuideBallPlaceholderPreview() {
	GuideBallPlaceholder()
}

