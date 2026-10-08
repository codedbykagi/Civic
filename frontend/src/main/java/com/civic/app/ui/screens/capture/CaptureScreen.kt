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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.civic.app.appContainer
import com.civic.app.camera.PhotoStorage
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.openAppSettings
import com.civic.shared.model.GeoLocation
import kotlinx.coroutines.launch

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/**
 * Camera screen: live preview, take a photo, tag it with GPS + time and hand it to [onPhotoCaptured]
 * (the caller decides whether it starts a new report, joins the form's draft or attaches to an existing report).
 * [needsLocation] false skips the GPS wait, e.g. when adding a photo to a report that already has a position.
 */
@Composable
fun CaptureScreen(
    onPhotoCaptured: suspend (photoPath: String, capturedAt: Long, location: GeoLocation?) -> Unit,
    needsLocation: Boolean = true,
) {
    val context = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    var hasCamera by remember { mutableStateOf(granted(Manifest.permission.CAMERA)) }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasCamera = granted(Manifest.permission.CAMERA)
        askedOnce = true
    }
    // Re-check when returning from system settings.
    LifecycleResumeEffect(Unit) {
        hasCamera = granted(Manifest.permission.CAMERA)
        onPauseOrDispose {}
    }
    LaunchedEffect(Unit) {
        if (REQUIRED_PERMISSIONS.any { !granted(it) }) launcher.launch(REQUIRED_PERMISSIONS)
    }

    if (!hasCamera) {
        PlaceholderScreen("Camera permission needed", "Civic needs the camera to photograph issues.") {
            // After a denial Android may stop showing the dialog, so offer system settings instead.
            if (askedOnce) {
                Button(onClick = { openAppSettings(context) }) { Text("Open settings") }
            } else {
                Button(onClick = { launcher.launch(REQUIRED_PERMISSIONS) }) { Text("Grant permission") }
            }
        }
        return
    }

    CameraPreview(onPhotoCaptured, needsLocation)
}

@Composable
private fun CameraPreview(
    onPhotoCaptured: suspend (photoPath: String, capturedAt: Long, location: GeoLocation?) -> Unit,
    needsLocation: Boolean,
) {
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
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    } catch (e: Exception) { // e.g. no back camera, or camera in use
                        Toast.makeText(ctx, "Camera unavailable: ${e.message}", Toast.LENGTH_LONG).show()
                    }
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
                                val location = if (needsLocation) {
                                    runCatching { context.appContainer.locationProvider.currentLocation() }.getOrNull()
                                } else {
                                    null
                                }
                                busy = false
                                onPhotoCaptured(file.absolutePath, capturedAt, location)
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

        if (busy && needsLocation) {
            Text(
                "Getting GPS location…",
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            )
        }
    }
}
