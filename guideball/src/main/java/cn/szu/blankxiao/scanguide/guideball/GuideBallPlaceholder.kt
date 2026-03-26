package cn.szu.blankxiao.scanguide.guideball

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.szu.blankxiao.scanguide.guideball.view.GuideBallGlView

/**
 * GuideBall 组件占位符
 * 纯加速度方案，无磁场，无陀螺仪
 *
 * 原理：
 * - 仅用加速度计检测重力方向
 * - 视图矩阵由传感器数据实时计算
 */
@Composable
fun GuideBallPlaceholder(
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	var completeness by remember { mutableFloatStateOf(0f) }
	var glView by remember { mutableStateOf<GuideBallGlView?>(null) }

	Box(modifier = modifier.fillMaxSize()) {
		AndroidView(
			factory = { ctx ->
				GuideBallGlView(ctx) { completeness = it }.also { glView = it }
			},
			modifier = Modifier.fillMaxSize()
		)
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
