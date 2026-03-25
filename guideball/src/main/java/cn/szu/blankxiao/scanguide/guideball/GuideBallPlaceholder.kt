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
import cn.szu.blankxiao.scanguide.guideball.camera.CameraViewProvider
import cn.szu.blankxiao.scanguide.guideball.camera.SensorCameraViewProvider
import cn.szu.blankxiao.scanguide.guideball.view.GuideBallGlView

/**
 * Demo：GL 球体 + 底部圆环进度（与 [SphereScanState.completeness] 同源）；
 * 点击圆环区域切换暂停（停止停留累计与生长时间冻结）。
 *
 * @param cameraViewProvider 为 null 时使用 [SensorCameraViewProvider]；宿主接入请传入 [cn.szu.blankxiao.scanguide.guideball.camera.HostCameraFrameProvider] 并每帧 [HostCameraFrameProvider.setCameraFrame]。
 */
@Composable
fun GuideBallPlaceholder(
	modifier: Modifier = Modifier,
	cameraViewProvider: CameraViewProvider? = null
) {
	val context = LocalContext.current
	val resolvedProvider = remember(cameraViewProvider) {
		cameraViewProvider ?: SensorCameraViewProvider(context)
	}
	var completeness by remember { mutableFloatStateOf(0f) }
	var glView by remember { mutableStateOf<GuideBallGlView?>(null) }

	Box(modifier = modifier.fillMaxSize()) {
		AndroidView(
			factory = { ctx ->
				GuideBallGlView(ctx, { completeness = it }, resolvedProvider).also { glView = it }
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
