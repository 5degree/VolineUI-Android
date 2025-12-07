@file:Suppress("unused")

package com.cropintellix.volineui

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * FullScreenImageViewer - A full-screen image viewer with zoom and pan capabilities
 * 
 * Features:
 * - Pinch-to-zoom gesture support
 * - Double-tap to zoom in/out (toggle between 1x and 2x)
 * - Smooth zoom animations
 * - Pan/drag when zoomed beyond 1x
 * - Momentum scrolling with velocity
 * - Boundary constraints (image snaps back if over-panned)
 * - Close button (X) on top-right
 * - Image counter for multi-image mode (1/10)
 * - Configurable min/max zoom levels (0.5x - 5x default)
 * 
 * Usage:
 * ```kotlin
 * // Show from file
 * FullScreenImageViewer.show(context, imageFile)
 * 
 * // Show from bitmap
 * FullScreenImageViewer.show(context, bitmap)
 * 
 * // Show from URL
 * FullScreenImageViewer.show(context, imageUrl)
 * 
 * // Show with options
 * FullScreenImageViewer.Builder(context)
 *     .setImageFile(file)
 *     .setMinZoom(0.5f)
 *     .setMaxZoom(10f)
 *     .setShowCloseButton(true)
 *     .show()
 * ```
 */
class FullScreenImageViewer private constructor(
    context: Context,
    private val options: ViewerOptions
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    data class ViewerOptions(
        val imageFile: File? = null,
        val imageBitmap: Bitmap? = null,
        val imageUri: Uri? = null,
        val imageUrl: String? = null,
        val imageDrawableRes: Int = 0,
        val minZoom: Float = 0.5f,
        val maxZoom: Float = 5f,
        val showCloseButton: Boolean = true,
        val imageIndex: Int = 0,
        val totalImages: Int = 1,
        val backgroundColor: Int = Color.BLACK,
        val onDismissListener: (() -> Unit)? = null
    )

    // Views
    private lateinit var rootLayout: FrameLayout
    private lateinit var zoomableImageView: ZoomableImageView
    private lateinit var closeButton: ImageView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var counterText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Remove title and set fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(options.backgroundColor))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

        setupViews()
        loadImage()
    }

    private fun setupViews() {
        rootLayout = FrameLayout(context).apply {
            setBackgroundColor(options.backgroundColor)
        }
        setContentView(rootLayout, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Zoomable image view
        zoomableImageView = ZoomableImageView(context).apply {
            minZoom = options.minZoom
            maxZoom = options.maxZoom
            setOnSingleTapListener { toggleUI() }
        }
        rootLayout.addView(zoomableImageView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Loading indicator
        loadingIndicator = ProgressBar(context).apply {
            isIndeterminate = true
        }
        val loadingParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        rootLayout.addView(loadingIndicator, loadingParams)

        // Close button
        if (options.showCloseButton) {
            closeButton = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.WHITE)
                setPadding(dpToPx(16f), dpToPx(16f), dpToPx(16f), dpToPx(16f))
                setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
                setOnClickListener { dismiss() }
                alpha = 0.9f
            }
            val closeParams = FrameLayout.LayoutParams(
                dpToPx(56f),
                dpToPx(56f),
                Gravity.TOP or Gravity.END
            )
            closeParams.topMargin = dpToPx(24f)
            closeParams.marginEnd = dpToPx(16f)
            rootLayout.addView(closeButton, closeParams)
        }

        // Image counter (for multi-image mode)
        if (options.totalImages > 1) {
            counterText = TextView(context).apply {
                text = "${options.imageIndex + 1}/${options.totalImages}"
                setTextColor(Color.WHITE)
                textSize = 16f
                setPadding(dpToPx(12f), dpToPx(8f), dpToPx(12f), dpToPx(8f))
                setBackgroundColor(0x80000000.toInt())
            }
            val counterParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
            counterParams.topMargin = dpToPx(24f)
            rootLayout.addView(counterText, counterParams)
        }
    }

    private fun loadImage() {
        loadingIndicator.visibility = View.VISIBLE

        val glideRequest = when {
            options.imageFile != null -> Glide.with(context).load(options.imageFile)
            options.imageBitmap != null -> Glide.with(context).load(options.imageBitmap)
            options.imageUri != null -> Glide.with(context).load(options.imageUri)
            options.imageUrl != null -> Glide.with(context).load(options.imageUrl)
            options.imageDrawableRes != 0 -> Glide.with(context).load(options.imageDrawableRes)
            else -> {
                loadingIndicator.visibility = View.GONE
                return
            }
        }

        glideRequest
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    loadingIndicator.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    loadingIndicator.visibility = View.GONE
                    return false
                }
            })
            .into(zoomableImageView)
    }

    private var uiVisible = true

    private fun toggleUI() {
        uiVisible = !uiVisible
        val alpha = if (uiVisible) 1f else 0f
        
        if (options.showCloseButton) {
            closeButton.animate().alpha(if (uiVisible) 0.9f else 0f).setDuration(200).start()
        }
        if (options.totalImages > 1 && ::counterText.isInitialized) {
            counterText.animate().alpha(alpha).setDuration(200).start()
        }
    }

    override fun dismiss() {
        options.onDismissListener?.invoke()
        super.dismiss()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * ZoomableImageView - An ImageView with pinch-to-zoom and pan support
     */
    inner class ZoomableImageView(context: Context) : androidx.appcompat.widget.AppCompatImageView(context) {

        var minZoom = 0.5f
        var maxZoom = 5f

        private var onSingleTapListener: (() -> Unit)? = null

        private val matrix = Matrix()
        private val savedMatrix = Matrix()
        private val matrixValues = FloatArray(9)

        // Touch mode constants
        private val TOUCH_NONE = 0
        private val TOUCH_DRAG = 1
        private val TOUCH_ZOOM = 2

        // Touch handling
        private var mode = TOUCH_NONE
        private val start = android.graphics.PointF()
        private val mid = android.graphics.PointF()
        private var oldDist = 1f

        // Gesture detectors
        private val scaleGestureDetector: ScaleGestureDetector
        private val gestureDetector: GestureDetector

        // Boundary calculation
        private val displayRect = RectF()
        private val viewRect = RectF()

        // Animation
        private var zoomAnimator: ValueAnimator? = null

        init {
            scaleType = ScaleType.MATRIX
            
            scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    val focusX = detector.focusX
                    val focusY = detector.focusY
                    
                    val currentScale = getCurrentScale()
                    val newScale = currentScale * scaleFactor
                    
                    if (newScale in minZoom..maxZoom) {
                        matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                        constrainMatrix()
                        imageMatrix = matrix
                    }
                    
                    return true
                }
            })

            gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val currentScale = getCurrentScale()
                    val targetScale = if (currentScale < 1.5f) 2f else 1f
                    animateZoom(currentScale, targetScale, e.x, e.y)
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onSingleTapListener?.invoke()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    // Apply momentum scrolling
                    if (getCurrentScale() > 1f) {
                        animateFling(velocityX, velocityY)
                    }
                    return true
                }
            })
        }

        fun setOnSingleTapListener(listener: () -> Unit) {
            onSingleTapListener = listener
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    start.set(event.x, event.y)
                    mode = TOUCH_DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = spacing(event)
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        midPoint(mid, event)
                        mode = TOUCH_ZOOM
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == TOUCH_DRAG && getCurrentScale() > 1f) {
                        matrix.set(savedMatrix)
                        val dx = event.x - start.x
                        val dy = event.y - start.y
                        matrix.postTranslate(dx, dy)
                        constrainMatrix()
                        imageMatrix = matrix
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = TOUCH_NONE
                    constrainMatrixAnimated()
                }
            }

            return true
        }

        private fun getCurrentScale(): Float {
            matrix.getValues(matrixValues)
            return matrixValues[Matrix.MSCALE_X]
        }

        private fun spacing(event: MotionEvent): Float {
            if (event.pointerCount < 2) return 0f
            val x = event.getX(0) - event.getX(1)
            val y = event.getY(0) - event.getY(1)
            return kotlin.math.sqrt(x * x + y * y)
        }

        private fun midPoint(point: android.graphics.PointF, event: MotionEvent) {
            if (event.pointerCount < 2) return
            point.x = (event.getX(0) + event.getX(1)) / 2
            point.y = (event.getY(0) + event.getY(1)) / 2
        }

        private fun constrainMatrix() {
            val drawable = drawable ?: return
            
            val drawableWidth = drawable.intrinsicWidth.toFloat()
            val drawableHeight = drawable.intrinsicHeight.toFloat()
            
            displayRect.set(0f, 0f, drawableWidth, drawableHeight)
            matrix.mapRect(displayRect)
            
            viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
            
            var dx = 0f
            var dy = 0f
            
            // Constrain horizontal
            if (displayRect.width() <= viewRect.width()) {
                dx = (viewRect.width() - displayRect.width()) / 2 - displayRect.left
            } else {
                if (displayRect.left > 0) dx = -displayRect.left
                else if (displayRect.right < viewRect.width()) dx = viewRect.width() - displayRect.right
            }
            
            // Constrain vertical
            if (displayRect.height() <= viewRect.height()) {
                dy = (viewRect.height() - displayRect.height()) / 2 - displayRect.top
            } else {
                if (displayRect.top > 0) dy = -displayRect.top
                else if (displayRect.bottom < viewRect.height()) dy = viewRect.height() - displayRect.bottom
            }
            
            matrix.postTranslate(dx, dy)
        }

        private fun constrainMatrixAnimated() {
            val drawable = drawable ?: return
            
            val currentScale = getCurrentScale()
            if (currentScale < 1f) {
                // Animate back to scale 1
                matrix.getValues(matrixValues)
                val centerX = width / 2f
                val centerY = height / 2f
                animateZoom(currentScale, 1f, centerX, centerY)
            }
        }

        private fun animateZoom(fromScale: Float, toScale: Float, focusX: Float, focusY: Float) {
            zoomAnimator?.cancel()
            
            val startMatrix = Matrix(matrix)
            
            zoomAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    val scale = fromScale + (toScale - fromScale) * progress
                    val scaleFactor = scale / getCurrentScale()
                    
                    matrix.set(startMatrix)
                    val scaleFromStart = scale / fromScale
                    matrix.postScale(scaleFromStart, scaleFromStart, focusX, focusY)
                    constrainMatrix()
                    imageMatrix = matrix
                }
                start()
            }
        }

        private fun animateFling(velocityX: Float, velocityY: Float) {
            val startMatrix = Matrix(matrix)
            val maxTranslate = 500f
            
            val dx = (velocityX / 10f).coerceIn(-maxTranslate, maxTranslate)
            val dy = (velocityY / 10f).coerceIn(-maxTranslate, maxTranslate)
            
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    matrix.postTranslate(dx * progress * 0.1f, dy * progress * 0.1f)
                    constrainMatrix()
                    imageMatrix = matrix
                }
                start()
            }
        }

        override fun setImageDrawable(drawable: Drawable?) {
            super.setImageDrawable(drawable)
            resetZoom()
        }

        private fun resetZoom() {
            val drawable = drawable ?: return
            
            post {
                val drawableWidth = drawable.intrinsicWidth.toFloat()
                val drawableHeight = drawable.intrinsicHeight.toFloat()
                
                val scaleX = width.toFloat() / drawableWidth
                val scaleY = height.toFloat() / drawableHeight
                val scale = min(scaleX, scaleY)
                
                matrix.reset()
                matrix.postScale(scale, scale)
                
                val scaledWidth = drawableWidth * scale
                val scaledHeight = drawableHeight * scale
                val dx = (width - scaledWidth) / 2
                val dy = (height - scaledHeight) / 2
                
                matrix.postTranslate(dx, dy)
                imageMatrix = matrix
            }
        }
    }

    /**
     * Builder for creating FullScreenImageViewer with custom options
     */
    class Builder(private val context: Context) {
        private var options = ViewerOptions()

        fun setImageFile(file: File) = apply { options = options.copy(imageFile = file) }
        fun setImageBitmap(bitmap: Bitmap) = apply { options = options.copy(imageBitmap = bitmap) }
        fun setImageUri(uri: Uri) = apply { options = options.copy(imageUri = uri) }
        fun setImageUrl(url: String) = apply { options = options.copy(imageUrl = url) }
        fun setImageDrawableRes(resId: Int) = apply { options = options.copy(imageDrawableRes = resId) }
        fun setMinZoom(minZoom: Float) = apply { options = options.copy(minZoom = minZoom) }
        fun setMaxZoom(maxZoom: Float) = apply { options = options.copy(maxZoom = maxZoom) }
        fun setShowCloseButton(show: Boolean) = apply { options = options.copy(showCloseButton = show) }
        fun setImageIndex(index: Int, total: Int) = apply { 
            options = options.copy(imageIndex = index, totalImages = total) 
        }
        fun setBackgroundColor(color: Int) = apply { options = options.copy(backgroundColor = color) }
        fun setOnDismissListener(listener: () -> Unit) = apply { 
            options = options.copy(onDismissListener = listener) 
        }

        fun build() = FullScreenImageViewer(context, options)
        fun show() = build().also { it.show() }
    }

    companion object {
        /**
         * Show fullscreen viewer for a file
         */
        fun show(context: Context, file: File) {
            Builder(context).setImageFile(file).show()
        }

        /**
         * Show fullscreen viewer for a bitmap
         */
        fun show(context: Context, bitmap: Bitmap) {
            Builder(context).setImageBitmap(bitmap).show()
        }

        /**
         * Show fullscreen viewer for a URL
         */
        fun show(context: Context, url: String) {
            Builder(context).setImageUrl(url).show()
        }

        /**
         * Show fullscreen viewer for a URI
         */
        fun show(context: Context, uri: Uri) {
            Builder(context).setImageUri(uri).show()
        }
    }
}
