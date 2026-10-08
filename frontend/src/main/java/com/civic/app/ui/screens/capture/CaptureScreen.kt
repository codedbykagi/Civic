package com.civic.app.ui.screens.capture

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.civic.app.appContainer
import com.civic.app.camera.PhotoStorage
import com.civic.app.data.Draft
import com.civic.app.ui.components.PlaceholderScreen
import kotlinx.coroutines.launch

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Camera screen: live preview, take photo, tag it with GPS + time, then go to the post form. */
@Composable
fun CaptureScreen(onPhotoCaptured: () -> Unit) {
    val context = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    var hasCamera by remember { mutableStateOf(granted(Manifest.permission.CAMERA)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasCamera = granted(Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) {
        if (REQUIRED_PERMISSIONS.any { !granted(it) }) launcher.launch(REQUIRED_PERMISSIONS)
    }

    if (!hasCamera) {
        PlaceholderScreen("Camera permission needed", "Civic needs the camera to photograph issues.") {
            Button(onClick = { launcher.launch(REQUIRED_PERMISSIONS) }) { Text("Grant permission") }
        }
        return
    }

    CameraPreview(onPhotoCaptured)
}

@Composable
private fun CameraPreview(onPhotoCaptured: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    var busy by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).size(72.dp),
            onClick = {
                if (busy) return@FloatingActionButton
                busy = true
                val file = PhotoStorage.newPhotoFile(context)
                val capturedAt = System.currentTimeMillis()
                imageCapture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(file).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            scope.launch {
                                val container = context.appContainer
                                val location = runCatching { container.locationProvider.currentLocation() }.getOrNull()
                                container.draftStore.current = Draft(file.absolutePath, location, capturedAt)
                                busy = false
                                onPhotoCaptured()
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            busy = false
                            Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                )
            },
        ) {
            if (busy) CircularProgressIndicator() else Icon(Icons.Filled.PhotoCamera, contentDescription = "Take photo")
        }

        if (busy) {
            Text(
                "Getting GPS location…",
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            )
        }
    }
}
