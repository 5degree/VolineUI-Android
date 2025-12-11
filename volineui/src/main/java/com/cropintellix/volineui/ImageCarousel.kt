@file:Suppress("unused")

package com.cropintellix.volineui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
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
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.drawable.Drawable
import java.io.File

/**
 * ImageCarousel - A horizontal scrolling image carousel with delete icons
 */
class ImageCarousel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

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

    // Properties (with defaults)
    private var carouselLabel: String = ""
    private var carouselLabelGap: Float = dpToPx(8f)
    private var itemWidth: Float = dpToPx(120f)
    private var itemHeight: Float = dpToPx(120f)
    private var itemSpacing: Float = dpToPx(8f)
    private var carouselCornerRadius: Float = dpToPx(8f)
    private var showIndicators: Boolean = true
    private var indicatorSize: Float = dpToPx(6f)
    private var indicatorSpacing: Float = dpToPx(4f)
    private var indicatorActiveColor: Int = Color.WHITE
    private var indicatorInactiveColor: Int = 0x80FFFFFF.toInt()
    private var showItemDeleteIcon: Boolean = true
    private var enableFullScreen: Boolean = true
    private var emptyStateTextValue: String = "No images"

    // State
    private val imageSources = mutableListOf<ImageSource>()
    private var currentIndex = 0

    // Listeners
    private var onImageClickListener: ((Int) -> Unit)? = null
    private var onImageDeleteListener: ((Int) -> Unit)? = null
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
            isFillViewport = false
            overScrollMode = OVER_SCROLL_NEVER
        }
        addView(scrollView)

        // Image container
        imageContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        scrollView.addView(imageContainer, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT
        ))

        // Indicators
        indicatorContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = GONE
        }
        val indParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        indParams.bottomMargin = dpToPx(8f).toInt()
        addView(indicatorContainer, indParams)

        // Empty state
        emptyStateView = FrameLayout(context).apply {
            visibility = VISIBLE
            isClickable = true
            setBackgroundColor(0xFFF5F5F5.toInt())
            setOnClickListener { onEmptyStateClickListener?.invoke() }
        }
        addView(emptyStateView)

        emptyStateText = TextView(context).apply {
            text = emptyStateTextValue
            gravity = Gravity.CENTER
            alpha = 0.5f
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(0xFF666666.toInt())
        }
        emptyStateView.addView(emptyStateText, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ))

        scrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            updateCurrentIndex(scrollX)
        }

        if (attrs != null) parseAttributes(attrs, defStyleAttr)
        updateEmptyState()
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ImageCarousel, defStyleAttr, 0)
        try {
            carouselLabel = ta.getString(R.styleable.ImageCarousel_carouselLabel) ?: ""
            if (carouselLabel.isNotEmpty()) {
                labelTextView.text = carouselLabel
                labelTextView.visibility = VISIBLE
            }
            carouselLabelGap = ta.getDimension(R.styleable.ImageCarousel_carouselLabelGap, dpToPx(8f))
            itemWidth = ta.getDimension(R.styleable.ImageCarousel_carouselItemWidth, dpToPx(120f))
            itemHeight = ta.getDimension(R.styleable.ImageCarousel_carouselItemHeight, dpToPx(120f))
            itemSpacing = ta.getDimension(R.styleable.ImageCarousel_carouselItemSpacing, dpToPx(8f))
            carouselCornerRadius = ta.getDimension(R.styleable.ImageCarousel_carouselCornerRadius, dpToPx(8f))
            showIndicators = ta.getBoolean(R.styleable.ImageCarousel_showIndicators, true)
            indicatorSize = ta.getDimension(R.styleable.ImageCarousel_indicatorSize, dpToPx(6f))
            indicatorSpacing = ta.getDimension(R.styleable.ImageCarousel_indicatorSpacing, dpToPx(4f))
            indicatorActiveColor = ta.getColor(R.styleable.ImageCarousel_indicatorActiveColor, Color.WHITE)
            indicatorInactiveColor = ta.getColor(R.styleable.ImageCarousel_indicatorInactiveColor, 0x80FFFFFF.toInt())
            showItemDeleteIcon = ta.getBoolean(R.styleable.ImageCarousel_showItemDeleteIcon, true)
            enableFullScreen = ta.getBoolean(R.styleable.ImageCarousel_enableCarouselFullScreen, true)
            emptyStateTextValue = ta.getString(R.styleable.ImageCarousel_carouselEmptyText) ?: "No images"
            emptyStateText.text = emptyStateTextValue
        } finally {
            ta.recycle()
        }
    }

    // Public API

    fun addImage(file: File) { addSource(ImageSource.FileSource(file)) }
    fun addImage(bitmap: Bitmap) { addSource(ImageSource.BitmapSource(bitmap)) }
    fun addImage(url: String) { addSource(ImageSource.UrlSource(url)) }
    fun addImage(uri: Uri) { addSource(ImageSource.UriSource(uri)) }
    fun addImage(drawableResId: Int) { addSource(ImageSource.DrawableSource(drawableResId)) }

    fun addImages(files: List<File>) { files.forEach { addImage(it) } }
    fun addImageUrls(urls: List<String>) { urls.forEach { addImage(it) } }
    fun addImageBitmaps(bitmaps: List<Bitmap>) { bitmaps.forEach { addImage(it) } }

    private fun addSource(source: ImageSource) {
        imageSources.add(source)
        addImageView(source, imageSources.size - 1)
        updateEmptyState()
        updateIndicators()
    }

    fun removeImageAt(index: Int) {
        if (index in imageSources.indices) {
            imageSources.removeAt(index)
            imageContainer.removeViewAt(index)
            // Update indices for remaining items
            rebuildIndices()
            updateEmptyState()
            updateIndicators()
            onImageDeleteListener?.invoke(index)
        }
    }

    fun clearImages() {
        imageSources.clear()
        imageContainer.removeAllViews()
        updateEmptyState()
        updateIndicators()
    }

    fun getImageCount(): Int = imageSources.size
    fun getCurrentIndex(): Int = currentIndex

    // Listeners
    fun setOnImageClickListener(listener: ((Int) -> Unit)?) { onImageClickListener = listener }
    fun setOnImageDeleteListener(listener: ((Int) -> Unit)?) { onImageDeleteListener = listener }
    fun setOnEmptyStateClickListener(listener: (() -> Unit)?) { onEmptyStateClickListener = listener }

    fun setLabel(text: String) {
        carouselLabel = text
        labelTextView.text = text
        labelTextView.visibility = if (text.isNotEmpty()) VISIBLE else GONE
        requestLayout()
    }

    private fun addImageView(source: ImageSource, index: Int) {
        val container = FrameLayout(context).apply {
            tag = index
            clipChildren = true
            clipToPadding = true
        }

        // Rounded background
        val bg = GradientDrawable().apply {
            setColor(0xFFF0F0F0.toInt())
            cornerRadius = carouselCornerRadius
        }
        container.background = bg

        // Image
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setOnClickListener {
                val idx = container.tag as Int
                if (enableFullScreen) showFullScreen(idx)
                onImageClickListener?.invoke(idx)
            }
        }
        container.addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Loading
        val loading = ProgressBar(context).apply { isIndeterminate = true }
        container.addView(loading, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ))

        // Delete button (top-right, red)
        if (showItemDeleteIcon) {
            val deleteBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(0xFFE53935.toInt())
                setPadding(dpToPx(4f).toInt(), dpToPx(4f).toInt(), dpToPx(4f).toInt(), dpToPx(4f).toInt())
                val dbg = GradientDrawable().apply {
                    setColor(0x33E53935.toInt())
                    cornerRadius = (carouselCornerRadius * 0.5f).coerceAtLeast(dpToPx(4f))
                }
                background = dbg
                setOnClickListener { removeImageAt(container.tag as Int) }
            }
            val delParams = LayoutParams(dpToPx(24f).toInt(), dpToPx(24f).toInt())
            delParams.gravity = Gravity.TOP or Gravity.END
            delParams.marginEnd = dpToPx(4f).toInt()
            delParams.topMargin = dpToPx(4f).toInt()
            container.addView(deleteBtn, delParams)
        }

        // Layout params
        val params = LinearLayout.LayoutParams(itemWidth.toInt(), itemHeight.toInt())
        if (imageContainer.childCount > 0) params.marginStart = itemSpacing.toInt()
        imageContainer.addView(container, params)

        // Load image
        loadImage(source, imageView, loading)
    }

    private fun loadImage(source: ImageSource, iv: ImageView, loading: ProgressBar) {
        val req = when (source) {
            is ImageSource.FileSource -> Glide.with(context).load(source.file)
            is ImageSource.BitmapSource -> Glide.with(context).load(source.bitmap)
            is ImageSource.UrlSource -> Glide.with(context).load(source.url)
            is ImageSource.UriSource -> Glide.with(context).load(source.uri)
            is ImageSource.DrawableSource -> Glide.with(context).load(source.resId)
        }

        req.transform(CenterCrop(), RoundedCorners(carouselCornerRadius.toInt()))
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    loading.visibility = GONE
                    iv.setImageDrawable(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) { iv.setImageDrawable(placeholder) }
                override fun onLoadFailed(errorDrawable: Drawable?) { loading.visibility = GONE }
            })
    }

    private fun showFullScreen(index: Int) {
        val source = imageSources.getOrNull(index) ?: return
        val builder = FullScreenImageViewer.Builder(context).setImageIndex(index, imageSources.size)
        when (source) {
            is ImageSource.FileSource -> builder.setImageFile(source.file)
            is ImageSource.BitmapSource -> builder.setImageBitmap(source.bitmap)
            is ImageSource.UrlSource -> builder.setImageUrl(source.url)
            is ImageSource.UriSource -> builder.setImageUri(source.uri)
            is ImageSource.DrawableSource -> builder.setImageDrawableRes(source.resId)
        }
        builder.show()
    }

    private fun rebuildIndices() {
        for ((i, child) in imageContainer.children.withIndex()) {
            child.tag = i
        }
    }

    private fun updateCurrentIndex(scrollX: Int) {
        if (imageSources.isEmpty()) return
        val newIdx = ((scrollX + itemWidth / 2) / (itemWidth + itemSpacing)).toInt()
            .coerceIn(0, imageSources.size - 1)
        if (newIdx != currentIndex) {
            currentIndex = newIdx
            updateIndicatorSelection()
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
        if (!showIndicators || imageSources.size <= 1) {
            indicatorContainer.visibility = GONE
            return
        }
        indicatorContainer.visibility = VISIBLE
        indicatorContainer.removeAllViews()
        for (i in imageSources.indices) {
            val dot = View(context).apply { background = createDot(i == currentIndex) }
            val p = LinearLayout.LayoutParams(indicatorSize.toInt(), indicatorSize.toInt())
            if (i > 0) p.marginStart = indicatorSpacing.toInt()
            indicatorContainer.addView(dot, p)
        }
    }

    private fun updateIndicatorSelection() {
        for ((i, child) in indicatorContainer.children.withIndex()) {
            child.background = createDot(i == currentIndex)
        }
    }

    private fun createDot(active: Boolean): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (active) indicatorActiveColor else indicatorInactiveColor
            }
            override fun draw(canvas: Canvas) {
                val r = minOf(bounds.width(), bounds.height()) / 2f
                canvas.drawCircle(bounds.centerX().toFloat(), bounds.centerY().toFloat(), r, paint)
            }
            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: android.graphics.ColorFilter?) { paint.colorFilter = cf }
            @Suppress("DEPRECATION")
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        var h = paddingTop + paddingBottom

        if (labelTextView.isVisible) {
            measureChild(labelTextView, MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            h += labelTextView.measuredHeight + carouselLabelGap.toInt()
        }

        val carouselH = itemHeight.toInt()
        measureChild(scrollView, MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(carouselH, MeasureSpec.EXACTLY))
        measureChild(emptyStateView, MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(carouselH, MeasureSpec.EXACTLY))
        measureChild(indicatorContainer, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))

        h += carouselH
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var top = paddingTop
        if (labelTextView.isVisible) {
            labelTextView.layout(paddingStart, top, paddingStart + labelTextView.measuredWidth,
                top + labelTextView.measuredHeight)
            top += labelTextView.measuredHeight + carouselLabelGap.toInt()
        }
        scrollView.layout(paddingStart, top, paddingStart + scrollView.measuredWidth,
            top + scrollView.measuredHeight)
        emptyStateView.layout(paddingStart, top, paddingStart + emptyStateView.measuredWidth,
            top + emptyStateView.measuredHeight)

        val indLeft = (width - indicatorContainer.measuredWidth) / 2
        val indBottom = top + scrollView.measuredHeight - dpToPx(8f).toInt()
        indicatorContainer.layout(indLeft, indBottom - indicatorContainer.measuredHeight,
            indLeft + indicatorContainer.measuredWidth, indBottom)
    }

    private fun dpToPx(dp: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
}
