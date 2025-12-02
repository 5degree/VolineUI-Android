@file:Suppress("unused", "MissingPermission")

package com.cropintellix.volineui

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Window
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PhotoCaptureManager - A comprehensive and reusable photo capture manager for Android
 * 
 * Features:
 * - Camera capture with Activity Result API
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
    private var cameraLauncher: ActivityResultLauncher<Uri>? = null
    private var galleryLauncher: ActivityResultLauncher<String>? = null
    
    // Callbacks for capture operations
    private var currentCallback: ((PhotoCaptureResult) -> Unit)? = null
    private var currentConfig: PhotoCaptureConfig? = null
    
    // Temporary file for camera capture
    private var tempCameraFile: File? = null
    private var tempCameraUri: Uri? = null
    
    init {
        // Register activity lifecycle callbacks to automatically track current activity
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                    registerActivityResultLaunchers(activity)
                }
            }
            
            override fun onActivityStarted(activity: Activity) {}
            
            override fun onActivityResumed(activity: Activity) {
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                }
            }
            
            override fun onActivityPaused(activity: Activity) {}
            
            override fun onActivityStopped(activity: Activity) {}
            
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivityRef?.get() == activity) {
                    currentActivityRef = null
                    cameraLauncher = null
                    galleryLauncher = null
                }
            }
        })
    }
    
    /**
     * Register activity result launchers for the current activity
     */
    private fun registerActivityResultLaunchers(activity: ComponentActivity) {
        // Camera launcher
        cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && tempCameraUri != null) {
                tempCameraFile?.let { file ->
                    processImage(file, currentConfig ?: PhotoCaptureConfig(), currentCallback)
                }
            } else if (!success) {
                currentCallback?.invoke(PhotoCaptureResult.Cancelled)
                cleanupTempFiles()
            }
            currentCallback = null
            currentConfig = null
        }
        
        // Gallery picker launcher
        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val tempFile = copyUriToTempFile(uri)
                    processImage(tempFile, currentConfig ?: PhotoCaptureConfig(), currentCallback)
                } catch (e: Exception) {
                    currentCallback?.invoke(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(PhotoCaptureException.ERROR_IMAGE_PROCESSING_FAILED),
                            "Failed to load image from gallery"
                        )
                    )
                }
            } else {
                currentCallback?.invoke(PhotoCaptureResult.Cancelled)
            }
            currentCallback = null
            currentConfig = null
        }
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
        
        // Check camera permission
        if (!PermissionManager.instance.isCameraPermissionGranted) {
            PermissionManager.instance.requestCameraPermission { result ->
                if (result.isGranted) {
                    launchCamera(config, callback)
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
        
        launchCamera(config, callback)
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
        
        // Check storage permission
        if (!PermissionManager.instance.isStoragePermissionGranted) {
            PermissionManager.instance.requestStoragePermission { results ->
                if (results.values.any { it.isGranted }) {
                    launchGalleryPicker(config, callback)
                } else {
                    callback(
                        PhotoCaptureResult.Error(
                            PhotoCaptureException(PhotoCaptureException.ERROR_PERMISSION_DENIED),
                            "Storage permission is required to pick photos"
                        )
                    )
                }
            }
            return
        }
        
        launchGalleryPicker(config, callback)
    }
    
    /**
     * Preview photo in full-screen dialog
     * 
     * @param context Context to show dialog
     * @param file Photo file to preview
     */
    fun previewPhoto(context: Context, file: File) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        
        // Create ImageView programmatically
        val imageView = ImageView(context).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            setOnClickListener { dialog.dismiss() }
        }
        
        dialog.setContentView(imageView)
        dialog.window?.setLayout(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }
    
    /**
     * Launch camera
     */
    private fun launchCamera(config: PhotoCaptureConfig, callback: (PhotoCaptureResult) -> Unit) {
        try {
            val activity = currentActivityRef?.get() ?: return
            
            // Create temp file for camera output
            tempCameraFile = createTempImageFile()
            tempCameraUri = FileProvider.getUriForFile(
                activity,
                "${application.packageName}.provider",
                tempCameraFile!!
            )
            
            currentCallback = callback
            currentConfig = config
            
            cameraLauncher?.launch(tempCameraUri)
        } catch (e: Exception) {
            callback(
                PhotoCaptureResult.Error(
                    PhotoCaptureException(PhotoCaptureException.ERROR_FILE_CREATION_FAILED),
                    "Failed to create camera file: ${e.message}"
                )
            )
            cleanupTempFiles()
        }
    }
    
    /**
     * Launch gallery picker
     */
    private fun launchGalleryPicker(config: PhotoCaptureConfig, callback: (PhotoCaptureResult) -> Unit) {
        currentCallback = callback
        currentConfig = config
        galleryLauncher?.launch("image/*")
    }
    
    /**
     * Process captured or picked image
     */
    private fun processImage(
        sourceFile: File,
        config: PhotoCaptureConfig,
        callback: ((PhotoCaptureResult) -> Unit)?
    ) {
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
                location = getLocationForWatermark(config.printFreshLatLng)
                bitmap = drawWatermarkOnBitmap(bitmap, config.watermarkText, timestamp, location, config.watermarkPosition)
            }
            
            // Step 4: Compress and save
            val finalFile = compressAndSaveImage(bitmap, config, timestamp)
            
            // Step 5: Optionally save to gallery
            if (config.saveToGallery) {
                saveToGallery(bitmap, "IMG_$timestamp", config.galleryFolder)
            }
            
            // Calculate file size
            val fileSizeKB = (finalFile.length() / 1024).toInt()
            
            // Return success result
            callback?.invoke(
                PhotoCaptureResult.Success(
                    file = finalFile,
                    bitmap = bitmap,
                    fileSizeKB = fileSizeKB,
                    dimensions = bitmap.width to bitmap.height,
                    location = location,
                    timestamp = timestamp,
                    hasWatermark = config.watermarkText != null
                )
            )
            
            cleanupTempFiles()
        } catch (e: PhotoCaptureException) {
            callback?.invoke(
                PhotoCaptureResult.Error(e, e.message ?: "Unknown error")
            )
            cleanupTempFiles()
        } catch (e: Exception) {
            callback?.invoke(
                PhotoCaptureResult.Error(
                    PhotoCaptureException(PhotoCaptureException.ERROR_IMAGE_PROCESSING_FAILED),
                    "Failed to process image: ${e.message}"
                )
            )
            cleanupTempFiles()
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
     * Get location for watermark
     */
    private fun getLocationForWatermark(fetchFresh: Boolean): LocationResult? {
        return try {
            var result: LocationResult? = null
            val locationManager = LocationManager.instance
            
            if (fetchFresh) {
                // Fetch fresh location (blocking call with short timeout)
                locationManager.getLatestLocation(timeout = 5000) { location ->
                    result = location
                }
                // Wait a bit for callback (simplified - in production use coroutines)
                Thread.sleep(5500)
            } else {
                // Use cached location
                locationManager.getCachedLocation { location ->
                    result = location
                }
            }
            
            result
        } catch (e: Exception) {
            null
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
        
        // Configure paint for text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = bitmap.width * 0.04f // Scale text size based on image width
            isAntiAlias = true
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        
        // Configure paint for background
        val bgPaint = Paint().apply {
            color = Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }
        
        // Format timestamp
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestampStr = dateFormat.format(Date(timestamp))
        
        // Create watermark lines
        val lines = mutableListOf<String>()
        lines.add(watermarkText)
        lines.add(timestampStr)
        if (location != null) {
            lines.add("Lat: ${String.format("%.5f", location.latitude)}, Lng: ${String.format("%.5f", location.longitude)}")
        }
        
        // Calculate text bounds
        val lineHeight = textPaint.textSize * 1.3f
        val maxTextWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val totalHeight = lines.size * lineHeight
        val padding = 20f
        
        // Calculate position
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
        
        // Draw background rectangle
        canvas.drawRect(
            x - padding,
            y - lineHeight,
            x + maxTextWidth + padding,
            y + totalHeight - lineHeight + padding,
            bgPaint
        )
        
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
        tempCameraUri = null
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
