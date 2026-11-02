@file:Suppress("unused")

package `in`.fivedegree.volineui.photocapturemanager

/**
 * PhotoCaptureConfig - Configuration for photo capture operations
 * 
 * Customize photo capture behavior including watermark, compression, and storage options.
 * 
 * @property watermarkText Optional watermark text to display on the photo
 * @property printFreshLatLng If true, fetch fresh location; if false, use cached location
 * @property targetFileSizeKB Target file size in kilobytes (default: 200KB)
 * @property maxImageDimension Maximum width/height in pixels before scaling (default: 1920)
 * @property compressionQuality Initial JPEG compression quality 0-100 (default: 90)
 * @property saveToGallery If true, save the photo to device gallery (default: true)
 * @property galleryFolder Name of the gallery folder to save photos (default: "Photos")
 * @property watermarkPosition Position of watermark on image (default: BOTTOM_LEFT)
 * @property ocrFields Data the host wants collected from a document photo
 *   (default: empty, meaning no OCR step). A non-empty list switches the
 *   capture on: after the user confirms the photo, an OCR step lets the
 *   user tap words on the photo for each field, and the collected values
 *   come back in [PhotoCaptureResult.Success.ocrData]. This also applies
 *   to gallery picks, which open the OCR step on the picked photo.
 */
data class PhotoCaptureConfig(
    val watermarkText: String? = null,
    val printFreshLatLng: Boolean = false,
    val targetFileSizeKB: Int = 200,
    val maxImageDimension: Int = 1920,
    val compressionQuality: Int = 90,
    val saveToGallery: Boolean = true,
    val galleryFolder: String = "VolineUI",
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val ocrFields: List<OcrField> = emptyList()
) {
    /**
     * Watermark position on the image
     */
    enum class WatermarkPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }
    
    /**
     * Validate configuration parameters
     */
    init {
        require(targetFileSizeKB > 0) { "targetFileSizeKB must be greater than 0" }
        require(maxImageDimension > 0) { "maxImageDimension must be greater than 0" }
        require(compressionQuality in 1..100) { "compressionQuality must be between 1 and 100" }
        // OCR field keys become the keys of Success.ocrData, so a blank
        // or duplicate key would make the result ambiguous for the host.
        require(ocrFields.all { it.key.isNotBlank() }) { "ocrFields keys must not be blank" }
        require(ocrFields.map { it.key }.toSet().size == ocrFields.size) {
            "ocrFields keys must be unique"
        }
    }
}
