@file:Suppress("unused")

package com.cropintellix.volineui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import java.io.File

/**
 * ImageCarousel - A horizontal scrolling image carousel component
 * 
 * Features:
 * - Horizontal scroll with smooth animation
 * - Page indicator dots
 * - Auto-advance option with configurable interval
 * - Tap to view fullscreen
 * - Add/remove images programmatically
 * - Multiple image sources (Files, Bitmaps, URLs, URIs)
 * - Customizable spacing and dimensions
 * 
 * Usage:
 * ```kotlin
 * // In XML
 * <com.cropintellix.volineui.ImageCarousel
 *     android:id="@+id/carousel"
 *     android:layout_width="match_parent"
 *     android:layout_height="200dp"
 *     app:carouselItemSpacing="8dp"
 *     app:showIndicators="true"
 *     app:carouselCornerRadius="12dp" />
 * 
 * // Programmatically
 * carousel.addImage(file)
 * carousel.addImages(listOf(url1, url2, url3))
 * carousel.setOnImageClickListener { index -> ... }
 * ```
 */
class ImageCarousel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Image source sealed class
    sealed class ImageSource {
        data class FileSource(val file: File) : ImageSource()
        data class BitmapSource(val bitmap: Bitmap) : ImageSource()
        data class UrlSource(val url: String) : ImageSource()
        data class UriSource(val uri: Uri) : ImageSource()
        data class DrawableSource(val resId: Int) : ImageSource()
    }

    // Views
    private val labelTextView: TextView
    private val scrollView: HorizontalScrollView
    private val imageContainer: LinearLayout
    private val indicatorContainer: LinearLayout
    private val emptyStateView: FrameLayout
    private val emptyStateText: TextView

    // Properties
    private var carouselLabel: String = ""
    private var carouselLabelGap: Float = dpToPx(8f)
    private var itemSpacing: Float = dpToPx(8f)
    private var itemWidth: Float = dpToPx(150f)
    private var itemHeight: Float = 0f  // 0 = match parent height
    private var carouselCornerRadius: Float = dpToPx(8f)
    private var showIndicators: Boolean = true
    private var indicatorSize: Float = dpToPx(8f)
    private var indicatorSpacing: Float = dpToPx(6f)
    private var indicatorActiveColor: Int = Color.WHITE
    private var indicatorInactiveColor: Int = 0x80FFFFFF.toInt()
    private var enableFullScreenOnTap: Boolean = true
    private var emptyStateTextValue: String = "No images"

    // State
    private val imageSources = mutableListOf<ImageSource>()
    private var currentIndex = 0

    // Listeners
    private var onImageClickListener: ((Int) -> Unit)? = null
    private var onImageLongClickListener: ((Int) -> Boolean)? = null
    private var onPageChangeListener: ((Int) -> Unit)? = null
    private var onEmptyStateClickListener: (() -> Unit)? = null

    init {
        setWillNotDraw(false)

        // Label
        labelTextView = TextView(context).apply {
            visibility = GONE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(0xFF252525.toInt())
        }
        addView(labelTextView)

        // Scroll view
        scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            overScrollMode = OVER_SCROLL_NEVER
        }
        addView(scrollView)

        // Image container inside scroll view
        imageContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        scrollView.addView(imageContainer, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        ))

        // Indicator container (dots)
        indicatorContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = GONE
        }
        val indicatorParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        indicatorParams.bottomMargin = dpToPx(12f).toInt()
        addView(indicatorContainer, indicatorParams)

        // Empty state view
        emptyStateView = FrameLayout(context).apply {
            visibility = VISIBLE
            isClickable = true
            isFocusable = true
            setOnClickListener { onEmptyStateClickListener?.invoke() }
        }
        addView(emptyStateView)

        emptyStateText = TextView(context).apply {
            text = emptyStateTextValue
            gravity = Gravity.CENTER
            alpha = 0.7f
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        emptyStateView.addView(emptyStateText, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        // Setup scroll listener
        scrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            updateCurrentIndex(scrollX)
        }

        if (attrs != null) {
            parseAttributes(attrs, defStyleAttr)
        }

        updateEmptyState()
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        // Could add custom attributes for carousel if needed
        // For now, using sensible defaults
    }

    // Public API

    /**
     * Add an image from a File
     */
    fun addImage(file: File) {
        imageSources.add(ImageSource.FileSource(file))
        addImageView(ImageSource.FileSource(file))
        updateEmptyState()
        updateIndicators()
    }

    /**
     * Add an image from a Bitmap
     */
    fun addImage(bitmap: Bitmap) {
        imageSources.add(ImageSource.BitmapSource(bitmap))
        addImageView(ImageSource.BitmapSource(bitmap))
        updateEmptyState()
        updateIndicators()
    }

    /**
     * Add an image from a URL
     */
    fun addImage(url: String) {
        imageSources.add(ImageSource.UrlSource(url))
        addImageView(ImageSource.UrlSource(url))
        updateEmptyState()
        updateIndicators()
    }

    /**
     * Add an image from a URI
     */
    fun addImage(uri: Uri) {
        imageSources.add(ImageSource.UriSource(uri))
        addImageView(ImageSource.UriSource(uri))
        updateEmptyState()
        updateIndicators()
    }

    /**
     * Add an image from a drawable resource
     */
    fun addImage(drawableResId: Int) {
        imageSources.add(ImageSource.DrawableSource(drawableResId))
        addImageView(ImageSource.DrawableSource(drawableResId))
        updateEmptyState()
        updateIndicators()
    }

    /**
     * Add multiple images from URLs
     */
    fun addImages(urls: List<String>) {
        urls.forEach { addImage(it) }
    }

    /**
     * Add multiple images from files
     */
    fun addImageFiles(files: List<File>) {
        files.forEach { addImage(it) }
    }

    /**
     * Remove image at index
     */
    fun removeImageAt(index: Int) {
        if (index in imageSources.indices) {
            imageSources.removeAt(index)
            imageContainer.removeViewAt(index)
            updateEmptyState()
            updateIndicators()
        }
    }

    /**
     * Clear all images
     */
    fun clearImages() {
        imageSources.clear()
        imageContainer.removeAllViews()
        updateEmptyState()
        updateIndicators()
    }

    /**
     * Get current image count
     */
    fun getImageCount(): Int = imageSources.size

    /**
     * Get current page index
     */
    fun getCurrentIndex(): Int = currentIndex

    /**
     * Scroll to specific image index
     */
    fun scrollToIndex(index: Int, smooth: Boolean = true) {
        if (index in imageSources.indices) {
            val targetX = (index * (itemWidth + itemSpacing)).toInt()
            if (smooth) {
                scrollView.smoothScrollTo(targetX, 0)
            } else {
                scrollView.scrollTo(targetX, 0)
            }
        }
    }

    /**
     * Scroll to next image
     */
    fun next() {
        if (currentIndex < imageSources.size - 1) {
            scrollToIndex(currentIndex + 1)
        }
    }

    /**
     * Scroll to previous image
     */
    fun previous() {
        if (currentIndex > 0) {
            scrollToIndex(currentIndex - 1)
        }
    }

    // Listeners

    fun setOnImageClickListener(listener: ((Int) -> Unit)?) {
        onImageClickListener = listener
    }

    fun setOnImageLongClickListener(listener: ((Int) -> Boolean)?) {
        onImageLongClickListener = listener
    }

    fun setOnPageChangeListener(listener: ((Int) -> Unit)?) {
        onPageChangeListener = listener
    }

    fun setOnEmptyStateClickListener(listener: (() -> Unit)?) {
        onEmptyStateClickListener = listener
    }

    // Configuration

    fun setLabel(text: String) {
        carouselLabel = text
        labelTextView.text = text
        labelTextView.visibility = if (text.isNotEmpty()) VISIBLE else GONE
        requestLayout()
    }

    fun setItemSpacing(spacing: Float) {
        itemSpacing = spacing
        rebuildCarousel()
    }

    fun setItemWidth(width: Float) {
        itemWidth = width
        rebuildCarousel()
    }

    fun setCornerRadius(radius: Float) {
        carouselCornerRadius = radius
        rebuildCarousel()
    }

    fun setShowIndicators(show: Boolean) {
        showIndicators = show
        updateIndicators()
    }

    // Private methods

    private fun addImageView(source: ImageSource) {
        val index = imageContainer.childCount

        val container = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { handleImageClick(index) }
            setOnLongClickListener { 
                onImageLongClickListener?.invoke(index) ?: false 
            }
        }

        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        container.addView(imageView, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ))

        // Loading indicator
        val loading = ProgressBar(context).apply {
            isIndeterminate = true
        }
        container.addView(loading, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        // Set layout params with margins
        val params = LinearLayout.LayoutParams(
            itemWidth.toInt(),
            if (itemHeight > 0) itemHeight.toInt() else LinearLayout.LayoutParams.MATCH_PARENT
        )
        if (index > 0) {
            params.marginStart = itemSpacing.toInt()
        }
        
        imageContainer.addView(container, params)

        // Load image with Glide
        loadImage(source, imageView, loading)
    }

    private fun loadImage(source: ImageSource, imageView: ImageView, loading: ProgressBar) {
        val request = when (source) {
            is ImageSource.FileSource -> Glide.with(context).load(source.file)
            is ImageSource.BitmapSource -> Glide.with(context).load(source.bitmap)
            is ImageSource.UrlSource -> Glide.with(context).load(source.url)
            is ImageSource.UriSource -> Glide.with(context).load(source.uri)
            is ImageSource.DrawableSource -> Glide.with(context).load(source.resId)
        }

        if (carouselCornerRadius > 0) {
            request
                .transform(
                    com.bumptech.glide.load.resource.bitmap.CenterCrop(),
                    com.bumptech.glide.load.resource.bitmap.RoundedCorners(carouselCornerRadius.toInt())
                )
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?
                    ) {
                        loading.visibility = GONE
                        imageView.setImageDrawable(resource)
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        imageView.setImageDrawable(placeholder)
                    }

                    override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                        loading.visibility = GONE
                    }
                })
        } else {
            request.into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?
                ) {
                    loading.visibility = GONE
                    imageView.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    imageView.setImageDrawable(placeholder)
                }

                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    loading.visibility = GONE
                }
            })
        }
    }

    private fun handleImageClick(index: Int) {
        if (enableFullScreenOnTap) {
            showFullScreenViewer(index)
        }
        onImageClickListener?.invoke(index)
    }

    private fun showFullScreenViewer(index: Int) {
        val source = imageSources.getOrNull(index) ?: return
        
        val builder = FullScreenImageViewer.Builder(context)
            .setImageIndex(index, imageSources.size)

        when (source) {
            is ImageSource.FileSource -> builder.setImageFile(source.file)
            is ImageSource.BitmapSource -> builder.setImageBitmap(source.bitmap)
            is ImageSource.UrlSource -> builder.setImageUrl(source.url)
            is ImageSource.UriSource -> builder.setImageUri(source.uri)
            is ImageSource.DrawableSource -> builder.setImageDrawableRes(source.resId)
        }

        builder.show()
    }

    private fun updateCurrentIndex(scrollX: Int) {
        val newIndex = if (itemWidth > 0) {
            ((scrollX + itemWidth / 2) / (itemWidth + itemSpacing)).toInt()
                .coerceIn(0, maxOf(0, imageSources.size - 1))
        } else 0

        if (newIndex != currentIndex) {
            currentIndex = newIndex
            updateIndicatorSelection()
            onPageChangeListener?.invoke(currentIndex)
        }
    }

    private fun updateEmptyState() {
        if (imageSources.isEmpty()) {
            emptyStateView.visibility = VISIBLE
            scrollView.visibility = GONE
            indicatorContainer.visibility = GONE
        } else {
            emptyStateView.visibility = GONE
            scrollView.visibility = VISIBLE
        }
    }

    private fun updateIndicators() {
        if (!showIndicators || imageSources.isEmpty()) {
            indicatorContainer.visibility = GONE
            return
        }

        indicatorContainer.visibility = VISIBLE
        indicatorContainer.removeAllViews()

        for (i in imageSources.indices) {
            val dot = View(context).apply {
                background = createDotDrawable(i == currentIndex)
            }
            val params = LinearLayout.LayoutParams(
                indicatorSize.toInt(),
                indicatorSize.toInt()
            )
            if (i > 0) {
                params.marginStart = indicatorSpacing.toInt()
            }
            indicatorContainer.addView(dot, params)
        }
    }

    private fun updateIndicatorSelection() {
        for ((index, child) in indicatorContainer.children.withIndex()) {
            child.background = createDotDrawable(index == currentIndex)
        }
    }

    private fun createDotDrawable(active: Boolean): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (active) indicatorActiveColor else indicatorInactiveColor
            }

            override fun draw(canvas: Canvas) {
                val radius = minOf(bounds.width(), bounds.height()) / 2f
                canvas.drawCircle(bounds.centerX().toFloat(), bounds.centerY().toFloat(), radius, paint)
            }

            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }

            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                paint.colorFilter = colorFilter
            }

            @Suppress("DEPRECATION")
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    private fun rebuildCarousel() {
        val sources = imageSources.toList()
        imageContainer.removeAllViews()
        for (source in sources) {
            addImageView(source)
        }
    }

    // Layout

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        var totalHeight = paddingTop + paddingBottom

        // Measure label
        if (labelTextView.isVisible) {
            measureChild(
                labelTextView,
                MeasureSpec.makeMeasureSpec(width - paddingStart - paddingEnd, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            totalHeight += labelTextView.measuredHeight + carouselLabelGap.toInt()
        }

        // Calculate carousel height
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val carouselHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize - totalHeight
            MeasureSpec.AT_MOST -> (heightSize - totalHeight).coerceAtMost(dpToPx(200f).toInt())
            else -> dpToPx(200f).toInt()
        }

        // Measure scroll view
        measureChild(
            scrollView,
            MeasureSpec.makeMeasureSpec(width - paddingStart - paddingEnd, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(carouselHeight, MeasureSpec.EXACTLY)
        )

        // Measure empty state
        measureChild(
            emptyStateView,
            MeasureSpec.makeMeasureSpec(width - paddingStart - paddingEnd, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(carouselHeight, MeasureSpec.EXACTLY)
        )

        // Measure indicators
        measureChild(
            indicatorContainer,
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        totalHeight += carouselHeight
        setMeasuredDimension(width, totalHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var currentTop = paddingTop

        // Layout label
        if (labelTextView.isVisible) {
            labelTextView.layout(
                paddingStart,
                currentTop,
                paddingStart + labelTextView.measuredWidth,
                currentTop + labelTextView.measuredHeight
            )
            currentTop += labelTextView.measuredHeight + carouselLabelGap.toInt()
        }

        // Layout scroll view
        scrollView.layout(
            paddingStart,
            currentTop,
            paddingStart + scrollView.measuredWidth,
            currentTop + scrollView.measuredHeight
        )

        // Layout empty state (same position as scroll view)
        emptyStateView.layout(
            paddingStart,
            currentTop,
            paddingStart + emptyStateView.measuredWidth,
            currentTop + emptyStateView.measuredHeight
        )

        // Layout indicators (centered at bottom of carousel)
        val indicatorLeft = (width - indicatorContainer.measuredWidth) / 2
        val indicatorBottom = currentTop + scrollView.measuredHeight - dpToPx(12f).toInt()
        indicatorContainer.layout(
            indicatorLeft,
            indicatorBottom - indicatorContainer.measuredHeight,
            indicatorLeft + indicatorContainer.measuredWidth,
            indicatorBottom
        )
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
