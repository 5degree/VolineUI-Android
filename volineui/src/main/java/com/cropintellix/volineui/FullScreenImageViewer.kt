@file:Suppress("unused")

package com.cropintellix.volineui

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File
import kotlin.math.min

/**
 * FullScreenImageViewer - Full-screen image viewer with zoom and pan
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
        val minZoom: Float = 1f,      // Min zoom = fit (no zoom out)
        val maxZoom: Float = 5f,
        val showCloseButton: Boolean = true,
        val imageIndex: Int = 0,
        val totalImages: Int = 1,
        val backgroundColor: Int = Color.BLACK,
        val onDismissListener: (() -> Unit)? = null
    )

    private lateinit var rootLayout: FrameLayout
    private lateinit var zoomableImageView: ZoomableImageView
    private lateinit var closeButton: ImageView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var counterText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(options.backgroundColor))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            @Suppress("DEPRECATION")
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
        }
        rootLayout.addView(zoomableImageView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Loading indicator
        loadingIndicator = ProgressBar(context).apply { isIndeterminate = true }
        rootLayout.addView(loadingIndicator, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        // Close button - black icon with white shadow, no bg
        if (options.showCloseButton) {
            closeButton = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(0xFF222222.toInt())  // Dark/black icon
                setPadding(dpToPx(12f), dpToPx(12f), dpToPx(12f), dpToPx(12f))
                // White shadow effect via layer
                elevation = dpToPx(4f).toFloat()
                setOnClickListener { dismiss() }
            }
            val closeParams = FrameLayout.LayoutParams(
                dpToPx(48f), dpToPx(48f),
                Gravity.TOP or Gravity.END
            )
            closeParams.topMargin = dpToPx(32f)
            closeParams.marginEnd = dpToPx(16f)
            rootLayout.addView(closeButton, closeParams)
        }

        // Image counter
        if (options.totalImages > 1) {
            counterText = TextView(context).apply {
                text = "${options.imageIndex + 1}/${options.totalImages}"
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(dpToPx(10f), dpToPx(6f), dpToPx(10f), dpToPx(6f))
                setBackgroundColor(0x66000000.toInt())
            }
            val counterParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
            counterParams.topMargin = dpToPx(32f)
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
                    e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean {
                    loadingIndicator.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable>,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    loadingIndicator.visibility = View.GONE
                    return false
                }
            })
            .into(zoomableImageView)
    }

    override fun dismiss() {
        options.onDismissListener?.invoke()
        super.dismiss()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * ZoomableImageView - ImageView with pinch-zoom, double-tap, and pan
     * Default shows image fit to screen (no zoom out below fit)
     */
    inner class ZoomableImageView(context: Context) : androidx.appcompat.widget.AppCompatImageView(context) {

        var minZoom = 1f
        var maxZoom = 5f

        private val matrix = Matrix()
        private val matrixValues = FloatArray(9)

        // Touch constants
        private val TOUCH_NONE = 0
        private val TOUCH_DRAG = 1
        private val TOUCH_ZOOM = 2

        private var mode = TOUCH_NONE
        private var lastTouchX = 0f
        private var lastTouchY = 0f
        private var baseScale = 1f  // Scale that fits image to screen

        private val displayRect = RectF()
        private val scaleGestureDetector: ScaleGestureDetector
        private val gestureDetector: GestureDetector
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

                    // Clamp to min/max (baseScale is the "fit" scale which is minZoom)
                    if (newScale >= baseScale * minZoom && newScale <= baseScale * maxZoom) {
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
                    // Toggle between fit (baseScale) and 2x
                    val targetScale = if (currentScale < baseScale * 1.5f) baseScale * 2f else baseScale
                    animateZoom(currentScale, targetScale, e.x, e.y)
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    dismiss()  // Single tap closes the viewer
                    return true
                }
            })
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    mode = TOUCH_DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    mode = TOUCH_ZOOM
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == TOUCH_DRAG && getCurrentScale() > baseScale) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        matrix.postTranslate(dx, dy)
                        constrainMatrix()
                        imageMatrix = matrix
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = TOUCH_NONE
                    snapBack()
                }
            }
            return true
        }

        private fun getCurrentScale(): Float {
            matrix.getValues(matrixValues)
            return matrixValues[Matrix.MSCALE_X]
        }

        private fun constrainMatrix() {
            val drawable = drawable ?: return
            val dw = drawable.intrinsicWidth.toFloat()
            val dh = drawable.intrinsicHeight.toFloat()

            displayRect.set(0f, 0f, dw, dh)
            matrix.mapRect(displayRect)

            var dx = 0f
            var dy = 0f

            // Center if smaller than view, otherwise constrain edges
            if (displayRect.width() <= width) {
                dx = (width - displayRect.width()) / 2 - displayRect.left
            } else {
                if (displayRect.left > 0) dx = -displayRect.left
                else if (displayRect.right < width) dx = width - displayRect.right
            }

            if (displayRect.height() <= height) {
                dy = (height - displayRect.height()) / 2 - displayRect.top
            } else {
                if (displayRect.top > 0) dy = -displayRect.top
                else if (displayRect.bottom < height) dy = height - displayRect.bottom
            }

            matrix.postTranslate(dx, dy)
        }

        private fun snapBack() {
            val currentScale = getCurrentScale()
            if (currentScale < baseScale) {
                animateZoom(currentScale, baseScale, width / 2f, height / 2f)
            }
        }

        private fun animateZoom(fromScale: Float, toScale: Float, focusX: Float, focusY: Float) {
            zoomAnimator?.cancel()
            val startMatrix = Matrix(matrix)

            zoomAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    val t = anim.animatedValue as Float
                    val scale = fromScale + (toScale - fromScale) * t
                    matrix.set(startMatrix)
                    val factor = scale / fromScale
                    matrix.postScale(factor, factor, focusX, focusY)
                    constrainMatrix()
                    imageMatrix = matrix
                }
                start()
            }
        }

        override fun setImageDrawable(drawable: Drawable?) {
            super.setImageDrawable(drawable)
            resetToFit()
        }

        private fun resetToFit() {
            val drawable = drawable ?: return
            post {
                val dw = drawable.intrinsicWidth.toFloat()
                val dh = drawable.intrinsicHeight.toFloat()

                val scaleX = width.toFloat() / dw
                val scaleY = height.toFloat() / dh
                baseScale = min(scaleX, scaleY)  // Fit scale

                matrix.reset()
                matrix.postScale(baseScale, baseScale)

                val scaledW = dw * baseScale
                val scaledH = dh * baseScale
                val dx = (width - scaledW) / 2
                val dy = (height - scaledH) / 2

                matrix.postTranslate(dx, dy)
                imageMatrix = matrix
            }
        }
    }

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
        fun show(context: Context, file: File) = Builder(context).setImageFile(file).show()
        fun show(context: Context, bitmap: Bitmap) = Builder(context).setImageBitmap(bitmap).show()
        fun show(context: Context, url: String) = Builder(context).setImageUrl(url).show()
        fun show(context: Context, uri: Uri) = Builder(context).setImageUri(uri).show()

        /**
         * Show carousel with swipe navigation between images
         */
        fun showCarousel(context: Context, sources: List<ImageCarousel.ImageSource>, startIndex: Int = 0) {
            if (sources.isEmpty()) return
            CarouselViewer(context, sources, startIndex).show()
        }
    }
}

