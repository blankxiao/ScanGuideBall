package cn.szu.blankxiao.scanguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.szu.blankxiao.scanguide.guideball.GuideBallPlaceholder
import cn.szu.blankxiao.scanguide.ui.theme.ScanGuideBallTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			ScanGuideBallTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					GuideBallPlaceholder(modifier = Modifier.padding(innerPadding))
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun GuideBallDemoPreview() {
	ScanGuideBallTheme {
		GuideBallPlaceholder()
	}
}