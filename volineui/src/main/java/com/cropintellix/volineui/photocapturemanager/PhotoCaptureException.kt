@file:Suppress("unused")

package com.cropintellix.volineui.photocapturemanager

/**
 * PhotoCaptureException - Custom exception for photo capture-related errors
 * 
 * Thrown when there's an error in photo capture, processing, or compression.
 */
class PhotoCaptureException(message: String) : Exception(message) {
    
    companion object {
        /**
         * Error: PhotoCaptureManager.init() was not called
         */
        const val ERROR_NOT_INITIALIZED = 
            "PhotoCaptureManager not initialized. Call PhotoCaptureManager.init() in your Application class."
        
        /**
         * Error: Activity is destroyed or not available
         */
        const val ERROR_ACTIVITY_DESTROYED = 
            "Cannot capture photo - no active activity available. Make sure an activity is in the foreground."
        
        /**
         * Error: Permission denied
         */
        const val ERROR_PERMISSION_DENIED = 
            "Camera or storage permission denied. Please grant permissions to capture photos."
        
        /**
         * Error: Camera not available
         */
        const val ERROR_CAMERA_UNAVAILABLE = 
            "Camera is not available on this device."
        
        /**
         * Error: Image processing failed
         */
        const val ERROR_IMAGE_PROCESSING_FAILED = 
            "Failed to process captured image. The image may be corrupted."
        
        /**
         * Error: Compression failed
         */
        const val ERROR_COMPRESSION_FAILED = 
            "Failed to compress image to target size. Try increasing targetFileSizeKB."
        
        /**
         * Error: File creation failed
         */
        const val ERROR_FILE_CREATION_FAILED = 
            "Failed to create image file. Check storage permissions and available space."
    }
}
