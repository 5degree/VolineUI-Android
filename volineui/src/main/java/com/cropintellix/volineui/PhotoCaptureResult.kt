@file:Suppress("unused")

package com.cropintellix.volineui

import android.graphics.Bitmap
import java.io.File

/**
 * PhotoCaptureResult - Result of a photo capture operation
 * 
 * Sealed class representing the outcome of capturing or picking a photo.
 * Use when pattern matching to handle different result states.
 * 
 * Usage:
 * ```kotlin
 * when (result) {
 *     is PhotoCaptureResult.Success -> {
 *         println("Photo saved: ${result.file.absolutePath}")
 *         println("File size: ${result.fileSizeKB}KB")
 *     }
 *     is PhotoCaptureResult.Error -> {
 *         println("Error: ${result.message}")
 *     }
 *     is PhotoCaptureResult.Cancelled -> {
 *         println("User cancelled")
 *     }
 * }
 * ```
 */
sealed class PhotoCaptureResult {
    
    /**
     * Success - Photo captured and processed successfully
     * 
     * @property file Processed image file
     * @property bitmap Optional bitmap for immediate display (null to save memory)
     * @property fileSizeKB Actual file size in kilobytes
     * @property dimensions Image dimensions as (width, height)
     * @property location GPS coordinates if watermark was applied with location
     * @property timestamp Capture timestamp in milliseconds
     * @property hasWatermark True if watermark was applied
     */
    data class Success(
        val file: File,
        val bitmap: Bitmap? = null,
        val fileSizeKB: Int,
        val dimensions: Pair<Int, Int>,
        val location: LocationResult? = null,
        val timestamp: Long,
        val hasWatermark: Boolean
    ) : PhotoCaptureResult() {
        /**
         * Get user-friendly file size display
         */
        val fileSizeDisplay: String
            get() = if (fileSizeKB < 1024) {
                "${fileSizeKB}KB"
            } else {
                String.format("%.2fMB", fileSizeKB / 1024.0)
            }
        
        /**
         * Get dimensions as display string
         */
        val dimensionsDisplay: String
            get() = "${dimensions.first} × ${dimensions.second}"
    }
    
    /**
     * Error - Photo capture or processing failed
     * 
     * @property exception PhotoCaptureException with details
     * @property message User-friendly error message
     */
    data class Error(
        val exception: PhotoCaptureException,
        val message: String
    ) : PhotoCaptureResult()
    
    /**
     * Cancelled - User cancelled the photo capture operation
     */
    object Cancelled : PhotoCaptureResult()
    
    /**
     * Convenience property - true if result is Success
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Convenience property - true if result is Error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Convenience property - true if result is Cancelled
     */
    val isCancelled: Boolean
        get() = this is Cancelled
}
