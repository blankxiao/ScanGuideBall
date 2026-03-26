package cn.szu.blankxiao.scanguide

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cn.szu.blankxiao.scanguide.guideball.GuideBallPlaceholder
import cn.szu.blankxiao.scanguide.ui.theme.ScanGuideBallTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		// 避免桌面图标重复拉起导致任务栈出现两个 MainActivity（返回需要按两次）
		if (!isTaskRoot &&
			intent?.action == Intent.ACTION_MAIN &&
			intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == true
		) {
			finish()
			return
		}
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			ScanGuideBallTheme {
				Box(modifier = Modifier.fillMaxSize()) {
					CameraPreviewContent(modifier = Modifier.fillMaxSize())
					GuideBallPlaceholder(
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.fillMaxWidth()
							.padding(bottom = 8.dp),
						showOrientationAngles = true,
					)
				}
			}
		}
	}
}

@Composable
private fun CameraPreviewContent(modifier: Modifier = Modifier) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	var hasCameraPermission by remember {
		mutableStateOf(
			ContextCompat.checkSelfPermission(
				context,
				Manifest.permission.CAMERA
			) == android.content.pm.PackageManager.PERMISSION_GRANTED
		)
	}
	val permissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { granted -> hasCameraPermission = granted }

	LaunchedEffect(Unit) {
		if (!hasCameraPermission) {
			permissionLauncher.launch(Manifest.permission.CAMERA)
		}
	}

	if (!hasCameraPermission) {
		Box(
			modifier = modifier.background(Color.Black),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = "请授予相机权限以显示预览",
				color = Color.White,
				style = MaterialTheme.typography.bodyMedium
			)
		}
		return
	}

	var previewView: PreviewView? by remember { mutableStateOf(null) }
	AndroidView(
		modifier = modifier,
		factory = { ctx ->
			PreviewView(ctx).apply {
				scaleType = PreviewView.ScaleType.FILL_CENTER
				implementationMode = PreviewView.ImplementationMode.PERFORMANCE
			}.also { previewView = it }
		}
	)
	DisposableEffect(lifecycleOwner, hasCameraPermission, previewView) {
		val view = previewView
		if (view == null || !hasCameraPermission) {
			onDispose { }
		} else {
			val providerFuture = ProcessCameraProvider.getInstance(context)
			val executor = ContextCompat.getMainExecutor(context)
			val listener = Runnable {
				val cameraProvider = providerFuture.get()
				val preview = CameraXPreview.Builder().build().also {
					it.surfaceProvider = view.surfaceProvider
				}
				cameraProvider.unbindAll()
				cameraProvider.bindToLifecycle(
					lifecycleOwner,
					CameraSelector.DEFAULT_BACK_CAMERA,
					preview
				)
			}
			providerFuture.addListener(listener, executor)
			onDispose {
				if (providerFuture.isDone) {
					providerFuture.get().unbindAll()
				}
			}
		}
	}
}

@ComposePreview(showBackground = true)
@Composable
fun GuideBallDemoPreview() {
	ScanGuideBallTheme {
		Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
			GuideBallPlaceholder(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth()
			)
		}
	}
}