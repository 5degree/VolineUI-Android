@file:Suppress("unused")

package `in`.fivedegree.volineui.photocapturemanager

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

        /**
         * Error: The capture ended because the device was outside the
         * host's allowed area. Delivered when the user abandons a
         * geofenced capture after a failed location check (the check
         * itself only shows a dialog and lets the user retry, so the
         * host hears about it only when the capture ends this way).
         * The message is the same text the user saw in the dialog.
         */
        const val ERROR_OUTSIDE_ALLOWED_AREA =
            "You're outside the allowed area for this photo. Move inside the marked area and try again."
    }
}
