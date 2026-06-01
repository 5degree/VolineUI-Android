@file:Suppress("unused")

package `in`.fivedegree.volineui.imageview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * ImageFilter - Predefined image filter presets
 * 
 * Provides ready-to-use filters similar to photo editing apps:
 * - Grayscale, Sepia, Vintage, Warm, Cool
 * - High Contrast, Soft, Vivid, Muted
 * - Invert, Noir, Chrome, Fade
 * 
 * Usage:
 * ```kotlin
 * // Apply a preset filter
 * val filtered = ImageFilter.SEPIA.apply(originalBitmap)
 * 
 * // Or use the apply function
 * val result = ImageFilter.applyFilter(bitmap, ImageFilter.VINTAGE)
 * 
 * // Get all available filters for UI
 * val filters = ImageFilter.entries
 * filters.forEach { filter ->
 *     println("${filter.displayName}: ${filter.description}")
 * }
 * ```
 */
enum class ImageFilter(
    val displayName: String,
    val description: String,
    private val colorMatrix: FloatArray
) {
    /**
     * No filter applied - original colors
     */
    NONE(
        "Original",
        "No filter applied",
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Classic black and white
     */
    GRAYSCALE(
        "Grayscale",
        "Classic black and white",
        floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Warm brownish tone - vintage photos
     */
    SEPIA(
        "Sepia",
        "Warm brownish vintage tone",
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Retro aged photo look
     */
    VINTAGE(
        "Vintage",
        "Retro aged photo look",
        floatArrayOf(
            0.9f, 0.5f, 0.1f, 0f, -20f,
            0.3f, 0.8f, 0.1f, 0f, -10f,
            0.2f, 0.3f, 0.5f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Warm golden tones
     */
    WARM(
        "Warm",
        "Warm golden tones",
        floatArrayOf(
            1.2f, 0f, 0f, 0f, 15f,
            0f, 1.1f, 0f, 0f, 10f,
            0f, 0f, 0.9f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Cool blue tones
     */
    COOL(
        "Cool",
        "Cool blue tones",
        floatArrayOf(
            0.9f, 0f, 0f, 0f, -10f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * High contrast dramatic look
     */
    HIGH_CONTRAST(
        "High Contrast",
        "Dramatic high contrast",
        floatArrayOf(
            1.5f, 0f, 0f, 0f, -50f,
            0f, 1.5f, 0f, 0f, -50f,
            0f, 0f, 1.5f, 0f, -50f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Soft dreamy look
     */
    SOFT(
        "Soft",
        "Soft dreamy look",
        floatArrayOf(
            0.9f, 0.1f, 0.1f, 0f, 20f,
            0.1f, 0.9f, 0.1f, 0f, 20f,
            0.1f, 0.1f, 0.9f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Vivid saturated colors
     */
    VIVID(
        "Vivid",
        "Boosted saturated colors",
        floatArrayOf(
            1.3f, -0.15f, -0.15f, 0f, 10f,
            -0.15f, 1.3f, -0.15f, 0f, 10f,
            -0.15f, -0.15f, 1.3f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Muted desaturated colors
     */
    MUTED(
        "Muted",
        "Subtle desaturated tones",
        floatArrayOf(
            0.7f, 0.15f, 0.15f, 0f, 10f,
            0.15f, 0.7f, 0.15f, 0f, 10f,
            0.15f, 0.15f, 0.7f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Invert all colors
     */
    INVERT(
        "Invert",
        "Inverted negative colors",
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Film noir style - dramatic B&W
     */
    NOIR(
        "Noir",
        "Film noir dramatic style",
        floatArrayOf(
            1.5f, 0.3f, 0.3f, 0f, -70f,
            0.2f, 1.5f, 0.2f, 0f, -70f,
            0.2f, 0.2f, 1.5f, 0f, -70f,
            0f, 0f, 0f, 1f, 0f
        ).let {
            // Apply grayscale on top
            floatArrayOf(
                0.299f * 1.5f, 0.587f * 1.5f, 0.114f * 1.5f, 0f, -40f,
                0.299f * 1.5f, 0.587f * 1.5f, 0.114f * 1.5f, 0f, -40f,
                0.299f * 1.5f, 0.587f * 1.5f, 0.114f * 1.5f, 0f, -40f,
                0f, 0f, 0f, 1f, 0f
            )
        }
    ),

    /**
     * Chrome metallic sheen
     */
    CHROME(
        "Chrome",
        "Metallic chrome sheen",
        floatArrayOf(
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            1f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Faded washed out look
     */
    FADE(
        "Fade",
        "Washed out faded look",
        floatArrayOf(
            0.8f, 0.1f, 0.1f, 0f, 30f,
            0.1f, 0.8f, 0.1f, 0f, 30f,
            0.1f, 0.1f, 0.8f, 0f, 30f,
            0f, 0f, 0f, 0.9f, 0f
        )
    ),

    /**
     * Polaroid instant camera
     */
    POLAROID(
        "Polaroid",
        "Classic instant camera look",
        floatArrayOf(
            1.438f, -0.062f, -0.062f, 0f, 0f,
            -0.122f, 1.378f, -0.122f, 0f, 0f,
            -0.016f, -0.016f, 1.483f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Sunset golden hour
     */
    SUNSET(
        "Sunset",
        "Golden hour sunset tones",
        floatArrayOf(
            1.2f, 0.1f, 0f, 0f, 30f,
            0f, 1f, 0f, 0f, 20f,
            0f, 0f, 0.8f, 0f, -20f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    /**
     * Night vision green
     */
    NIGHT_VISION(
        "Night Vision",
        "Green night vision effect",
        floatArrayOf(
            0.1f, 0.4f, 0f, 0f, 0f,
            0.3f, 1f, 0.3f, 0f, 0f,
            0f, 0.4f, 0.1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    );

    /**
     * Apply this filter to a bitmap
     * @param source Source bitmap
     * @return Filtered bitmap (new instance)
     */
    fun apply(source: Bitmap): Bitmap {
        if (this == NONE) return source.copy(Bitmap.Config.ARGB_8888, true)
        
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val cm = ColorMatrix(colorMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        
        return output
    }

    /**
     * Get the color matrix for this filter
     */
    fun getColorMatrix(): ColorMatrix = ColorMatrix(colorMatrix)

    /**
     * Get the raw color matrix array
     */
    fun getColorMatrixArray(): FloatArray = colorMatrix.copyOf()

    companion object {
        /**
         * Apply a filter to a bitmap
         */
        fun applyFilter(source: Bitmap, filter: ImageFilter): Bitmap = filter.apply(source)

        /**
         * Get filter by name (case-insensitive)
         */
        fun fromName(name: String): ImageFilter? {
            return entries.find { 
                it.name.equals(name, ignoreCase = true) || 
                it.displayName.equals(name, ignoreCase = true) 
            }
        }

        /**
         * Get all filter names for display
         */
        fun getFilterNames(): List<String> = entries.map { it.displayName }

        /**
         * Create a preview of all filters applied to a source bitmap
         * Useful for filter selector UI
         */
        fun createPreviews(source: Bitmap, targetSize: Int = 100): Map<ImageFilter, Bitmap> {
            // Scale down source for preview
            val scale = targetSize.toFloat() / maxOf(source.width, source.height)
            val previewWidth = (source.width * scale).toInt()
            val previewHeight = (source.height * scale).toInt()
            val scaledSource = Bitmap.createScaledBitmap(source, previewWidth, previewHeight, true)
            
            return entries.associateWith { filter ->
                filter.apply(scaledSource)
            }
        }
    }
}
