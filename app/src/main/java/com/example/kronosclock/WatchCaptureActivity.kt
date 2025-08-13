package com.example.kronosanalogclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kronosanalogclock.data.Watch
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WatchCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as KronosApp
            val db = app.database
            val scope = rememberCoroutineScope()
            var make by remember { mutableStateOf("") }
            var model by remember { mutableStateOf("") }
            val previewView = remember { PreviewView(context) }

            LaunchedEffect(Unit) {
                val cameraProvider = ProcessCameraProvider.getInstance(context).await(context)
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@WatchCaptureActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AndroidView({ previewView }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = make, onValueChange = { make = it }, label = { Text("Make") })
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") })
                Button(
                    onClick = {
                        scope.launch {
                            db.watchDao().insert(
                                Watch(
                                    make = make,
                                    model = model,
                                    lastSyncedEpochMs = System.currentTimeMillis()
                                )
                            )
                            this@WatchCaptureActivity.finish()
                        }
                    },
                    enabled = make.isNotBlank() && model.isNotBlank()
                ) {
                    Text("Set as Synced")
                }
            }
        }
    }
}

private suspend fun <T> ListenableFuture<T>.await(context: android.content.Context): T =
    suspendCancellableCoroutine { cont ->
        addListener({ cont.resume(get()) }, ContextCompat.getMainExecutor(context))
    }
