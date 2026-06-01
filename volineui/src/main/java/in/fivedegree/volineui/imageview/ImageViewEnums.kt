@file:Suppress("unused")

package `in`.fivedegree.volineui.imageview

/**
 * Scale type options for AdvancedImageView.
 * 
 * Defines how the image should be scaled to fit within the view bounds.
 * Used by both View-based and Compose implementations.
 */
enum class ImageScaleType(val value: Int) {
    /**
     * Scale the image to fit within the bounds while maintaining aspect ratio.
     * The entire image will be visible, but there may be empty space.
     */
    FIT(0),
    
    /**
     * Scale the image to fill the entire bounds while maintaining aspect ratio.
     * The image will be cropped if necessary.
     */
    FILL(1),
    
    /**
     * Center crop - scale to fill and crop from center.
     * Same as FILL but explicitly named for clarity.
     */
    CROP(2),
    
    /**
     * Center the image without scaling if smaller than bounds.
     * Scale down to fit if larger.
     */
    CENTER(3),
    
    /**
     * Stretch the image to fill the bounds, ignoring aspect ratio.
     */
    STRETCH(4);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: CROP
    }
}

/**
 * State of the AdvancedImageView.
 * 
 * Represents the current loading/display state of the image.
 * Used by both View-based and Compose implementations.
 */
enum class ImageState {
    /**
     * No image is set - showing placeholder
     */
    EMPTY,
    
    /**
     * Image is currently being loaded
     */
    LOADING,
    
    /**
     * Image has been successfully loaded and is displayed
     */
    LOADED,
    
    /**
     * Failed to load the image
     */
    ERROR
}

/**
 * Source type for loading images in AdvancedImageView.
 * 
 * Represents different image source types that can be loaded.
 */
sealed class ImageSource {
    /**
     * Load image from a URL string
     */
    data class Url(val url: String) : ImageSource()
    
    /**
     * Load image from a file path
     */
    data class FilePath(val path: String) : ImageSource()
    
    /**
     * Load image from a java.io.File
     */
    data class File(val file: java.io.File) : ImageSource()
    
    /**
     * Load image from an Android Uri
     */
    data class Uri(val uri: android.net.Uri) : ImageSource()
    
    /**
     * Load image from a drawable resource ID
     */
    data class DrawableRes(val resId: Int) : ImageSource()
    
    /**
     * Load image from a Bitmap
     */
    data class Bitmap(val bitmap: android.graphics.Bitmap) : ImageSource()
    
    /**
     * Load image from a Base64 encoded string
     */
    data class Base64(val base64String: String) : ImageSource()
    
    /**
     * No image source
     */
    data object Empty : ImageSource()
}