/**
 * CarouselViewer - Full-screen viewer with horizontal swipe between images
 */
class CarouselViewer(
    context: Context,
    private val sources: List<ImageCarousel.ImageSource>,
    private val startIndex: Int
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private var currentIndex = startIndex
    private lateinit var imageView: ImageView
    private lateinit var counterText: TextView
    private lateinit var loadingIndicator: ProgressBar
    private var startX = 0f
    private val swipeThreshold = 100f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            @Suppress("DEPRECATION")
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        setupViews()
        loadImage(currentIndex)
    }

    private fun setupViews() {
        val root = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Image
        imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(imageView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Loading
        loadingIndicator = ProgressBar(context).apply { isIndeterminate = true }
        root.addView(loadingIndicator, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ))

        // Counter
        counterText = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dpToPx(12f), dpToPx(8f), dpToPx(12f), dpToPx(8f))
            setBackgroundColor(0x66000000.toInt())
        }
        val counterParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        )
        counterParams.topMargin = dpToPx(40f)
        root.addView(counterText, counterParams)

        // Close button
        val closeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(0xFF333333.toInt())
            setPadding(dpToPx(12f), dpToPx(12f), dpToPx(12f), dpToPx(12f))
            elevation = dpToPx(4f).toFloat()
            setOnClickListener { dismiss() }
        }
        val closeParams = FrameLayout.LayoutParams(dpToPx(48f), dpToPx(48f), Gravity.TOP or Gravity.END)
        closeParams.topMargin = dpToPx(32f)
        closeParams.marginEnd = dpToPx(16f)
        root.addView(closeBtn, closeParams)

        // Swipe detection
        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - startX
                    when {
                        deltaX < -swipeThreshold && currentIndex < sources.size - 1 -> {
                            currentIndex++
                            loadImage(currentIndex)
                        }
                        deltaX > swipeThreshold && currentIndex > 0 -> {
                            currentIndex--
                            loadImage(currentIndex)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        updateCounter()
    }

    private fun loadImage(index: Int) {
        loadingIndicator.visibility = View.VISIBLE
        val req = when (val source = sources[index]) {
            is ImageCarousel.ImageSource.FileSource -> Glide.with(context).load(source.file)
            is ImageCarousel.ImageSource.BitmapSource -> Glide.with(context).load(source.bitmap)
            is ImageCarousel.ImageSource.UrlSource -> Glide.with(context).load(source.url)
            is ImageCarousel.ImageSource.UriSource -> Glide.with(context).load(source.uri)
            is ImageCarousel.ImageSource.DrawableSource -> Glide.with(context).load(source.resId)
        }

        req.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, m: Any?, t: Target<Drawable>, b: Boolean): Boolean {
                loadingIndicator.visibility = View.GONE
                return false
            }
            override fun onResourceReady(r: Drawable, m: Any, t: Target<Drawable>, d: DataSource, b: Boolean): Boolean {
                loadingIndicator.visibility = View.GONE
                return false
            }
        }).into(imageView)

        updateCounter()
    }

    private fun updateCounter() {
        counterText.text = "${currentIndex + 1} / ${sources.size}"
        counterText.visibility = if (sources.size > 1) View.VISIBLE else View.GONE
    }

    private fun dpToPx(dp: Float): Int = (dp * context.resources.displayMetrics.density).toInt()
}

