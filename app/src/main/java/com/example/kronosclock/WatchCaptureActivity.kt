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
import com.example.kronosclock.data.WatchDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WatchCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val watchId = intent.getLongExtra("watch_id", -1L)
        setContent { MaterialTheme { WatchCaptureScreen(watchId) } }
    }
}

@Composable
private fun WatchCaptureScreen(watchId: Long) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentContext by rememberUpdatedState(context)
    val watchDao = remember { WatchDatabase.getInstance(context).watchDao() }
    val scope = rememberCoroutineScope()

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
                                val uri = outputFileResults.savedUri
                                if (uri != null && watchId > 0) {
                                    scope.launch(Dispatchers.IO) {
                                        val bitmap = MediaStore.Images.Media.getBitmap(
                                            currentContext.contentResolver, uri
                                        )
                                        val watchTime = WatchTimeAnalyzer.detectTime(
                                            bitmap, currentContext
                                        )
                                        if (watchTime != null) {
                                            val nowMs = (currentContext.applicationContext as KronosApp)
                                                .kronos
                                                .getCurrentTimeMs()
                                                ?: System.currentTimeMillis()

                                            val actualTime = Instant.ofEpochMilli(nowMs)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalTime()

                                            val offset =
                                                java.time.Duration.between(actualTime, watchTime)
                                                    .toMillis()

                                            val watch = watchDao.get(watchId)
                                            watch?.let {
                                                val prev = it.lastOffsetMs
                                                watchDao.update(
                                                    it.copy(
                                                        lastSyncedEpochMs = nowMs,
                                                        lastOffsetMs = offset
                                                    )
                                                )
                                                withContext(Dispatchers.Main) {
                                                    val diffStr = String.format(
                                                        Locale.US, "%+.2fs", offset / 1000.0
                                                    )
                                                    val drift = prev?.let { o -> offset - o }
                                                    val driftStr = drift?.let {
                                                        String.format(
                                                            Locale.US,
                                                            " (%+.2fs since last)",
                                                            it / 1000.0
                                                        )
                                                    } ?: ""
                                                    Toast.makeText(
                                                        currentContext,
                                                        "Offset $diffStr$driftStr",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    currentContext,
                                                    "Unable to read watch time",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                } else {
                                    val message = "Saved: ${uri?.toString() ?: "MediaStore"}"
                                    Toast.makeText(currentContext, message, Toast.LENGTH_SHORT)
                                        .show()
                                }
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
