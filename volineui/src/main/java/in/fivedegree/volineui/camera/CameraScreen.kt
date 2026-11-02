@file:Suppress("unused")

package `in`.fivedegree.volineui.camera

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * In-app camera screen.
 *
 * Professional camera: black preview sized to the selected aspect
 * ratio (not stretched to fill the screen), top status pill (lens +
 * flash), large central shutter with aspect-ratio pill and
 * switch-camera on either side, tap-to-focus, pinch-to-zoom, an
 * iPhone-style ruler zoom control driven by the bound camera's own
 * zoom range (swipe left to zoom in, right to zoom out), an
 * aspect-ratio switch (4:3 / 16:9, persisted across opens), and a
 * Retake / Use photo review step with plain text buttons. It never
 * launches an external camera app.
 *
 * Defaults: back camera, flash off. The lens is deliberately not
 * persisted (unlike the aspect ratio), so every fresh camera open
 * starts on the back camera even if a previous open-ended on front;
 * rememberSaveable only preserves the lens across recreation of the
 * same open, e.g. a rotation.
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
	modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var lensFacing by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by rememberSaveable { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
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

    // Aspect ratio, remembered across camera opens in SharedPreferences
    // (rememberSavable alone only survives recreation of this activity).
    // CameraX only offers 4:3 and 16:9 target ratios, so those are the
    // only options offered; anything else would silently fall back.
    var aspect by remember { mutableStateOf(loadCameraAspect(context)) }
    LaunchedEffect(aspect) { saveCameraAspect(context, aspect) }

    // Zoom range, read from the bound camera (never hardcoded): some
    // devices have a fixed lens (max == min) and then the ruler hides.
    var zoomMin by remember { mutableFloatStateOf(1f) }
    var zoomMax by remember { mutableFloatStateOf(1f) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    fun applyZoom(ratio: Float) {
        val cam = camera ?: return
        val state = cam.cameraInfo.zoomState.value ?: return
        val clamped = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        zoomRatio = clamped
        cam.cameraControl.setZoomRatio(clamped)
    }

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

    // Bind / rebind the camera whenever the lens or aspect ratio changes.
    // The finally block unbinds when the lens changes or the screen leaves
    // composition, so the camera is never left bound to a dead lifecycle.
    LaunchedEffect(previewView, lensFacing, aspect) {
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
            val preview = Preview.Builder()
                .setTargetAspectRatio(aspect.cameraRatio)
                .build().also {
                    it.surfaceProvider = view.surfaceProvider
                }
            val capture = ImageCapture.Builder()
                .setTargetAspectRatio(aspect.cameraRatio)
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
            // Zoom range for this lens; the ruler slider and pinch both
            // read and write zoomRatio, so they always stay in sync.
            // A fresh bind starts at 1x (clamped into the new range).
            val zoomState = boundCamera.cameraInfo.zoomState.value
            if (zoomState != null) {
                zoomMin = zoomState.minZoomRatio
                zoomMax = zoomState.maxZoomRatio
                zoomRatio = 1f.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                boundCamera.cameraControl.setZoomRatio(zoomRatio)
            } else {
                zoomMin = 1f
                zoomMax = 1f
                zoomRatio = 1f
            }
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
            // Preview, sized to the selected aspect ratio instead of
            // filling the whole screen. In portrait a 4:3 capture is
            // displayed 3:4 and a 16:9 capture 9:16, with black bars
            // above and below; in landscape the label ratio applies
            // directly. FIT_CENTER means the preview frame is the
            // captured frame, not a centre crop of it, so what is
            // shown is what the photo will contain.
            //
            // Pinch-to-zoom stays on the full-screen box (bars
            // included). Tap-to-focus lives on the preview box
            // itself, so its coordinates are already in PreviewView
            // space -- on the old full-screen layout the two boxes
            // were the same size, but they are not any more, and a
            // tap measured against the full screen would focus the
            // wrong point on an aspect-sized preview.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            val cam = camera ?: return@detectTransformGestures
                            val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                            val newRatio = (zoomState.zoomRatio * zoomChange)
                                .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                            // Keep the ruler position in sync with pinch zoom.
                            zoomRatio = newRatio
                            cam.cameraControl.setZoomRatio(newRatio)
                        }
                    }
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val labelRatio = if (aspect == CameraAspect.FOUR_THREE) 4f / 3f else 16f / 9f
                    val displayRatio = if (LocalConfiguration.current.orientation ==
                        Configuration.ORIENTATION_PORTRAIT
                    ) {
                        1f / labelRatio
                    } else {
                        labelRatio
                    }
                    val previewWidth = minOf(maxWidth, maxHeight * displayRatio)
                    val previewHeight = previewWidth / displayRatio
                    Box(
                        modifier = Modifier
                            .size(previewWidth, previewHeight)
                            .align(Alignment.Center)
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
                                        delay(1500.milliseconds)
                                        focusVisible = false
                                    }
                                }
                            }
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FIT_CENTER
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
                // Centre pill: live lens + flash state, so the top bar
                // reads as a camera status bar, not just two buttons.
                // Left (48dp close) and right (48dp flash/spacer) match,
                // so the pill stays centred.
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buildString {
                            append(if (lensFacing == CameraSelector.LENS_FACING_FRONT) "Front" else "Back")
                            if (hasFlash) {
                                append(" • ")
                                append(
                                    when (flashMode) {
                                        ImageCapture.FLASH_MODE_ON -> "Flash on"
                                        ImageCapture.FLASH_MODE_OFF -> "Flash off"
                                        else -> "Flash auto"
                                    }
                                )
                            }
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
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
                    // Switch-camera moved to the bottom bar, next to the
                    // shutter, like a native camera. Keep a 48dp spacer
                    // here when there is no flash, so the centre pill
                    // stays centred between equal left/right widths.
                    if (!hasFlash) {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }

            // Bottom control cluster, over a gradient scrim so the
            // controls stay readable over any preview: ruler zoom
            // control, zoom readout, then aspect pill / shutter /
            // switch-camera in a row whose equal side weights keep
            // the shutter centred.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCapturing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    if (zoomMax > zoomMin) {
                        ZoomRuler(
                            zoomRatio = zoomRatio,
                            zoomMin = zoomMin,
                            zoomMax = zoomMax,
                            onZoomChange = { applyZoom(it) }
                        )
                        // Tapping the readout resets to 1x.
                        Text(
                            text = String.format(Locale.US, "%.1f×", zoomRatio),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Reset zoom to 1x"
                                ) { applyZoom(1f) }
                                .padding(bottom = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aspect ratio pill: tap cycles 4:3 -> 16:9.
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.4f),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Change aspect ratio"
                                    ) { aspect = aspect.next() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = aspect.label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(4.dp, Color.White, CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Take photo"
                                ) { takePhoto() }
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
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
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
                                        .background(
                                            Color.Black.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Cameraswitch,
                                        contentDescription = "Switch camera",
                                        tint = Color.White
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }
                    }
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
                // Both buttons are plain text buttons with the same look,
                // as requested. They deliberately do NOT use AdvancedButton:
                // that component maps a pure-white text colour to black
                // for TEXT/OUTLINED styles, which is exactly what made
                // Retake invisible (black text on this black screen).
                // Material3 TextButton with explicit white text has no
                // such mapping, so both buttons are actually readable.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            capturedPath = null
                            try {
                                outputFile.delete()
                            } catch (_: Exception) {
                                // The next capture overwrites the file.
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Retake", color = Color.White)
                    }
                    TextButton(
                        onClick = { onUsePhoto(outputFile) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Use photo", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Aspect ratios CameraX actually supports as target ratios. 1:1 is not
 * one of them, so it is not offered: offering it would silently fall
 * back to one of these and mislead the user about the captured photo.
 */
private enum class CameraAspect(val label: String, val cameraRatio: Int) {
    FOUR_THREE("4:3", AspectRatio.RATIO_4_3),
    SIXTEEN_NINE("16:9", AspectRatio.RATIO_16_9);

    fun next(): CameraAspect = if (this == FOUR_THREE) SIXTEEN_NINE else FOUR_THREE
}

private const val CAMERA_PREFS_NAME = "voline_camera"
private const val CAMERA_PREFS_ASPECT = "aspect_ratio"

private fun loadCameraAspect(context: Context): CameraAspect {
    val stored = context.getSharedPreferences(CAMERA_PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(CAMERA_PREFS_ASPECT, 0)
    return if (stored == 1) CameraAspect.SIXTEEN_NINE else CameraAspect.FOUR_THREE
}

private fun saveCameraAspect(context: Context, aspect: CameraAspect) {
    context.getSharedPreferences(CAMERA_PREFS_NAME, Context.MODE_PRIVATE)
        .edit {
	        putInt(CAMERA_PREFS_ASPECT, if (aspect == CameraAspect.SIXTEEN_NINE) 1 else 0)
        }
}

/**
 * iPhone-style ruler zoom control, in the look of the library's
 * RulerValuePicker: a horizontal ruler strip with ticks and zoom
 * labels that slides under a fixed centre indicator. It is not a
 * position slider: the ruler itself moves with the finger, exactly
 * like the iPhone camera zoom dial, so swiping left moves higher
 * zoom values into the centre (zoom in) and swiping right moves
 * lower values in (zoom out). Tapping a visible stop jumps straight
 * to it. Zoom maps to the ruler on a logarithmic scale, so the ruler
 * moves evenly in perceived zoom rather than bunching up at the low
 * end. Pinch-to-zoom writes the same zoomRatio state, so dragging
 * the ruler and pinching always agree with each other.
 */
@Composable
private fun ZoomRuler(
	zoomRatio: Float,
	zoomMin: Float,
	zoomMax: Float,
	onZoomChange: (Float) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    // The gesture blocks below are keyed only on the zoom range, so
    // they are not restarted when zoom changes mid-drag. Reading the
    // zoom through this updated-state reference (rather than the
    // captured zoomRatio) is what makes a drag accumulate instead of
    // snapping back to the zoom at gesture-block creation, and what
    // makes a tap after a pinch use the current zoom.
    val latestZoomRatio by rememberUpdatedState(zoomRatio)

    fun tForRatio(ratio: Float): Float {
        if (zoomMax <= zoomMin) return 0f
        return (ln((ratio / zoomMin).toDouble()) / ln((zoomMax / zoomMin).toDouble()))
            .toFloat().coerceIn(0f, 1f)
    }

    fun ratioForT(t: Float): Float {
        if (zoomMax <= zoomMin) return zoomMin
        return (zoomMin * (zoomMax / zoomMin).toDouble().pow(t.toDouble())).toFloat()
            .coerceIn(zoomMin, zoomMax)
    }

    val t = tForRatio(zoomRatio)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomMin, zoomMax) {
                    detectTapGestures { offset ->
                        // Bring the tapped ruler position to the centre.
                        val spanPx = size.width * 1.5f
                        val tappedT = tForRatio(latestZoomRatio) +
                            (offset.x - size.width / 2f) / spanPx
                        onZoomChange(ratioForT(tappedT.coerceIn(0f, 1f)))
                    }
                }
                .pointerInput(zoomMin, zoomMax) {
                    detectDragGestures { change, dragAmount ->
                        // Finger left (negative dx) moves the ruler left,
                        // so a higher zoom value arrives at the centre:
                        // zoom in. Finger right zooms out. Each event
                        // is a delta, so it is applied to the latest
                        // zoom, not to a captured one.
                        val spanPx = size.width * 1.5f
                        val newT = tForRatio(latestZoomRatio) - dragAmount.x / spanPx
                        onZoomChange(ratioForT(newT.coerceIn(0f, 1f)))
                        change.consume()
                    }
                }
        ) {
            val cx = size.width / 2f
            val spanPx = size.width * 1.5f
            fun xForRatio(ratio: Float): Float = cx + (tForRatio(ratio) - t) * spanPx
            // Minor ticks: 20 even steps on the log scale.
            for (i in 0..20) {
                val ratio = ratioForT(i / 20f)
                val x = xForRatio(ratio)
                if (x < 0f || x > size.width) continue
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(x, 8.dp.toPx()),
                    end = Offset(x, 16.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            // Major ticks and labels at the zoom stops that exist on
            // this device.
            for (stop in listOf(1f, 2f, 3f, 5f, zoomMax).distinct()) {
                if (stop !in zoomMin..zoomMax) continue
                val x = xForRatio(stop)
                if (x < 0f || x > size.width) continue
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(x, 8.dp.toPx()),
                    end = Offset(x, 26.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                val label = if (stop == zoomMax && stop != stop.toInt().toFloat()) {
                    String.format(Locale.US, "%.1f×", stop)
                } else {
                    "${stop.toInt()}×"
                }
                val measured = textMeasurer.measure(
                    text = label,
                    style = TextStyle(color = Color.White, fontSize = 11.sp)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(
                        (x - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                        32.dp.toPx()
                    ),
                    style = TextStyle(color = Color.White, fontSize = 11.sp)
                )
            }
            // Fixed centre indicator: the value under it is the zoom.
            drawLine(
                color = primary,
                start = Offset(cx, 4.dp.toPx()),
                end = Offset(cx, size.height - 4.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * Decode a downscaled bitmap for the review screen only. The full-resolution
 * file is what gets returned to the manager; the review bitmap must never
 * be used as the saved photo, to avoid holding a full-resolution bitmap in
 * composition memory.
 *
 * The decoded bitmap is rotated by the file's EXIF orientation, the same
 * correction the manager's pipeline applies before returning the photo.
 * Without this, the review shows the file's raw sensor orientation --
 * 90 degrees anti-clockwise on a typical portrait capture -- while the
 * photo returned to AdvancedImageView is upright, so the confirmation
 * screen disagrees with both the preview and the final photo. Only
 * rotation is applied, never a flip: the front camera's saved file is
 * not mirrored, and the review must match the returned photo, not the
 * mirrored preview.
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
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
        val degrees = when (ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        rotated
    } catch (_: Exception) {
        null
    }
}
