@file:Suppress("unused")

package com.cropintellix.volineui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import kotlin.math.min

/**
 * ImageTransformation - Utility class for applying transformations to bitmaps
 * 
 * Supports:
 * - Rotation (90°, 180°, 270°, or custom angle)
 * - Flip (horizontal/vertical)
 * - Rounded corners
 * - Circle crop
 * - Blur effect
 * - Brightness/Contrast adjustment
 * - Saturation control
 * 
 * Usage:
 * ```kotlin
 * val transformer = ImageTransformation(context)
 * 
 * // Chain transformations
 * val result = transformer
 *     .rotate(90f)
 *     .flipHorizontal()
 *     .adjustBrightness(0.2f)
 *     .apply(originalBitmap)
 * 
 * // Or apply individually
 * val rotated = transformer.applyRotation(bitmap, 90f)
 * val blurred = transformer.applyBlur(bitmap, 15f)
 * val rounded = transformer.applyRoundedCorners(bitmap, 24f)
 * ```
 */
class ImageTransformation(private val context: Context) {

    // Transformation queue
    private val transformations = mutableListOf<Transformation>()

    // Sealed class for transformation types
    sealed class Transformation {
        data class Rotate(val degrees: Float) : Transformation()
        data class Flip(val horizontal: Boolean, val vertical: Boolean) : Transformation()
        data class RoundedCorners(val radius: Float) : Transformation()
        data class CircleCrop(val borderWidth: Float = 0f, val borderColor: Int = 0) : Transformation()
        data class Blur(val radius: Float) : Transformation()
        data class Brightness(val value: Float) : Transformation()  // -1.0 to 1.0
        data class Contrast(val value: Float) : Transformation()    // 0.0 to 2.0
        data class Saturation(val value: Float) : Transformation()  // 0.0 to 2.0
        object Grayscale : Transformation()
        data class ColorMatrix(val matrix: FloatArray) : Transformation() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ColorMatrix
                return matrix.contentEquals(other.matrix)
            }
            override fun hashCode(): Int = matrix.contentHashCode()
        }
    }

    // Builder-style methods

    /**
     * Add rotation transformation
     * @param degrees Rotation angle (positive = clockwise)
     */
    fun rotate(degrees: Float): ImageTransformation {
        transformations.add(Transformation.Rotate(degrees))
        return this
    }

    /**
     * Add 90° clockwise rotation
     */
    fun rotate90(): ImageTransformation = rotate(90f)

    /**
     * Add 180° rotation
     */
    fun rotate180(): ImageTransformation = rotate(180f)

    /**
     * Add 270° clockwise (90° counter-clockwise) rotation
     */
    fun rotate270(): ImageTransformation = rotate(270f)

    /**
     * Add horizontal flip
     */
    fun flipHorizontal(): ImageTransformation {
        transformations.add(Transformation.Flip(horizontal = true, vertical = false))
        return this
    }

    /**
     * Add vertical flip
     */
    fun flipVertical(): ImageTransformation {
        transformations.add(Transformation.Flip(horizontal = false, vertical = true))
        return this
    }

    /**
     * Add rounded corners
     * @param radius Corner radius in pixels
     */
    fun roundedCorners(radius: Float): ImageTransformation {
        transformations.add(Transformation.RoundedCorners(radius))
        return this
    }

    /**
     * Add circle crop
     * @param borderWidth Optional border width
     * @param borderColor Optional border color
     */
    fun circleCrop(borderWidth: Float = 0f, borderColor: Int = 0): ImageTransformation {
        transformations.add(Transformation.CircleCrop(borderWidth, borderColor))
        return this
    }

    /**
     * Add blur effect
     * @param radius Blur radius (1-25)
     */
    fun blur(radius: Float): ImageTransformation {
        transformations.add(Transformation.Blur(radius.coerceIn(1f, 25f)))
        return this
    }

    /**
     * Adjust brightness
     * @param value Brightness adjustment (-1.0 to 1.0, 0 = no change)
     */
    fun adjustBrightness(value: Float): ImageTransformation {
        transformations.add(Transformation.Brightness(value.coerceIn(-1f, 1f)))
        return this
    }

    /**
     * Adjust contrast
     * @param value Contrast multiplier (0.0 to 2.0, 1.0 = no change)
     */
    fun adjustContrast(value: Float): ImageTransformation {
        transformations.add(Transformation.Contrast(value.coerceIn(0f, 2f)))
        return this
    }

    /**
     * Adjust saturation
     * @param value Saturation multiplier (0.0 = grayscale, 1.0 = no change, 2.0 = vivid)
     */
    fun adjustSaturation(value: Float): ImageTransformation {
        transformations.add(Transformation.Saturation(value.coerceIn(0f, 2f)))
        return this
    }

    /**
     * Convert to grayscale
     */
    fun grayscale(): ImageTransformation {
        transformations.add(Transformation.Grayscale)
        return this
    }

    /**
     * Apply custom color matrix
     * @param matrix 4x5 color matrix as 20-element float array
     */
    fun colorMatrix(matrix: FloatArray): ImageTransformation {
        require(matrix.size == 20) { "Color matrix must have 20 elements (4x5)" }
        transformations.add(Transformation.ColorMatrix(matrix))
        return this
    }

    /**
     * Clear all queued transformations
     */
    fun clear(): ImageTransformation {
        transformations.clear()
        return this
    }

    /**
     * Apply all queued transformations to the bitmap
     * @param source Source bitmap
     * @return Transformed bitmap (new instance)
     */
    fun apply(source: Bitmap): Bitmap {
        var result = source.copy(Bitmap.Config.ARGB_8888, true)
        
        for (transformation in transformations) {
            result = when (transformation) {
                is Transformation.Rotate -> applyRotation(result, transformation.degrees)
                is Transformation.Flip -> applyFlip(result, transformation.horizontal, transformation.vertical)
                is Transformation.RoundedCorners -> applyRoundedCorners(result, transformation.radius)
                is Transformation.CircleCrop -> applyCircleCrop(result, transformation.borderWidth, transformation.borderColor)
                is Transformation.Blur -> applyBlur(result, transformation.radius)
                is Transformation.Brightness -> applyBrightness(result, transformation.value)
                is Transformation.Contrast -> applyContrast(result, transformation.value)
                is Transformation.Saturation -> applySaturation(result, transformation.value)
                is Transformation.Grayscale -> applyGrayscale(result)
                is Transformation.ColorMatrix -> applyColorMatrix(result, transformation.matrix)
            }
        }
        
        return result
    }

    // Individual transformation methods

    /**
     * Apply rotation to bitmap
     */
    fun applyRotation(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Apply flip to bitmap
     */
    fun applyFlip(source: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.postScale(
            if (horizontal) -1f else 1f,
            if (vertical) -1f else 1f,
            source.width / 2f,
            source.height / 2f
        )
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Apply rounded corners to bitmap
     */
    fun applyRoundedCorners(source: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, source.width, source.height)
        val rectF = RectF(rect)
        
        canvas.drawRoundRect(rectF, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, rect, rect, paint)
        
        return output
    }

    /**
     * Apply circle crop to bitmap
     */
    fun applyCircleCrop(source: Bitmap, borderWidth: Float = 0f, borderColor: Int = 0): Bitmap {
        val size = min(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        
        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = size / 2f
        
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squaredBitmap, 0f, 0f, paint)
        
        // Draw border if specified
        if (borderWidth > 0 && borderColor != 0) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
                color = borderColor
            }
            canvas.drawCircle(radius, radius, radius - borderWidth / 2, borderPaint)
        }
        
        if (squaredBitmap != source) {
            squaredBitmap.recycle()
        }
        
        return output
    }

    /**
     * Apply blur effect to bitmap
     * Note: Uses RenderScript which is deprecated in API 31+
     * For newer APIs, consider using RenderEffect
     */
    @Suppress("DEPRECATION")
    fun applyBlur(source: Bitmap, radius: Float): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        
        try {
            val rs = RenderScript.create(context)
            val input = Allocation.createFromBitmap(rs, source)
            val outputAlloc = Allocation.createFromBitmap(rs, output)
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            
            script.setRadius(radius.coerceIn(1f, 25f))
            script.setInput(input)
            script.forEach(outputAlloc)
            outputAlloc.copyTo(output)
            
            input.destroy()
            outputAlloc.destroy()
            script.destroy()
            rs.destroy()
        } catch (e: Exception) {
            // Fallback: return original if RenderScript fails
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }
        
        return output
    }

    /**
     * Apply brightness adjustment
     * @param value -1.0 (dark) to 1.0 (bright), 0 = no change
     */
    fun applyBrightness(source: Bitmap, value: Float): Bitmap {
        val brightness = (value * 255).toInt()
        val colorMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightness.toFloat(),
            0f, 1f, 0f, 0f, brightness.toFloat(),
            0f, 0f, 1f, 0f, brightness.toFloat(),
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrixFilter(source, colorMatrix)
    }

    /**
     * Apply contrast adjustment
     * @param value 0.0 to 2.0, 1.0 = no change
     */
    fun applyContrast(source: Bitmap, value: Float): Bitmap {
        val scale = value
        val translate = (-.5f * scale + .5f) * 255f
        val colorMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrixFilter(source, colorMatrix)
    }

    /**
     * Apply saturation adjustment
     * @param value 0.0 = grayscale, 1.0 = no change, 2.0 = vivid
     */
    fun applySaturation(source: Bitmap, value: Float): Bitmap {
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(value)
        return applyColorMatrixFilter(source, colorMatrix)
    }

    /**
     * Convert to grayscale
     */
    fun applyGrayscale(source: Bitmap): Bitmap {
        return applySaturation(source, 0f)
    }

    /**
     * Apply custom color matrix
     */
    fun applyColorMatrix(source: Bitmap, matrix: FloatArray): Bitmap {
        return applyColorMatrixFilter(source, ColorMatrix(matrix))
    }

    private fun applyColorMatrixFilter(source: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    companion object {
        /**
         * Create a new transformer instance
         */
        fun create(context: Context) = ImageTransformation(context)
    }
}
