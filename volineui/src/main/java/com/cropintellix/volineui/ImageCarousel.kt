@file:Suppress("unused")

package com.cropintellix.volineui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
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
import java.io.File

/**
 * ImageCarousel - Horizontal scrolling image carousel with add button and delete icons
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
    private lateinit var addButton: FrameLayout

    // Label properties
    private var carouselLabel: String = ""
    private var carouselLabelGap: Float = 0f
    private var carouselLabelTextSize: Float = 0f
    private var carouselLabelTextColor: Int = 0xFF252525.toInt()
    private var carouselLabelTextStyle: Int = Typeface.NORMAL

    // Item properties
    private var itemWidth: Float = 0f
    private var itemHeight: Float = 0f
    private var itemSpacing: Float = 0f
    private var carouselCornerRadius: Float = 0f
    private var carouselBorderWidth: Float = 0f
    private var carouselBorderColor: Int = 0xFFCCCCCC.toInt()

    // Indicators
    private var showIndicators: Boolean = true
    private var indicatorSize: Float = 0f
    private var indicatorSpacing: Float = 0f
    private var indicatorActiveColor: Int = Color.WHITE
    private var indicatorInactiveColor: Int = 0x80FFFFFF.toInt()

    // Features
    private var showItemDeleteIcon: Boolean = true
    private var enableFullScreen: Boolean = true
    private var maxImageCount: Int = Int.MAX_VALUE

    // State
    private val imageFiles = mutableListOf<File>()
    private val imageSources = mutableListOf<ImageSource>()
    private var currentIndex = 0

    // Listeners
    private var onImageClickListener: ((Int) -> Unit)? = null
    private var onImageDeleteListener: ((Int) -> Unit)? = null
    private var onAddClickListener: (() -> Unit)? = null

    init {
        setWillNotDraw(false)

        // Initialize dp values
        carouselLabelGap = dpToPx(5f)
        carouselLabelTextSize = dpToPx(14f)
        itemWidth = dpToPx(120f)
        itemHeight = dpToPx(120f)
        itemSpacing = dpToPx(8f)
        carouselCornerRadius = dpToPx(8f)
        carouselBorderWidth = dpToPx(1f)
        indicatorSize = dpToPx(6f)
        indicatorSpacing = dpToPx(4f)

        // Label
        labelTextView = TextView(context).apply {
            visibility = GONE
            setTextSize(TypedValue.COMPLEX_UNIT_PX, carouselLabelTextSize)
            setTextColor(carouselLabelTextColor)
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

        scrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            updateCurrentIndex(scrollX)
        }

        if (attrs != null) parseAttributes(attrs, defStyleAttr)

        // Create add button
        createAddButton()
        updateAddButtonVisibility()
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ImageCarousel, defStyleAttr, 0)
        try {
            // Label (matching Radio style)
            carouselLabel = ta.getString(R.styleable.ImageCarousel_carouselLabel) ?: ""
            carouselLabelGap = ta.getDimension(R.styleable.ImageCarousel_carouselLabelGap, carouselLabelGap)
            carouselLabelTextSize = ta.getDimension(R.styleable.ImageCarousel_carouselLabelTextSize, carouselLabelTextSize)
            carouselLabelTextColor = ta.getColor(R.styleable.ImageCarousel_carouselLabelTextColor, carouselLabelTextColor)
            carouselLabelTextStyle = ta.getInt(R.styleable.ImageCarousel_carouselLabelTextStyle, Typeface.NORMAL)

            if (carouselLabel.isNotEmpty()) {
                labelTextView.text = carouselLabel
                labelTextView.visibility = VISIBLE
            }
            labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, carouselLabelTextSize)
            labelTextView.setTextColor(carouselLabelTextColor)
            labelTextView.setTypeface(labelTextView.typeface, carouselLabelTextStyle)

            // Items
            itemWidth = ta.getDimension(R.styleable.ImageCarousel_carouselItemWidth, itemWidth)
            itemHeight = ta.getDimension(R.styleable.ImageCarousel_carouselItemHeight, itemHeight)
            itemSpacing = ta.getDimension(R.styleable.ImageCarousel_carouselItemSpacing, itemSpacing)
            carouselCornerRadius = ta.getDimension(R.styleable.ImageCarousel_carouselCornerRadius, carouselCornerRadius)
            carouselBorderWidth = ta.getDimension(R.styleable.ImageCarousel_carouselBorderWidth, carouselBorderWidth)
            carouselBorderColor = ta.getColor(R.styleable.ImageCarousel_carouselBorderColor, carouselBorderColor)

            // Indicators
            showIndicators = ta.getBoolean(R.styleable.ImageCarousel_showIndicators, true)
            indicatorSize = ta.getDimension(R.styleable.ImageCarousel_indicatorSize, indicatorSize)
            indicatorSpacing = ta.getDimension(R.styleable.ImageCarousel_indicatorSpacing, indicatorSpacing)
            indicatorActiveColor = ta.getColor(R.styleable.ImageCarousel_indicatorActiveColor, indicatorActiveColor)
            indicatorInactiveColor = ta.getColor(R.styleable.ImageCarousel_indicatorInactiveColor, indicatorInactiveColor)

            // Features
            showItemDeleteIcon = ta.getBoolean(R.styleable.ImageCarousel_showItemDeleteIcon, true)
            enableFullScreen = ta.getBoolean(R.styleable.ImageCarousel_enableCarouselFullScreen, true)
            maxImageCount = ta.getInt(R.styleable.ImageCarousel_maxImageCount, Int.MAX_VALUE)
        } finally {
            ta.recycle()
        }
    }

    private fun createAddButton() {
        addButton = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { onAddClickListener?.invoke() }
        }
        updateAddButtonBackground()

        // Plus icon
        val plusIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_input_add)
            setColorFilter(0xFF666666.toInt())
            alpha = 0.6f
        }
        val iconParams = LayoutParams(dpToPx(32f).toInt(), dpToPx(32f).toInt(), Gravity.CENTER)
        addButton.addView(plusIcon, iconParams)

        // Add to container
        val params = LinearLayout.LayoutParams(itemWidth.toInt(), itemHeight.toInt())
        imageContainer.addView(addButton, params)
    }

    private fun updateAddButtonBackground() {
        val bg = GradientDrawable().apply {
            setColor(Color.WHITE)
            setStroke(carouselBorderWidth.toInt().coerceAtLeast(1), carouselBorderColor)
            cornerRadius = carouselCornerRadius
        }
        addButton.background = bg
        addButton.clipToOutline = true
    }

    private fun updateAddButtonVisibility() {
        val showAdd = imageSources.size < maxImageCount
        addButton.visibility = if (showAdd) VISIBLE else GONE
        
        // Update add button margin
        val params = addButton.layoutParams as? LinearLayout.LayoutParams
        params?.marginStart = if (imageSources.isNotEmpty()) itemSpacing.toInt() else 0
        params?.width = itemWidth.toInt()
        params?.height = itemHeight.toInt()
        addButton.layoutParams = params
        updateAddButtonBackground()
    }

    // Public API

    fun getMaxImageCount(): Int = maxImageCount
    fun canAddMore(): Boolean = imageSources.size < maxImageCount

    fun addImage(file: File) {
        if (!canAddMore()) return
        imageFiles.add(file)
        imageSources.add(ImageSource.FileSource(file))
        addImageView(ImageSource.FileSource(file), imageSources.size - 1)
        updateAddButtonVisibility()
        updateIndicators()
    }

    fun addImage(bitmap: Bitmap) {
        if (!canAddMore()) return
        imageSources.add(ImageSource.BitmapSource(bitmap))
        addImageView(ImageSource.BitmapSource(bitmap), imageSources.size - 1)
        updateAddButtonVisibility()
        updateIndicators()
    }

    fun addImage(url: String) {
        if (!canAddMore()) return
        imageSources.add(ImageSource.UrlSource(url))
        addImageView(ImageSource.UrlSource(url), imageSources.size - 1)
        updateAddButtonVisibility()
        updateIndicators()
    }

    fun addImageFiles(files: List<File>) { files.forEach { addImage(it) } }
    fun addImageBitmaps(bitmaps: List<Bitmap>) { bitmaps.forEach { addImage(it) } }
    fun addImageUrls(urls: List<String>) { urls.forEach { addImage(it) } }

    fun getImageFiles(): List<File> = imageFiles.toList()

    fun setImages(files: List<File>) {
        clearImages()
        files.take(maxImageCount).forEach { addImage(it) }
    }

    fun removeImageAt(index: Int) {
        if (index in imageSources.indices) {
            val source = imageSources.removeAt(index)
            if (source is ImageSource.FileSource) {
                imageFiles.remove(source.file)
            }
            
            val viewIndex = index
            if (viewIndex < imageContainer.childCount - 1) {
                imageContainer.removeViewAt(viewIndex)
            }
            
            rebuildIndices()
            updateAddButtonVisibility()
            updateIndicators()
            onImageDeleteListener?.invoke(index)
        }
    }

    fun clearImages() {
        imageFiles.clear()
        imageSources.clear()
        while (imageContainer.childCount > 1) {
            imageContainer.removeViewAt(0)
        }
        updateAddButtonVisibility()
        updateIndicators()
    }

    fun getImageCount(): Int = imageSources.size
    fun getCurrentIndex(): Int = currentIndex

    fun setOnImageClickListener(listener: ((Int) -> Unit)?) { onImageClickListener = listener }
    fun setOnImageDeleteListener(listener: ((Int) -> Unit)?) { onImageDeleteListener = listener }
    fun setOnAddClickListener(listener: (() -> Unit)?) { onAddClickListener = listener }

    fun setLabel(text: String) {
        carouselLabel = text
        labelTextView.text = text
        labelTextView.visibility = if (text.isNotEmpty()) VISIBLE else GONE
        requestLayout()
    }

    private fun addImageView(source: ImageSource, index: Int) {
        val container = FrameLayout(context).apply {
            tag = index
            clipToOutline = true
        }

        // Background with border and rounded corners
        val bg = GradientDrawable().apply {
            setColor(Color.WHITE)
            setStroke(carouselBorderWidth.toInt().coerceAtLeast(1), carouselBorderColor)
            cornerRadius = carouselCornerRadius
        }
        container.background = bg
        container.clipToOutline = true

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

        // Delete button
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
        if (index > 0) params.marginStart = itemSpacing.toInt()
        
        // Insert before add button
        val insertIndex = imageContainer.childCount - 1
        imageContainer.addView(container, insertIndex, params)

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

        // Apply corner radius transform
        req.transform(CenterCrop(), RoundedCorners(carouselCornerRadius.toInt().coerceAtLeast(1)))
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    loading.visibility = GONE
                    iv.setImageDrawable(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) { iv.setImageDrawable(placeholder) }
                override fun onLoadFailed(errorDrawable: Drawable?) { loading.visibility = GONE }
            })
    }

    private fun showFullScreen(startIndex: Int) {
        FullScreenImageViewer.showCarousel(context, imageSources, startIndex)
    }

    private fun rebuildIndices() {
        var idx = 0
        for (child in imageContainer.children) {
            if (child != addButton) {
                child.tag = idx
                idx++
            }
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

        val indLeft = (width - indicatorContainer.measuredWidth) / 2
        val indBottom = top + scrollView.measuredHeight - dpToPx(8f).toInt()
        indicatorContainer.layout(indLeft, indBottom - indicatorContainer.measuredHeight,
            indLeft + indicatorContainer.measuredWidth, indBottom)
    }

    private fun dpToPx(dp: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
}
