@file:Suppress("unused")

package `in`.fivedegree.volineui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import `in`.fivedegree.volineui.button.ButtonSize
import `in`.fivedegree.volineui.button.ButtonStyle
import `in`.fivedegree.volineui.compose.AdvancedButton
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * In-app camera screen.
 *
 * Professional, full-screen camera: black preview, top bar (close, flash,
 * front/back switch), large central shutter, tap-to-focus, pinch-to-zoom,
 * and a Retake / Use photo review step. It never launches an external
 * camera app.
 *
 * Front-camera mirroring (the "recommended" behaviour): CameraX mirrors the
 * front-camera preview so it feels like a mirror, but the saved file is
 * *not* mirrored -- it is saved as captured, so text and faces are not
 * reversed for anyone else viewing the photo. No extra code is needed for
 * this; it is CameraX's default ImageCapture behaviour, and it is documented
 * here so a future change does not "fix" it by mirroring the saved file.
 *
 * Theme: colours come from the MaterialTheme supplied by
 * [VolineCameraActivity], which derives its scheme from the host
 * application's theme. The preview itself is always black.
 */
@Composable
internal fun CameraScreen(
    outputFile: File,
    onUsePhoto: (File) -> Unit,
    onClose: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var lensFacing by rememberSaveable { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by rememberSaveable { mutableStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedPath by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasFlash by remember { mutableStateOf(false) }
    var hasFrontCamera by remember { mutableStateOf(false) }
    var hasBackCamera by remember { mutableStateOf(true) }

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusVisible by remember { mutableStateOf(false) }

    // Report a binding failure exactly once, with no system-camera fallback.
    LaunchedEffect(cameraError) {
        cameraError?.let { onError(it) }
    }

    // Availability of each lens, independent of the current binding.
    LaunchedEffect(Unit) {
        try {
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            hasFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            hasBackCamera = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            if (!hasFrontCamera && !hasBackCamera) {
                cameraError = "No camera is available on this device."
            }
        } catch (e: CancellationException) {
            // The effect was cancelled (e.g. the screen left composition).
            // That is normal teardown, not a camera failure, and must never
            // be reported as an error that finishes the activity.
            throw e
        } catch (e: Exception) {
            cameraError = "Camera could not be started: ${e.message}"
        }
    }

    // Bind / rebind the camera whenever the lens changes. The finally block
    // unbinds when the lens changes or the screen leaves composition, so the
    // camera is never left bound to a dead lifecycle.
    LaunchedEffect(previewView, lensFacing) {
        val view = previewView ?: return@LaunchedEffect
        var provider: ProcessCameraProvider? = null
        try {
            provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val selector = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            if (!provider.hasCamera(selector)) {
                cameraError = "The selected camera is not available on this device."
                return@LaunchedEffect
            }
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .build()
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                capture
            )
            camera = boundCamera
            imageCapture = capture
            hasFlash = boundCamera.cameraInfo.hasFlashUnit()
            // Keep the preview rotation in step with the display.
            // PreviewView handles this for the preview; ImageCapture reads
            // the target rotation from the capture use case at capture time.
            capture.targetRotation = view.display?.rotation ?: android.view.Surface.ROTATION_0
            // Wait until this effect is cancelled (lens change / dispose),
            // then release the camera in finally.
            kotlinx.coroutines.awaitCancellation()
        } catch (e: CancellationException) {
            // Normal teardown / lens-change restart: the effect is simply
            // restarted or disposed. Previously this was caught by the
            // generic Exception handler below, which set cameraError and
            // finished the activity with an error instead of rebinding.
            // Re-throw so cancellation stays cancellation.
            throw e
        } catch (e: Exception) {
            if (cameraError == null) {
                cameraError = "Camera could not be started: ${e.message}"
            }
        } finally {
            try {
                provider?.unbindAll()
            } catch (_: Exception) {
                // Unbinding during teardown must never crash the screen.
            }
            camera = null
            imageCapture = null
        }
    }

    // Flash changes apply to the already-bound capture use case.
    LaunchedEffect(flashMode, imageCapture) {
        imageCapture?.flashMode = flashMode
    }

    fun takePhoto() {
        val capture = imageCapture ?: run {
            onError("Camera is not ready yet. Please try again.")
            return
        }
        if (isCapturing) return
        isCapturing = true
        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    capturedPath = outputFile.absolutePath
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    // No fallback to a system camera app, by design.
                    onError("Failed to capture photo: ${exception.message}")
                }
            }
        )
    }

    val reviewBitmap: Bitmap? = remember(capturedPath) {
        capturedPath?.let { decodeReviewBitmap(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedPath == null) {
            // Preview, tap-to-focus and pinch-to-zoom.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera) {
                        detectTapGestures { offset ->
                            val view = previewView ?: return@detectTapGestures
                            val cam = camera ?: return@detectTapGestures
                            focusPoint = offset
                            focusVisible = true
                            try {
                                val point = view.meteringPointFactory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                cam.cameraControl.startFocusAndMetering(action)
                            } catch (_: Exception) {
                                // Focus is a best-effort enhancement; a failure
                                // must not block capture.
                            }
                            coroutineScope.launch {
                                delay(1500)
                                focusVisible = false
                            }
                        }
                    }
                    .pointerInput(camera) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            val cam = camera ?: return@detectTransformGestures
                            val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                            val newRatio = (zoomState.zoomRatio * zoomChange)
                                .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                            cam.cameraControl.setZoomRatio(newRatio)
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Focus ring.
                if (focusVisible && focusPoint != null) {
                    val point = focusPoint!!
                    val ringSize = 80.dp
                    val ringPx = with(density) { ringSize.toPx() }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (point.x - ringPx / 2).toInt(),
                                    (point.y - ringPx / 2).toInt()
                                )
                            }
                            .size(ringSize)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

            // Top bar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close camera",
                        tint = Color.White
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasFlash) {
                        val (flashIcon, flashDescription) = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn to "Flash on"
                            ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff to "Flash off"
                            else -> Icons.Filled.FlashAuto to "Flash auto"
                        }
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                    else -> ImageCapture.FLASH_MODE_AUTO
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = flashIcon,
                                contentDescription = flashDescription,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (hasFrontCamera && hasBackCamera) {
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                    CameraSelector.LENS_FACING_BACK
                                } else {
                                    CameraSelector.LENS_FACING_FRONT
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cameraswitch,
                                contentDescription = "Switch camera",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom shutter bar.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, Color.White, CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Take photo"
                            ) {
                                // Haptic feedback is provided by the system
                                // through the clickable; the shutter itself
                                // stays a single, unambiguous control.
                                takePhoto()
                            }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to capture",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // Review step: Retake / Use photo.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (reviewBitmap != null) {
                        Image(
                            bitmap = reviewBitmap.asImageBitmap(),
                            contentDescription = "Captured photo preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Photo captured",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdvancedButton(
                        text = "Retake",
                        onClick = {
                            capturedPath = null
                            try {
                                outputFile.delete()
                            } catch (_: Exception) {
                                // The next capture overwrites the file.
                            }
                        },
                        style = ButtonStyle.OUTLINED,
                        size = ButtonSize.M,
                        fullWidth = false,
                        modifier = Modifier.weight(1f)
                    )
                    AdvancedButton(
                        text = "Use photo",
                        onClick = { onUsePhoto(outputFile) },
                        style = ButtonStyle.FILLED,
                        size = ButtonSize.M,
                        fullWidth = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Decode a downscaled bitmap for the review screen only. The full-resolution
 * file is what gets returned to the manager; the review bitmap must never
 * be used as the saved photo, to avoid holding a full-resolution bitmap in
 * composition memory.
 */
private fun decodeReviewBitmap(path: String): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        val maxDimension = 1080
        while (bounds.outWidth / sampleSize > maxDimension ||
            bounds.outHeight / sampleSize > maxDimension
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeFile(path, options)
    } catch (_: Exception) {
        null
    }
}
