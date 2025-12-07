@file:Suppress("unused")

package com.cropintellix.volineui

/**
 * ImageViewException - Exception classes for AdvancedImageView operations
 */
sealed class ImageViewException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Failed to load image from source
     */
    class ImageLoadException(
        val source: String,
        message: String = "Failed to load image from: $source",
        cause: Throwable? = null
    ) : ImageViewException(message, cause)
    
    /**
     * Invalid image source provided
     */
    class InvalidSourceException(
        message: String = "Invalid image source",
        cause: Throwable? = null
    ) : ImageViewException(message, cause)
    
    /**
     * Unsupported image format
     */
    class UnsupportedFormatException(
        val format: String,
        message: String = "Unsupported image format: $format",
        cause: Throwable? = null
    ) : ImageViewException(message, cause)
    
    /**
     * No image currently loaded
     */
    class NoImageException(
        message: String = "No image is currently loaded",
        cause: Throwable? = null
    ) : ImageViewException(message, cause)
    
    companion object {
        fun loadFailed(source: String, cause: Throwable? = null) = 
            ImageLoadException(source, cause = cause)
        
        fun invalidSource(details: String? = null) = 
            InvalidSourceException(details ?: "Invalid image source")
        
        fun unsupportedFormat(format: String) = 
            UnsupportedFormatException(format)
        
        fun noImage() = NoImageException()
    }
}
