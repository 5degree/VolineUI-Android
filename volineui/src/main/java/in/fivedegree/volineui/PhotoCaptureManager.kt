@file:Suppress("unused", "MissingPermission")

package `in`.fivedegree.volineui

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import `in`.fivedegree.volineui.camera.VolineCameraActivity
import `in`.fivedegree.volineui.camera.detectFaceMesh
import `in`.fivedegree.volineui.locationmanager.LocationResult
import `in`.fivedegree.volineui.photocapturemanager.CaptureGuide
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureConfig
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureException
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * PhotoCaptureManager - A comprehensive and reusable photo capture manager for Android
 *
 * Features:
 * - In-app camera capture with CameraX (VolineCameraActivity). No external
 *   camera app is launched, and there is no fallback to one: a camera
 *   failure is reported as PhotoCaptureResult.Error.
 * - Gallery photo picker
 * - Intelligent image compression (target file size)
 * - Watermark with custom text, timestamp, and GPS coordinates
 * - Cached or fresh location support
 * - Photo preview with full-screen dialog
 * - Automatic permission handling via PermissionManager
 * - Automatic activity lifecycle tracking
 * - Zero boilerplate in activities
 * 
 * Usage:
 * ```kotlin
 * // 1. Initialize once in Application class
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         PermissionManager.init(this)
 *         LocationManager.init(this)
 *         PhotoCaptureManager.init(this)
 *     }
 * }
 * 
 * // 2. Capture photo with watermark
 * val config = PhotoCaptureConfig(
 *     watermarkText = "Farm Survey",
 *     printFreshLatLng = false
 * )
 * PhotoCaptureManager.instance.capturePhoto(config) { result ->
 *     when (result) {
 *         is PhotoCaptureResult.Success -> {
 *             println("Photo: ${result.file.absolutePath}")
 *             imageView.setImageBitmap(result.bitmap)
 *         }
 *         is PhotoCaptureResult.Error -> {
 *             println("Error: ${result.message}")
 *         }
 *         is PhotoCaptureResult.Cancelled -> {
 *             println("Cancelled")
 *         }
 *     }
 * }
 * 
 * // 3. Pick from gallery
 * PhotoCaptureManager.instance.pickPhotoFromGallery(config) { result ->
 *     // Handle result
 * }
 * 
 * // 4. Preview photo
 * PhotoCaptureManager.instance.previewPhoto(context, photoFile)
 * ```
 */
