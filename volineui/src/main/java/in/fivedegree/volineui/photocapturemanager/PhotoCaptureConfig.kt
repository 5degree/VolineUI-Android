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
 * @property aspectRatio Aspect ratio the camera captures at. A locked
 *   value (the default, [AspectRatio.RATIO_4_3]) fixes the camera for
 *   the whole capture and hides the aspect-ratio pill, so the user
 *   cannot change it. [AspectRatio.NONE] leaves the pill up and the
 *   user may switch between 4:3 and 16:9 for that capture only -- the
 *   choice is deliberately not remembered anywhere.
 * @property allowFrontCamera If true, the user may switch to the front
 *   camera when the device has one; the camera still opens on the back
 *   camera. If false (the default), the switch-camera button is hidden
 *   and the capture stays on the back camera.
 * @property captureGuide Guided capture for this photo (default NONE,
 *   today's behaviour). PERSON shows a person outline over the live
 *   preview and only enables the shutter while a person is framed
 *   inside it, then returns a face mesh with the photo. DOCUMENT
 *   launches Google's document-scanner UI instead of this camera
 *   (so it needs Play services) and the cleaned page comes back into
 *   the confirmation screen. See [CaptureGuide].
 * @property allowedCaptureArea The area the photo must be taken
 *   inside, as a polygon of [GeoPoint]s (default: empty, meaning no
 *   restriction -- today's behaviour). A non-empty list switches the
 *   geofence on for camera captures: tapping the shutter fetches the
 *   current location and the photo is only taken when the location is
 *   inside the polygon; otherwise the user is told why and no photo
 *   is taken. The polygon is one ring of at least 3 points, it
 *   auto-closes, winding does not matter, and a point on the
 *   boundary counts as inside. The check is planar, which is correct
 *   for the site-sized areas this is for (tens/hundreds of metres).
 *   It does not apply to gallery picks (nothing is captured at the
 *   location) or to DOCUMENT captures (the scanner UI replaces the
 *   camera, so there is no shutter to check at).
 * @property geofenceLocationMode Which location the geofence check
 *   uses (default [GeofenceLocationMode.FRESH_WITH_CACHE_FALLBACK]).
 *   Only read when [allowedCaptureArea] is non-empty.
 * @property geofenceLocationTimeoutMs How long the geofence check
 *   waits for a fresh location, in milliseconds (default 30,000 --
 *   the same default as the library's other location fetches). Only
 *   read when [allowedCaptureArea] is non-empty.
 * @property geofenceMaxAccuracyM The worst GPS accuracy, in metres,
 *   the geofence check accepts (default 10). A fix worse than this,
 *   or with unknown accuracy, fails the check: a fix that cannot
 *   prove it is within 10 m cannot prove it is inside the area.
 *   Only read when [allowedCaptureArea] is non-empty.
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
    val ocrFields: List<OcrField> = emptyList(),
    val aspectRatio: AspectRatio = AspectRatio.RATIO_4_3,
    val allowFrontCamera: Boolean = false,
    val captureGuide: CaptureGuide = CaptureGuide.NONE,
    val allowedCaptureArea: List<GeoPoint> = emptyList(),
    val geofenceLocationMode: GeofenceLocationMode = GeofenceLocationMode.FRESH_WITH_CACHE_FALLBACK,
    val geofenceLocationTimeoutMs: Long = 30_000,
    val geofenceMaxAccuracyM: Float = 10f
) {
    /**
     * Aspect ratio the host wants the camera locked to. CameraX only
     * offers 4:3 and 16:9 as target ratios, so those are the only locked
     * values; 1:1 is deliberately not offered. NONE means "no lock":
     * the user picks 4:3 or 16:9 from the pill during the capture.
     */
    enum class AspectRatio {
        RATIO_4_3,
        RATIO_16_9,
        NONE
    }

    /**
     * Which location the geofence check uses. The host picks one per
     * capture; the default matches what the library already does for
     * its other location fetches (fresh first, cached on timeout).
     */
    enum class GeofenceLocationMode {
        /** A fresh fix only. A cached fix never counts, even on timeout. */
        FRESH_ONLY,

        /**
         * A fresh fix first; if none arrives within
         * [geofenceLocationTimeoutMs], the last-known fix is used.
         */
        FRESH_WITH_CACHE_FALLBACK,

        /** Only the last-known fix; no fresh request is made. */
        CACHE_ONLY
    }

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
        // The geofence fields are only read when an area is given, but
        // they are validated always, so a bad value fails fast at
        // config construction instead of at shutter time on device.
        require(geofenceLocationTimeoutMs > 0) {
            "geofenceLocationTimeoutMs must be greater than 0"
        }
        require(geofenceMaxAccuracyM > 0) {
            "geofenceMaxAccuracyM must be greater than 0"
        }
        if (allowedCaptureArea.isNotEmpty()) {
            require(allowedCaptureArea.size >= 3) {
                "allowedCaptureArea must have at least 3 points"
            }
            require(allowedCaptureArea.all { it.latitude in -90.0..90.0 }) {
                "allowedCaptureArea latitudes must be between -90 and 90"
            }
            require(allowedCaptureArea.all { it.longitude in -180.0..180.0 }) {
                "allowedCaptureArea longitudes must be between -180 and 180"
            }
            // A degenerate ring (collinear or duplicate points) encloses
            // nothing, so every location would read as outside. Reject
            // it here rather than silently blocking every capture.
            require(allowedCaptureArea.polygonAreaDegrees() > 0.0) {
                "allowedCaptureArea must enclose a non-zero area"
            }
        }
    }
}

/**
 * Shoelace area of a ring, in square degrees; zero for a degenerate
 * ring (collinear or duplicate points). The sign depends on winding,
 * so the absolute value is what matters.
 */
private fun List<GeoPoint>.polygonAreaDegrees(): Double {
    var sum = 0.0
    for (i in indices) {
        val a = this[i]
        val b = this[(i + 1) % size]
        sum += a.longitude * b.latitude - b.longitude * a.latitude
    }
    return kotlin.math.abs(sum) / 2.0
}
