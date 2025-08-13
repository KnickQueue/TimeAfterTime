package com.example.kronosclock

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WatchCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WatchCaptureScreen() } }
    }
}

@Composable
private fun WatchCaptureScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentContext by rememberUpdatedState(context)

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_CAPTURE or
                    LifecycleCameraController.VIDEO_CAPTURE or
                    LifecycleCameraController.IMAGE_ANALYSIS
            )
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { }
    }

    val previewView = remember { PreviewView(context).apply { this.controller = controller } }

    var hasCamera by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCamera = granted
        if (!granted) {
            Toast.makeText(currentContext, "Camera permission denied", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) hasCamera = true else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { previewView }
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = hasCamera,
                onClick = {
                    val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(System.currentTimeMillis())
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "Kronos_$fileName.jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KronosClock")
                        }
                    }

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ).build()

                    controller.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(
                                    currentContext,
                                    "Capture failed: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val uri = outputFileResults.savedUri?.toString() ?: "MediaStore"
                                Toast.makeText(
                                    currentContext,
                                    "Saved: $uri",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            ) { Text("Capture") }

            FilledTonalButton(
                enabled = hasCamera,
                onClick = {
                    val hasFlash = controller.cameraInfo?.hasFlashUnit() == true
                    if (hasFlash) {
                        torchEnabled = !torchEnabled
                        controller.enableTorch(torchEnabled)
                    } else {
                        Toast.makeText(context, "Torch not available", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text(if (torchEnabled) "Torch Off" else "Torch On") }
        }
    }
}
