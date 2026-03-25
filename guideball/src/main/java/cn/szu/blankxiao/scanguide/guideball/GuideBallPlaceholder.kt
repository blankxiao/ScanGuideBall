package cn.szu.blankxiao.scanguide.guideball

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import cn.szu.blankxiao.scanguide.guideball.view.GuideBallGlView

/**
 * Demo 入口：全屏 [GuideBallGlView]（球体 + 透视 + 陀螺仪驱动模型矩阵）。
 */
@Composable
fun GuideBallPlaceholder(modifier: Modifier = Modifier) {
	AndroidView(
		factory = { ctx -> GuideBallGlView(ctx) },
		modifier = modifier.fillMaxSize()
	)
}

@Preview(showBackground = true)
@Composable
private fun GuideBallPlaceholderPreview() {
	GuideBallPlaceholder()
}