class PhotoCaptureManager private constructor(
    private val application: Application
) {
    
    // Current activity reference (weak to avoid memory leaks)
    private var currentActivityRef: WeakReference<ComponentActivity>? = null
    
    // Activity result launchers
    // Launcher for the library-owned in-app camera activity. It is separate
    // from the gallery launcher and is never a TakePicture launcher: photo
    // capture must not leave the app for a system camera app.
    /**
     * One host activity's result launchers. Launchers are bound to
     * the activity that registered them, so they are kept per
     * activity, not as a single shared set: a shared set breaks the
     * moment the host is recreated -- the new instance's
     * registration overwrites the fields, and the old instance's
     * destruction then clears them, leaving the live activity with
     * no launchers at all (the "not ready" failure). With a
     * per-activity map, destroying one instance only ever removes
     * its own entry. Keys are weak so a destroyed activity's entry
     * cannot leak; destruction also removes it explicitly.
     * The scanner launcher only receives the cleaned page the
     * scanner returns; the scanner owns its own capture screens.
     */
    private class ActivityLaunchers(
        val camera: ActivityResultLauncher<Intent>,
        val scanner: ActivityResultLauncher<IntentSenderRequest>,
        val gallery: ActivityResultLauncher<String>
    )

    private val launchersByActivity = WeakHashMap<ComponentActivity, ActivityLaunchers>()

    /** The current activity and its launchers, or null if it has none. */
    private fun currentLaunchers(): Pair<ComponentActivity, ActivityLaunchers>? {
        val activity = currentActivityRef?.get() ?: return null
        return launchersByActivity[activity]?.let { activity to it }
    }

    /**
     * Transient Play-services UI activities are not hosts: the
     * document-scanner screen runs in this app's process, so the
     * lifecycle callbacks see it. It must never own launchers or
     * become the "current activity": when it did, the confirm
     * screen launched from ITS camera launcher and its result died
     * with it when it was destroyed, so the host never saw the
     * photo.
     */
    private fun isScannerUiActivity(activity: Activity): Boolean =
        activity.javaClass.name.startsWith("com.google.mlkit")

    // Callbacks for capture operations
    private var currentCallback: ((PhotoCaptureResult) -> Unit)? = null
    private var currentConfig: PhotoCaptureConfig? = null

    // Temporary file for camera capture. The in-app camera writes directly
    // to this file; no FileProvider URI for an external app is needed.
    private var tempCameraFile: File? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    init {
        // Register activity lifecycle callbacks to automatically track current activity
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // The in-app camera activity must not overwrite the host
                // activity's launchers or become the "current activity" for
                // capture purposes: the host's camera-activity launcher is
                // what receives the camera result, and it must stay intact
                // while the camera screen is on top. The same goes for
                // the document-scanner UI activity (see
                // isScannerUiActivity): it is a transient screen, not
                // a host.
                if (activity is VolineCameraActivity || isScannerUiActivity(activity)) return
                if (activity is ComponentActivity) {
                    // Diagnostic (temporary): a host recreated
                    // mid-flow would orphan the result callback, so
                    // log every host creation/destruction with its
                    // instance hash.
                    Log.d(
                        "PhotoCaptureManager",
                        "onActivityCreated ${activity::class.simpleName} instance=${activity.hashCode()}"
                    )
                    currentActivityRef = WeakReference(activity)
                    registerActivityResultLaunchers(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (activity is VolineCameraActivity || isScannerUiActivity(activity)) return
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity is VolineCameraActivity) return
                // Diagnostic (temporary): see onActivityCreated.
                Log.d(
                    "PhotoCaptureManager",
                    "onActivityDestroyed ${activity::class.simpleName} instance=${activity.hashCode()}"
                )
                // Only this instance's entry goes: another activity's
                // launchers must survive this destruction untouched.
                if (activity is ComponentActivity) {
                    launchersByActivity.remove(activity)
                }
                if (currentActivityRef?.get() == activity) {
                    currentActivityRef = null
                }
            }
        })
    }
    
    /**
     * Register activity result launchers for the current activity
     */
    private fun registerActivityResultLaunchers(activity: ComponentActivity) {
        // In-app camera launcher. The camera activity writes directly to
        // tempCameraFile and returns RESULT_OK with that path; it never
        // launches a system camera app, so there is no TakePicture path here.
        val cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val file = tempCameraFile
            val config = currentConfig ?: PhotoCaptureConfig()
            val callback = currentCallback
            // Diagnostic (temporary): the scanner flow finishes
            // RESULT_OK but the host screen stays on its initial
            // text. Log every step here to find where the result
            // is lost. The returned path is logged only: the
            // decision below is the original pre-fix behaviour
            // (temp file must exist), restored so a failure shows
            // up cleanly instead of being papered over.
            Log.d(
                "PhotoCaptureManager",
                "Camera result: resultCode=${result.resultCode}, " +
                    "callback=${if (callback == null) "null" else "present"}, " +
                    "tempCameraFile=${file?.absolutePath} (exists=${file?.exists()}), " +
                    "returnedPath=${result.data?.getStringExtra(VolineCameraActivity.EXTRA_OUTPUT_PATH)}"
            )
            when {
                result.resultCode == VolineCameraActivity.RESULT_RETAKE -> {
                    // Confirm-mode Retake: the photo came from
                    // elsewhere (today, only the document scanner),
                    // so produce a fresh one and confirm again.
                    // Anything else reaching here is treated as a
                    // cancellation; the callback/config stay alive
                    // for the relaunch, and are only cleared on a
                    // terminal result below.
                    if (config.captureGuide == CaptureGuide.DOCUMENT && callback != null) {
                        cleanupTempFiles()
                        launchDocumentScanner(config, callback)
                    } else {
                        callback?.invoke(PhotoCaptureResult.Cancelled)
                        cleanupTempFiles()
                        currentCallback = null
                        currentConfig = null
                    }
                }
                result.resultCode == Activity.RESULT_OK && file != null && file.exists() -> {
                    // Notify processing started, then reuse the existing
                    // pipeline: EXIF rotation, watermark, compression, and
                    // optional gallery save. processImage() cleans up the
                    // temporary file itself. The OCR values collected in
                    // the camera activity (empty when OCR was off) travel
                    // with the result so the host gets photo and data
                    // together; the pipeline itself is unchanged.
                    callback?.invoke(PhotoCaptureResult.Processing)
                    processImage(file, config, callback, readOcrData(result.data))
                }
                result.resultCode == VolineCameraActivity.RESULT_ERROR -> {
                    val message = result.data
                        ?.getStringExtra(VolineCameraActivity.EXTRA_ERROR_MESSAGE)
                        ?: PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE
                    callback?.invoke(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(message),
                            message
                        )
                    )
                    cleanupTempFiles()
                    currentCallback = null
                    currentConfig = null
                }
                else -> {
                    Log.w(
                        "PhotoCaptureManager",
                        "Camera result not OK: resultCode=${result.resultCode}, " +
                            "tempCameraFile=${tempCameraFile?.absolutePath} " +
                            "(exists=${tempCameraFile?.exists()})"
                    )
                    callback?.invoke(PhotoCaptureResult.Cancelled)
                    cleanupTempFiles()
                    currentCallback = null
                    currentConfig = null
                }
            }
        }

        // Document-scanner launcher. The scanner returns cleaned
        // page images; the first page becomes the photo to confirm,
        // via the same tempCameraFile mechanism as a camera capture
        // so the camera-activity result handler -- and its cleanup --
        // treats it exactly like one. A user cancellation is a
        // cancellation, not an error; anything else is an error.
        val scannerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            val config = currentConfig ?: PhotoCaptureConfig()
            val callback = currentCallback
            // Diagnostic (temporary): trace the scanner leg.
            Log.d(
                "PhotoCaptureManager",
                "Scanner result: resultCode=${result.resultCode}, " +
                    "callback=${if (callback == null) "null" else "present"}"
            )
            when {
                result.resultCode == Activity.RESULT_OK -> {
                    val scanningResult = result.data?.let {
                        GmsDocumentScanningResult.fromActivityResultIntent(it)
                    }
                    val pageUri = scanningResult?.pages?.firstOrNull()?.imageUri
                    Log.d(
                        "PhotoCaptureManager",
                        "Scanner result OK: pages=${scanningResult?.pages?.size}, pageUri=$pageUri"
                    )
                    if (pageUri == null) {
                        callback?.invoke(
                            PhotoCaptureResult.Error(
                                PhotoCaptureException(PhotoCaptureException.ERROR_IMAGE_PROCESSING_FAILED),
                                "Document scan returned no pages."
                            )
                        )
                        cleanupTempFiles()
                        currentCallback = null
                        currentConfig = null
                    } else {
                        try {
                            val tempFile = copyUriToTempFile(pageUri)
                            tempCameraFile = tempFile
                            Log.d(
                                "PhotoCaptureManager",
                                "Scanner page copied: ${tempFile.absolutePath} " +
                                    "(exists=${tempFile.exists()}, length=${tempFile.length()})"
                            )
                            // Launch the confirm screen from THIS
                            // activity's own camera launcher -- the
                            // activity whose scanner launcher received
                            // this result. Resolving "current"
                            // launchers here was the bug: the scanner
                            // UI activity had become current when it
                            // was created, so confirm launched from
                            // ITS launcher and its result died with
                            // it when it was destroyed. The callback's
                            // own activity is always the live host,
                            // even across recreation (the registry
                            // re-delivers to the new instance's
                            // launcher).
                            val launchers = launchersByActivity[activity]
                            if (launchers == null) {
                                Log.w(
                                    "PhotoCaptureManager",
                                    "Scanner result with no launchers for its own activity"
                                )
                                callback?.invoke(
                                    PhotoCaptureResult.Error(
                                        PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                                        "Camera is not ready. Please try again."
                                    )
                                )
                                cleanupTempFiles()
                                currentCallback = null
                                currentConfig = null
                            } else {
                                val intent = Intent(activity, VolineCameraActivity::class.java).apply {
                                    putExtra(VolineCameraActivity.EXTRA_CONFIRM_PATH, tempFile.absolutePath)
                                    putOcrFieldExtras(config)
                                    putCameraOptionExtras(config)
                                }
                                Log.d(
                                    "PhotoCaptureManager",
                                    "Launching confirm for ${tempFile.absolutePath} " +
                                        "from ${activity::class.simpleName} " +
                                        "instance=${activity.hashCode()}"
                                )
                                launchers.camera.launch(intent)
                            }
                        } catch (e: Exception) {
                            callback?.invoke(
                                PhotoCaptureResult.Error(
                                    PhotoCaptureException(PhotoCaptureException.ERROR_IMAGE_PROCESSING_FAILED),
                                    "Failed to read the scanned document."
                                )
                            )
                            cleanupTempFiles()
                            currentCallback = null
                            currentConfig = null
                        }
                    }
                }
                result.resultCode == Activity.RESULT_CANCELED -> {
                    callback?.invoke(PhotoCaptureResult.Cancelled)
                    cleanupTempFiles()
                    currentCallback = null
                    currentConfig = null
                }
                else -> {
                    callback?.invoke(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                            "Document scanner failed."
                        )
                    )
                    cleanupTempFiles()
                    currentCallback = null
                    currentConfig = null
                }
            }
        }

        // Gallery picker launcher
        val galleryPickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val tempFile = copyUriToTempFile(uri)
                    val config = currentConfig ?: PhotoCaptureConfig()
                    val callback = currentCallback

                    if (config.ocrFields.isNotEmpty()) {
                        // Gallery photo with OCR: open the OCR step on the
                        // picked photo instead of processing immediately.
                        // The picked file becomes tempCameraFile so the
                        // camera-activity result handler above -- and its
                        // cleanup -- treats it exactly like a capture.
                        tempCameraFile = tempFile
                        // Same own-activity launch as the scanner
                        // handler: the callback's own activity is
                        // always the live host, while a "current"
                        // activity resolved here could be a transient
                        // UI activity created in between.
                        val launchers = launchersByActivity[activity]
                        if (launchers == null) {
                            Log.w(
                                "PhotoCaptureManager",
                                "Gallery OCR with no launchers for its own activity"
                            )
                            callback?.invoke(
                                PhotoCaptureResult.Error(
                                    PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                                    "Camera is not ready. Please try again."
                                )
                            )
                            cleanupTempFiles()
                            currentCallback = null
                            currentConfig = null
                        } else {
                            val intent = Intent(activity, VolineCameraActivity::class.java).apply {
                                putExtra(VolineCameraActivity.EXTRA_INPUT_PATH, tempFile.absolutePath)
                                putOcrFieldExtras(config)
                                putCameraOptionExtras(config)
                            }
                            Log.d(
                                "PhotoCaptureManager",
                                "Launching OCR for ${tempFile.absolutePath} " +
                                    "from ${activity::class.simpleName} " +
                                    "instance=${activity.hashCode()}"
                            )
                            launchers.camera.launch(intent)
                        }
                    } else {
                        // Notify processing started
                        callback?.invoke(PhotoCaptureResult.Processing)

                        processImage(tempFile, config, callback)
                    }
                } catch (e: Exception) {
                    currentCallback?.invoke(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(PhotoCaptureException.ERROR_IMAGE_PROCESSING_FAILED),
                            "Failed to load image from gallery: ${e.message}"
                        )
                    )
                    currentCallback = null
                    currentConfig = null
                }
            } else {
                currentCallback?.invoke(PhotoCaptureResult.Cancelled)
                currentCallback = null
                currentConfig = null
            }
        }

        // This activity's entry: later activities get their own, and
        // destroying one never touches another's.
        launchersByActivity[activity] = ActivityLaunchers(
            camera = cameraLauncher,
            scanner = scannerLauncher,
            gallery = galleryPickerLauncher
        )
    }

    /**
     * Capture photo using camera
     * 
     * @param config Configuration for photo capture
     * @param callback Callback invoked with the result
     * @throws PhotoCaptureException if no activity is available
     */
    fun capturePhoto(
        config: PhotoCaptureConfig = PhotoCaptureConfig(),
        callback: (PhotoCaptureResult) -> Unit
    ) {
        // Ensure we have an active activity
        val activity = currentActivityRef?.get()
            ?: throw PhotoCaptureException(PhotoCaptureException.ERROR_ACTIVITY_DESTROYED)

        // Document captures use Google's scanner UI, which needs no
        // camera permission from this app (Play services holds its
        // own), so they skip the camera-permission gate. The location
        // gates below still apply when a watermark with location was
        // requested, and launchCapture routes to the scanner.
        if (config.captureGuide == CaptureGuide.DOCUMENT) {
            checkLocationRequirementsAndLaunchCamera(config, callback)
            return
        }

        // Step 1: Check camera permission
        if (!PermissionManager.instance.isCameraPermissionGranted) {
            PermissionManager.instance.requestCameraPermission { result ->
                if (result.isGranted) {
                    // Permission granted, now check location if needed
                    checkLocationRequirementsAndLaunchCamera(config, callback)
                } else if (result.isPermanentlyDenied) {
                    showPermissionPermanentlyDeniedDialog(activity, "Camera")
                    callback(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(PhotoCaptureException.ERROR_PERMISSION_DENIED),
                            "Camera permission is permanently denied. Please enable it in settings."
                        )
                    )
                } else {
                    callback(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(PhotoCaptureException.ERROR_PERMISSION_DENIED),
                            "Camera permission is required to take photos"
                        )
                    )
                }
            }
            return
        }
        
        // Step 2: Check location requirements if watermark with location
        checkLocationRequirementsAndLaunchCamera(config, callback)
    }
    
    /**
     * Check location requirements before launching camera
     */
    private fun checkLocationRequirementsAndLaunchCamera(
        config: PhotoCaptureConfig,
        callback: (PhotoCaptureResult) -> Unit
    ) {
        val activity = currentActivityRef?.get() ?: return
        
        // If watermark with location is requested, check location permission and services
        if (config.watermarkText != null) {
            // Re-check permission in case it was just granted
            if (!PermissionManager.instance.isLocationPermissionGranted) {
                PermissionManager.instance.requestLocationPermission { result ->
                    if (result.isGranted) {
                        // Permission granted, check location services
                        checkLocationServicesAndLaunchCamera(config, callback)
                    } else if (result.isPermanentlyDenied) {
                        showPermissionPermanentlyDeniedDialog(activity, "Location")
                        callback(
                            PhotoCaptureResult.Error(
                                PhotoCaptureException(PhotoCaptureException.ERROR_PERMISSION_DENIED),
                                "Location permission is permanently denied. Watermark will not include coordinates."
                            )
                        )
                    } else {
                        callback(
                            PhotoCaptureResult.Error(
                                PhotoCaptureException(PhotoCaptureException.ERROR_PERMISSION_DENIED),
                                "Location permission is required for watermark with coordinates"
                            )
                        )
                    }
                }
                return
            }
            
            // Permission already granted, check location services
            checkLocationServicesAndLaunchCamera(config, callback)
        } else {
            // No watermark or watermark without location
            launchCapture(config, callback)
        }
    }
    
    /**
     * Check if location services are enabled
     */
    private fun checkLocationServicesAndLaunchCamera(
        config: PhotoCaptureConfig,
        callback: (PhotoCaptureResult) -> Unit
    ) {
        val activity = currentActivityRef?.get() ?: return
        
        if (!LocationManager.instance.isLocationEnabled) {
            // Show dialog to enable location services
            AlertDialog.Builder(activity)
                .setTitle("Location Services Disabled")
                .setMessage("Location services are turned off. Please enable GPS to add location to watermark.")
                .setPositiveButton("Open Settings") { _, _ ->
                    LocationManager.instance.openLocationSettings()
                    callback(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException("Location services disabled"),
                            "Please enable location services and try again"
                        )
                    )
                }
                .setNegativeButton("Cancel") { _, _ ->
                    callback(PhotoCaptureResult.Cancelled)
                }
                .setCancelable(false)
                .show()
            return
        }
        
        launchCapture(config, callback)
    }

    /**
     * Route a capture to the right capture surface: Google's
     * document-scanner UI for DOCUMENT, this library's in-app camera
     * for everything else (including the PERSON guide, which lives
     * inside that camera screen).
     */
    private fun launchCapture(config: PhotoCaptureConfig, callback: (PhotoCaptureResult) -> Unit) {
        if (config.captureGuide == CaptureGuide.DOCUMENT) {
            launchDocumentScanner(config, callback)
        } else {
            launchInAppCamera(config, callback)
        }
    }

    /**
     * Launch Google's document-scanner UI for a DOCUMENT capture.
     * Single page, gallery import off (the host asked for a camera
     * capture), JPEG only (the PDF is never used here), FULL mode
     * for the ML cleaning the design shows (crop, filters, shadow
     * and stain removal). Play services downloads the scanner on
     * first use; a missing Play services or a failed launch is an
     * Error, never a silent fallback to an unscanned photo.
     */
    private fun launchDocumentScanner(config: PhotoCaptureConfig, callback: (PhotoCaptureResult) -> Unit) {
        // Diagnostic (temporary).
        Log.d(
            "PhotoCaptureManager",
            "launchDocumentScanner: currentActivity=" +
                "${currentActivityRef?.get()?.let { "${it::class.simpleName}#${it.hashCode()}" }}"
        )
        try {
            // Resolve from the current activity's own entry: a
            // launcher from any other instance may be dead. A
            // missing activity or entry is an Error, never a silent
            // return that leaves the caller hanging.
            val current = currentLaunchers() ?: run {
                Log.w(
                    "PhotoCaptureManager",
                    "Document scanner launch with no launchers for the current activity"
                )
                callback(
                    PhotoCaptureResult.Error(
                        PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                        "Document scanner is not ready. Please try again."
                    )
                )
                return
            }
            val activity = current.first
            currentCallback = callback
            currentConfig = config
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
            // getStartScanIntent is a Task, not a direct return: the
            // scanner may still need to download before it can hand
            // over an IntentSender, so the launch waits for it. The
            // launcher is deliberately NOT captured here: while the
            // Task is pending, the host activity can be recreated
            // (rotation during a first-use download is the classic
            // case), and a launcher captured from the old instance
            // throws IllegalStateException on launch. Resolving it
            // fresh inside onSuccess uses the current activity's
            // launcher, and the try/catch turns a still-racing
            // teardown into an Error instead of a crash.
            GmsDocumentScanning.getClient(options)
                .getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    Log.d("PhotoCaptureManager", "Scanner intent ready, resolving launchers fresh")
                    val currentNow = currentLaunchers()
                    if (currentNow == null) {
                        Log.w(
                            "PhotoCaptureManager",
                            "Scanner intent ready but no launchers for the current activity"
                        )
                        callback(
                            PhotoCaptureResult.Error(
                                PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                                "Document scanner is not ready. Please try again."
                            )
                        )
                        cleanupTempFiles()
                        currentCallback = null
                        currentConfig = null
                    } else {
                        try {
                            currentNow.second.scanner.launch(IntentSenderRequest.Builder(intentSender).build())
                        } catch (e: IllegalStateException) {
                            callback(
                                PhotoCaptureResult.Error(
                                    PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                                    "Document scanner is not ready. Please try again."
                                )
                            )
                            cleanupTempFiles()
                            currentCallback = null
                            currentConfig = null
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("PhotoCaptureManager", "Scanner intent failed", e)
                    callback(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                            "Document scanner is not available on this device."
                        )
                    )
                    cleanupTempFiles()
                    currentCallback = null
                    currentConfig = null
                }
        } catch (e: Exception) {
            callback(
                PhotoCaptureResult.Error(
                    PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                    "Document scanner is not available on this device."
                )
            )
            cleanupTempFiles()
            currentCallback = null
            currentConfig = null
        }
    }

    /**
     * Show dialog for permanently denied permissions
     */
    private fun showPermissionPermanentlyDeniedDialog(context: Context, permissionName: String) {
        AlertDialog.Builder(context)
            .setTitle("$permissionName Permission Required")
            .setMessage("$permissionName permission has been permanently denied. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                PermissionManager.instance.openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Pick photo from gallery
     * 
     * @param config Configuration for photo processing
     * @param callback Callback invoked with the result
     * @throws PhotoCaptureException if no activity is available
     */
    fun pickPhotoFromGallery(
        config: PhotoCaptureConfig = PhotoCaptureConfig(),
        callback: (PhotoCaptureResult) -> Unit
    ) {
        // Ensure we have an active activity
        val activity = currentActivityRef?.get()
            ?: throw PhotoCaptureException(PhotoCaptureException.ERROR_ACTIVITY_DESTROYED)
        
        launchGalleryPicker(config, callback)
    }
    
    /**
     * Launch the library-owned in-app camera.
     *
     * No external camera app is launched and no FileProvider URI is handed
     * to another app: the camera activity writes directly to a temporary
     * file created here, and the result comes back through the current
     * activity's camera launcher. If that launcher is not available (for
     * example, capture was requested before any host activity was created),
     * this reports an error instead of falling back to a system camera.
     */
    private fun launchInAppCamera(config: PhotoCaptureConfig, callback: (PhotoCaptureResult) -> Unit) {
        try {
            // The current activity's own camera launcher; a missing
            // activity or entry is an Error, never a silent return.
            val current = currentLaunchers() ?: run {
                Log.w(
                    "PhotoCaptureManager",
                    "Camera launch with no launchers for the current activity"
                )
                callback(
                    PhotoCaptureResult.Error(
                        PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                        "Camera is not ready. Please try again."
                    )
                )
                return
            }
            val (activity, launchers) = current

            // Create temp file for the in-app camera's output.
            tempCameraFile = createTempImageFile()
            currentCallback = callback
            currentConfig = config

            val intent = Intent(activity, VolineCameraActivity::class.java).apply {
                putExtra(
                    VolineCameraActivity.EXTRA_OUTPUT_PATH,
                    tempCameraFile!!.absolutePath
                )
                putOcrFieldExtras(config)
                putCameraOptionExtras(config)
            }
            launchers.camera.launch(intent)
        } catch (e: Exception) {
            callback(
                PhotoCaptureResult.Error(
                    PhotoCaptureException(PhotoCaptureException.ERROR_FILE_CREATION_FAILED),
                    "Failed to start camera: ${e.message}"
                )
            )
            cleanupTempFiles()
        }
    }
    
    /**
     * Launch gallery picker
     */
    private fun launchGalleryPicker(config: PhotoCaptureConfig, callback: (PhotoCaptureResult) -> Unit) {
        // Same per-activity resolution: previously a missing
        // launcher silently did nothing here.
        val launchers = currentLaunchers()?.second ?: run {
            Log.w(
                "PhotoCaptureManager",
                "Gallery launch with no launchers for the current activity"
            )
            callback(
                PhotoCaptureResult.Error(
                    PhotoCaptureException(PhotoCaptureException.ERROR_CAMERA_UNAVAILABLE),
                    "Photo picker is not ready. Please try again."
                )
            )
            return
        }
        currentCallback = callback
        currentConfig = config
        launchers.gallery.launch("image/*")
    }
    
    /**
     * Read the OCR values the camera activity returned, as a field-key
     * to value map. Absent, mismatched, or non-OCR results yield an
     * empty map: OCR data is never fabricated from a partial extra set.
     */
    private fun readOcrData(data: Intent?): Map<String, String> {
        if (data == null) return emptyMap()
        val keys = data.getStringArrayExtra(VolineCameraActivity.EXTRA_OCR_DATA_KEYS)
            ?: return emptyMap()
        val values = data.getStringArrayExtra(VolineCameraActivity.EXTRA_OCR_DATA_VALUES)
            ?: return emptyMap()
        if (keys.size != values.size) return emptyMap()
        return keys.zip(values).toMap()
    }

    /**
     * Attach a config's camera options to a camera-activity intent:
     * the host's aspect choice, front-camera permission, and capture
     * guide. Sent on every camera-activity launch path so the
     * activity never has to guess.
     */
    private fun Intent.putCameraOptionExtras(config: PhotoCaptureConfig): Intent {
        putExtra(VolineCameraActivity.EXTRA_ASPECT_RATIO, config.aspectRatio.name)
        putExtra(VolineCameraActivity.EXTRA_ALLOW_FRONT_CAMERA, config.allowFrontCamera)
        putExtra(VolineCameraActivity.EXTRA_CAPTURE_GUIDE, config.captureGuide.name)
        return this
    }

    /**
     * Attach a config's OCR fields to a camera-activity intent as
     * parallel arrays. Empty for a non-OCR capture, which the activity
     * treats as "no OCR step".
     */
    private fun Intent.putOcrFieldExtras(config: PhotoCaptureConfig): Intent {
        putExtra(
            VolineCameraActivity.EXTRA_OCR_KEYS,
            config.ocrFields.map { it.key }.toTypedArray()
        )
        putExtra(
            VolineCameraActivity.EXTRA_OCR_LABELS,
            config.ocrFields.map { it.label }.toTypedArray()
        )
        putExtra(
            VolineCameraActivity.EXTRA_OCR_REQUIRED,
            config.ocrFields.map { it.required }.toBooleanArray()
        )
        return this
    }

    /**
     * Process captured or picked image - runs async after photo capture
     */
    private fun processImage(
        sourceFile: File,
        config: PhotoCaptureConfig,
        callback: ((PhotoCaptureResult) -> Unit)?,
        ocrData: Map<String, String> = emptyMap()
    ) {
        // Run processing in background thread
        Thread {
            // Diagnostic (temporary).
            Log.d(
                "PhotoCaptureManager",
                "processImage started: ${sourceFile.absolutePath} " +
                    "(exists=${sourceFile.exists()}, length=${sourceFile.length()}), " +
                    "callback=${if (callback == null) "null" else "present"}"
            )
            try {
                val timestamp = System.currentTimeMillis()
                
                // Step 1: Decode and resize bitmap
                var bitmap = decodeAndResizeBitmap(sourceFile, config.maxImageDimension)
                    ?: throw PhotoCaptureException(PhotoCaptureException.ERROR_IMAGE_PROCESSING_FAILED)
                
                // Step 2: Fix rotation based on EXIF data
                bitmap = rotateBitmapIfNeeded(sourceFile, bitmap)
                
                // Step 3: Apply watermark if configured
                var location: LocationResult? = null
                if (config.watermarkText != null) {
                    // Fetch location synchronously in background thread
                    location = getLocationForWatermarkSync(config.printFreshLatLng)
                    bitmap = drawWatermarkOnBitmap(bitmap, config.watermarkText, timestamp, location, config.watermarkPosition)
                }
                
                // Step 4: Compress and save
                val finalFile = compressAndSaveImage(bitmap, config, timestamp)
                
                // Step 5: Optionally save to gallery
                if (config.saveToGallery) {
                    saveToGallery(bitmap, "IMG_$timestamp", config.galleryFolder)
                }

                // Step 6: Face mesh for person captures, on the
                // finished bitmap so the points line up with the
                // exact delivered bytes. A mesh failure never fails
                // the capture -- detectFaceMesh returns an empty
                // mesh instead, and the host decides what that means.
                val faceMesh = if (config.captureGuide == CaptureGuide.PERSON) {
                    detectFaceMesh(bitmap)
                } else {
                    null
                }

                val fileSizeKB = (finalFile.length() / 1024).toInt()

                // Return success result on main thread
                mainHandler.post {
                    Log.d(
                        "PhotoCaptureManager",
                        "Processing done, delivering Success: ${finalFile.name}, " +
                            "callback=${if (callback == null) "null" else "present"}"
                    )
                    callback?.invoke(
                        PhotoCaptureResult.Success(
                            file = finalFile,
                            bitmap = bitmap,
                            fileSizeKB = fileSizeKB,
                            dimensions = bitmap.width to bitmap.height,
                            location = location,
                            timestamp = timestamp,
                            hasWatermark = config.watermarkText != null,
                            ocrData = ocrData,
                            faceMesh = faceMesh
                        )
                    )
                }
                
                cleanupTempFiles()
            } catch (e: PhotoCaptureException) {
                mainHandler.post {
                    Log.w(
                        "PhotoCaptureManager",
                        "Processing failed, delivering Error: ${e.message}"
                    )
                    callback?.invoke(
                        PhotoCaptureResult.Error(e, e.message ?: "Unknown error")
                    )
                }
                cleanupTempFiles()
            } catch (e: Exception) {
                mainHandler.post {
                    Log.w(
                        "PhotoCaptureManager",
                        "Processing failed, delivering Error: ${e.message}"
                    )
                    callback?.invoke(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(PhotoCaptureException.ERROR_IMAGE_PROCESSING_FAILED),
                            "Failed to process image: ${e.message}"
                        )
                    )
                }
                cleanupTempFiles()
            }
        }.start()
    }
    
    /**
     * Get location for watermark - sync version using CountDownLatch
     * Must be called from background thread
     */
    private fun getLocationForWatermarkSync(fetchFresh: Boolean): LocationResult?  {
        // Define timeouts - use same value for both location request and latch
        val locationTimeoutMs = 30_000L  // 30 seconds for fresh location
        val latchTimeoutSeconds = (locationTimeoutMs / 1000) + 2  // Add 2s buffer
        
        return try {
            val latch = CountDownLatch(1)
            var result: LocationResult? = null
            
            // LocationManager must be called on main thread
            mainHandler.post {
                try {
                    val locationManager = LocationManager.instance
                    
                    if (fetchFresh) {
                        // Fetch fresh location
                        locationManager.getLatestLocation(timeout = locationTimeoutMs) { location ->
                            result = location
                            latch.countDown()
                        }
                    } else {
                        // Use cached location
                        locationManager.getCachedLocation { location ->
                            result = location
                            latch.countDown()
                        }
                    }
                } catch (e: Exception) {
                    // If location fetch fails, continue without location
                    latch.countDown()
                }
            }
            
            // Wait for location fetch to complete
            // For fresh location, wait the full timeout + buffer
            // For cached location, 3 seconds is enough
            val waitTimeout = if (fetchFresh) latchTimeoutSeconds else 3L
            latch.await(waitTimeout, TimeUnit.SECONDS)
            
            result
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Decode and resize bitmap if needed
     */
    private fun decodeAndResizeBitmap(file: File, maxDimension: Int): Bitmap? {
        // First decode with inJustDecodeBounds to get dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true 
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        val (height, width) = options.outHeight to options.outWidth
        
        // Calculate inSampleSize for efficient memory usage
        var inSampleSize = 1
        if (height > maxDimension || width > maxDimension) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= maxDimension && 
                   halfWidth / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
        }
        
        // Decode with inSampleSize
        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        
        // Further resize if still too large
        return if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }
    
    /**
     * Rotate bitmap based on EXIF orientation
     */
    private fun rotateBitmapIfNeeded(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            
            if (rotation != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotation)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: IOException) {
            bitmap
        }
    }
    
    /**
     * Draw watermark on bitmap
     */
    private fun drawWatermarkOnBitmap(
        bitmap: Bitmap,
        watermarkText: String,
        timestamp: Long,
        location: LocationResult?,
        position: PhotoCaptureConfig.WatermarkPosition
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = bitmap.width * 0.03f
            isAntiAlias = true
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(12f, 0f, 0f, Color.BLACK)
        }

        // Format timestamp
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestampStr = dateFormat.format(Date(timestamp))

        // Create watermark lines
        val lines = mutableListOf<String>()
        lines.addAll(watermarkText.split("\n"))
        lines.add(timestampStr)
        if (location != null) {
            lines.add("Lat: ${String.format("%.5f", location.latitude)}, Lng: ${String.format("%.5f", location.longitude)}")
        }

        // Calculate text bounds
        val lineHeight = textPaint.textSize * 1.3f
        val maxTextWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val totalHeight = lines.size * lineHeight
        val padding = 20f

        val (x, y) = when (position) {
            PhotoCaptureConfig.WatermarkPosition.TOP_LEFT ->
                padding to padding + lineHeight
            PhotoCaptureConfig.WatermarkPosition.TOP_RIGHT ->
                (bitmap.width - maxTextWidth - padding) to padding + lineHeight
            PhotoCaptureConfig.WatermarkPosition.BOTTOM_LEFT ->
                padding to (bitmap.height - totalHeight)
            PhotoCaptureConfig.WatermarkPosition.BOTTOM_RIGHT ->
                (bitmap.width - maxTextWidth - padding) to (bitmap.height - totalHeight)
            PhotoCaptureConfig.WatermarkPosition.CENTER ->
                ((bitmap.width - maxTextWidth) / 2) to ((bitmap.height - totalHeight) / 2 + lineHeight)
        }

        // Draw each line of text
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, y + (index * lineHeight), textPaint)
        }

        return mutableBitmap
    }
    
    /**
     * Compress and save image to target size
     */
    private fun compressAndSaveImage(
        bitmap: Bitmap,
        config: PhotoCaptureConfig,
        timestamp: Long
    ): File {
        val outputFile = File(
            application.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "IMG_$timestamp.jpg"
        )
        
        var quality = config.compressionQuality
        var currentSizeKB: Int
        var attempts = 0
        val maxAttempts = 10
        
        do {
            FileOutputStream(outputFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }
            
            currentSizeKB = (outputFile.length() / 1024).toInt()
            
            if (currentSizeKB <= config.targetFileSizeKB) {
                break
            }
            
            quality -= 10
            attempts++
        } while (quality > 10 && attempts < maxAttempts)
        
        return outputFile
    }
    
    /**
     * Save image to device gallery
     */
    private fun saveToGallery(bitmap: Bitmap, filename: String, folder: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$folder")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = application.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
        
        return imageUri
    }
    
    /**
     * Create temporary image file
     */
    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = application.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("TEMP_${timeStamp}_", ".jpg", storageDir)
    }
    
    /**
     * Copy URI content to temporary file
     */
    private fun copyUriToTempFile(uri: Uri): File {
        val tempFile = createTempImageFile()
        application.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }
    
    /**
     * Clean up temporary files
     */
    private fun cleanupTempFiles() {
        tempCameraFile?.delete()
        tempCameraFile = null
    }
    
    companion object {
        @Volatile
        private var INSTANCE: PhotoCaptureManager? = null
        
        /**
         * Initialize the PhotoCaptureManager
         * 
         * Call this once in your Application class's onCreate()
         * Must be called after PermissionManager.init() and LocationManager.init()
         * 
         * @param application Application instance
         */
        @JvmStatic
        fun init(application: Application) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = PhotoCaptureManager(application)
                    }
                }
            }
        }
        
        /**
         * Get the singleton instance
         * 
         * @throws PhotoCaptureException if not initialized
         */
        @JvmStatic
        val instance: PhotoCaptureManager
            get() = INSTANCE 
                ?: throw PhotoCaptureException(PhotoCaptureException.ERROR_NOT_INITIALIZED)
    }
}
