@file:Suppress("unused")

package com.cropintellix.volineui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.cropintellix.volineui.imageview.FullScreenImageViewer
import com.cropintellix.volineui.photocapturemanager.PhotoCaptureConfig
import java.io.File

/**
 * AdvancedImageView - A comprehensive image display component
 */
class AdvancedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class ImageScaleType(val value: Int) {
        FIT(0), FILL(1), CROP(2), CENTER(3), STRETCH(4);
        companion object {
            fun fromValue(value: Int) = entries.find { it.value == value } ?: CROP
        }
    }

    enum class ImageState { EMPTY, LOADING, LOADED, ERROR }

    // Views
    private val labelTextView: TextView
    private val imageContainer: FrameLayout
    private val imageView: ImageView
    private val loadingIndicator: ProgressBar
    private val loadingGifView: ImageView
    private val deleteButton: ImageView
    private val placeholderContainer: LinearLayout
    private val placeholderIcon: ImageView
    private val placeholderTextView: TextView

    // Label properties (matching Radio component)
    private var imageLabel: String = ""
    private var imageLabelGap: Float = 0f
    private var imageLabelTextSize: Float = 0f
    private var imageLabelTextColor: Int = 0xFF252525.toInt()
    private var imageLabelTextStyle: Int = Typeface.NORMAL

    // Image properties
    private var imageScaleType: ImageScaleType = ImageScaleType.CROP
    private var imageAspectRatio: Float = 0f
    private var loadingGifResId: Int = 0
    private var imageCornerRadius: Float = 0f
    private var imageBorderWidth: Float = 0f
    private var imageBorderColor: Int = 0xFFCCCCCC.toInt()
    private var imageBackgroundColor: Int = Color.TRANSPARENT
    private var showDeleteIcon: Boolean = true
    private var deleteIconTint: Int = 0xFFE53935.toInt()
    private var showLoadingIndicator: Boolean = true
    private var enableFullScreenPreview: Boolean = true
    private var enableCameraCapture: Boolean = true

    // Track if attributes were explicitly set in XML
    private var borderWidthExplicitlySet = false
    private var cornerRadiusExplicitlySet = false
    private var showDeleteIconExplicitlySet = false

    // Placeholder properties
    private var placeholderIconResId: Int = 0
    private var placeholderText: String = "Tap to capture"
    private var placeholderIconColor: Int = 0xFF666666.toInt()
    private var placeholderTextColor: Int = 0xFF666666.toInt()
    private var placeholderGap: Float = 0f

    // State
    private var currentState: ImageState = ImageState.EMPTY
    private var currentImageFile: File? = null
    private var currentImageBitmap: Bitmap? = null
    private var currentImageUri: Uri? = null
    private var currentImageUrl: String? = null

    // Listeners
    private var onDeleteClickListener: (() -> Unit)? = null
    private var onImageClickListener: (() -> Unit)? = null
    private var onCaptureClickListener: ((PhotoCaptureConfig) -> Unit)? = null
    private var onImageLoadListener: ((Boolean) -> Unit)? = null

    // Paint for border
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val borderRect = RectF()
    private val clipPath = Path()

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false

        // Initialize defaults - these will be used when no imageSrc and not explicitly set
        imageLabelGap = dpToPx(5f)
        imageLabelTextSize = dpToPx(14f)
        imageBorderWidth = dpToPx(1f)  // Default for placeholder mode
        imageCornerRadius = dpToPx(8f)  // Default for placeholder mode
        placeholderGap = dpToPx(8f)

        borderPaint.color = imageBorderColor
        borderPaint.strokeWidth = imageBorderWidth

        // Label
        labelTextView = TextView(context).apply {
            visibility = GONE
            setTextSize(TypedValue.COMPLEX_UNIT_PX, imageLabelTextSize)
            setTextColor(imageLabelTextColor)
        }
        addView(labelTextView)

        // Image container
        imageContainer = FrameLayout(context).apply {
            clipChildren = true
            clipToPadding = true
        }
        addView(imageContainer)

        // Image view
        imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = GONE
        }
        imageContainer.addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Loading indicator
        loadingIndicator = ProgressBar(context).apply {
            visibility = GONE
            isIndeterminate = true
        }
        imageContainer.addView(loadingIndicator, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ))

        // Loading GIF view
        loadingGifView = ImageView(context).apply {
            visibility = GONE
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        imageContainer.addView(loadingGifView, LayoutParams(
            dpToPx(48f).toInt(), dpToPx(48f).toInt(), Gravity.CENTER
        ))

        // Delete button
        deleteButton = ImageView(context).apply {
            visibility = GONE
            setImageResource(R.drawable.ic_clear)
            setColorFilter(deleteIconTint)
            setPadding(dpToPx(5f).toInt(), dpToPx(5f).toInt(), dpToPx(5f).toInt(), dpToPx(5f).toInt())
            setOnClickListener {
                onDeleteClickListener?.invoke()
                clearImage()
            }
        }
        val deleteParams = LayoutParams(dpToPx(26f).toInt(), dpToPx(26f).toInt())
        deleteParams.gravity = Gravity.TOP or Gravity.END
        deleteParams.marginEnd = dpToPx(6f).toInt()
        deleteParams.topMargin = dpToPx(6f).toInt()
        imageContainer.addView(deleteButton, deleteParams)

        // Placeholder container (LinearLayout for vertical arrangement with gap)
        placeholderContainer = LinearLayout(context).apply {
            visibility = VISIBLE
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener { handlePlaceholderClick() }
        }
        imageContainer.addView(placeholderContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        placeholderIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_add_photo)
            setColorFilter(placeholderIconColor)
        }
        val iconParams = LinearLayout.LayoutParams(dpToPx(36f).toInt(), dpToPx(36f).toInt())
        placeholderContainer.addView(placeholderIcon, iconParams)

        placeholderTextView = TextView(context).apply {
            text = placeholderText
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(placeholderTextColor)
        }
        val textParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        textParams.topMargin = placeholderGap.toInt()
        placeholderContainer.addView(placeholderTextView, textParams)

        // Parse XML attributes
        if (attrs != null) parseAttributes(attrs, defStyleAttr)

        // Click listener for fullscreen
        imageView.setOnClickListener {
            if (currentState == ImageState.LOADED && enableFullScreenPreview) {
                showFullScreenPreview()
            }
            onImageClickListener?.invoke()
        }

        updateDeleteButtonStyle()
        updateContainerBackground()
        updateState(ImageState.EMPTY)
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AdvancedImageView, defStyleAttr, 0)

        try {
            imageLabel = ta.getString(R.styleable.AdvancedImageView_imageLabel) ?: ""
            imageLabelGap = ta.getDimension(R.styleable.AdvancedImageView_imageLabelGap, imageLabelGap)
            imageLabelTextSize = ta.getDimension(R.styleable.AdvancedImageView_imageLabelTextSize, imageLabelTextSize)
            imageLabelTextColor = ta.getColor(R.styleable.AdvancedImageView_imageLabelTextColor, imageLabelTextColor)
            imageLabelTextStyle = ta.getInt(R.styleable.AdvancedImageView_imageLabelTextStyle, Typeface.NORMAL)

            if (imageLabel.isNotEmpty()) {
                labelTextView.text = imageLabel
                labelTextView.visibility = VISIBLE
            }
            labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, imageLabelTextSize)
            labelTextView.setTextColor(imageLabelTextColor)
            labelTextView.setTypeface(labelTextView.typeface, imageLabelTextStyle)

            imageScaleType = ImageScaleType.fromValue(ta.getInt(R.styleable.AdvancedImageView_imageScaleType, ImageScaleType.CROP.value))
            imageAspectRatio = ta.getFloat(R.styleable.AdvancedImageView_imageAspectRatio, 0f)
            loadingGifResId = ta.getResourceId(R.styleable.AdvancedImageView_loadingGif, 0)

            // Placeholder
            placeholderIconResId = ta.getResourceId(R.styleable.AdvancedImageView_placeholderIcon, 0)
            placeholderText = ta.getString(R.styleable.AdvancedImageView_placeholderText) ?: placeholderText
            placeholderIconColor = ta.getColor(R.styleable.AdvancedImageView_placeholderIconColor, placeholderIconColor)
            placeholderTextColor = ta.getColor(R.styleable.AdvancedImageView_placeholderTextColor, placeholderTextColor)
            placeholderGap = ta.getDimension(R.styleable.AdvancedImageView_placeholderGap, placeholderGap)

            // Apply placeholder
            if (placeholderIconResId != 0) {
                placeholderIcon.setImageResource(placeholderIconResId)
                placeholderIcon.clearColorFilter()
            } else {
                placeholderIcon.setColorFilter(placeholderIconColor)
            }
            placeholderTextView.text = placeholderText
            placeholderTextView.setTextColor(placeholderTextColor)
            (placeholderTextView.layoutParams as? LinearLayout.LayoutParams)?.topMargin = placeholderGap.toInt()

            // Check if visual styling attributes were explicitly set
            borderWidthExplicitlySet = ta.hasValue(R.styleable.AdvancedImageView_imageBorderWidth)
            cornerRadiusExplicitlySet = ta.hasValue(R.styleable.AdvancedImageView_imageCornerRadius)
            showDeleteIconExplicitlySet = ta.hasValue(R.styleable.AdvancedImageView_showDeleteIcon)

            // Visual styling - read values if explicitly set
            if (cornerRadiusExplicitlySet) {
                imageCornerRadius = ta.getDimension(R.styleable.AdvancedImageView_imageCornerRadius, imageCornerRadius)
            }
            if (borderWidthExplicitlySet) {
                imageBorderWidth = ta.getDimension(R.styleable.AdvancedImageView_imageBorderWidth, imageBorderWidth)
            }
            imageBorderColor = ta.getColor(R.styleable.AdvancedImageView_imageBorderColor, imageBorderColor)
            imageBackgroundColor = ta.getColor(R.styleable.AdvancedImageView_imageBackgroundColor, imageBackgroundColor)

            // Features
            if (showDeleteIconExplicitlySet) {
                showDeleteIcon = ta.getBoolean(R.styleable.AdvancedImageView_showDeleteIcon, showDeleteIcon)
            }
            deleteIconTint = ta.getColor(R.styleable.AdvancedImageView_deleteIconTint, deleteIconTint)
            showLoadingIndicator = ta.getBoolean(R.styleable.AdvancedImageView_showLoadingIndicator, true)
            enableFullScreenPreview = ta.getBoolean(R.styleable.AdvancedImageView_enableFullScreenPreview, true)
            enableCameraCapture = ta.getBoolean(R.styleable.AdvancedImageView_enableCameraCapture, true)

            // Load imageSrc from XML
            val imageSrcResId = ta.getResourceId(R.styleable.AdvancedImageView_imageSrc, 0)
            if (imageSrcResId != 0) {
                // When imageSrc is set, use display-only defaults unless explicitly overridden
                if (!borderWidthExplicitlySet) imageBorderWidth = 0f
                if (!cornerRadiusExplicitlySet) imageCornerRadius = 0f
                if (!showDeleteIconExplicitlySet) showDeleteIcon = false

                // Apply the scaleType to imageView directly
                imageView.scaleType = getAndroidScaleType()

                if (isInEditMode) {
                    // In design preview, load directly without Glide
                    imageView.setImageResource(imageSrcResId)
                    imageView.visibility = VISIBLE
                    placeholderContainer.visibility = GONE
                    deleteButton.visibility = GONE
                    loadingIndicator.visibility = GONE
                    loadingGifView.visibility = GONE
                    currentState = ImageState.LOADED

                    // Apply visual styling for preview
                    updateContainerBackground()
                    updateDeleteButtonStyle()

                    // Force a layout pass
                    post {
                        requestLayout()
                        invalidate()
                    }
                } else {
                    post { loadFromDrawable(imageSrcResId) }
                }
            }

        } finally {
            ta.recycle()
        }

        borderPaint.color = imageBorderColor
        borderPaint.strokeWidth = imageBorderWidth
    }

    private fun updateDeleteButtonStyle() {
        deleteButton.setColorFilter(deleteIconTint)
        val deleteCorner = (imageCornerRadius * 0.4f).coerceAtLeast(dpToPx(4f))
        val bg = GradientDrawable().apply {
            setColor(0x33E53935.toInt())
            cornerRadius = deleteCorner
        }
        deleteButton.background = bg
    }

    private fun updateContainerBackground() {
        val bg = GradientDrawable().apply {
            setColor(imageBackgroundColor)
            cornerRadius = imageCornerRadius
        }
        imageContainer.background = bg
    }

    /**
     * Maps our ImageScaleType enum to Android's ImageView.ScaleType
     */
    private fun getAndroidScaleType(): ImageView.ScaleType {
        return when (imageScaleType) {
            ImageScaleType.FIT -> ImageView.ScaleType.FIT_CENTER
            ImageScaleType.FILL, ImageScaleType.CROP -> ImageView.ScaleType.CENTER_CROP
            ImageScaleType.CENTER -> ImageView.ScaleType.CENTER_INSIDE
            ImageScaleType.STRETCH -> ImageView.ScaleType.FIT_XY
        }
    }

    // Public methods

    /**
     * Show loading state (for PhotoCaptureResult.Processing)
     */
    fun showLoading() {
        updateState(ImageState.LOADING)
    }

    fun loadFromUrl(url: String) {
        if (url.isBlank()) { updateState(ImageState.EMPTY); return }
        currentImageUrl = url
        currentImageFile = null
        currentImageBitmap = null
        currentImageUri = null
        loadImageWithGlide(url)
    }

    fun loadFromFile(file: File) {
        if (!file.exists()) {
            updateState(ImageState.ERROR)
            onImageLoadListener?.invoke(false)
            return
        }
        currentImageFile = file
        currentImageUrl = null
        currentImageBitmap = null
        currentImageUri = null
        loadImageWithGlide(file)
    }

    fun loadFromBitmap(bitmap: Bitmap) {
        currentImageBitmap = bitmap
        currentImageFile = null
        currentImageUrl = null
        currentImageUri = null
        loadImageWithGlide(bitmap)
    }

    fun loadFromUri(uri: Uri) {
        currentImageUri = uri
        currentImageFile = null
        currentImageUrl = null
        currentImageBitmap = null
        loadImageWithGlide(uri)
    }

    fun loadFromDrawable(drawableResId: Int) {
        currentImageFile = null
        currentImageUrl = null
        currentImageBitmap = null
        currentImageUri = null
        loadImageWithGlide(drawableResId)
    }

    fun loadFromBase64(base64String: String) {
        try {
            val bytes = Base64.decode(base64String, Base64.DEFAULT)
            loadImageWithGlide(bytes)
        } catch (e: Exception) {
            updateState(ImageState.ERROR)
            onImageLoadListener?.invoke(false)
        }
    }

    private fun loadImageWithGlide(source: Any) {
        updateState(ImageState.LOADING)

        // Apply scaleType directly to imageView
        imageView.scaleType = getAndroidScaleType()

        var request = Glide.with(context).load(source)

        val transforms = mutableListOf<com.bumptech.glide.load.Transformation<Bitmap>>()
        transforms.add(when (imageScaleType) {
            ImageScaleType.FIT -> FitCenter()
            ImageScaleType.FILL, ImageScaleType.CROP -> CenterCrop()
            ImageScaleType.CENTER -> CenterInside()
            ImageScaleType.STRETCH -> FitCenter()
        })
        if (imageCornerRadius > 0) {
            transforms.add(RoundedCorners(imageCornerRadius.toInt()))
        }

        request = request.transform(*transforms.toTypedArray())

        request.into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                imageView.setImageDrawable(resource)
                if (resource is GifDrawable) resource.start()
                updateState(ImageState.LOADED)
                onImageLoadListener?.invoke(true)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                imageView.setImageDrawable(placeholder)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                imageView.setImageDrawable(errorDrawable)
                updateState(ImageState.ERROR)
                onImageLoadListener?.invoke(false)
            }
        })
    }

    private fun updateState(newState: ImageState) {
        currentState = newState
        when (newState) {
            ImageState.EMPTY -> {
                imageView.visibility = GONE
                loadingIndicator.visibility = GONE
                loadingGifView.visibility = GONE
                deleteButton.visibility = GONE
                placeholderContainer.visibility = VISIBLE
                // Restore placeholder
                if (placeholderIconResId != 0) {
                    placeholderIcon.setImageResource(placeholderIconResId)
                    placeholderIcon.clearColorFilter()
                } else {
                    placeholderIcon.setImageResource(R.drawable.ic_add_photo)
                    placeholderIcon.setColorFilter(placeholderIconColor)
                }
                placeholderTextView.text = placeholderText
                placeholderTextView.setTextColor(placeholderTextColor)
            }
            ImageState.LOADING -> {
                imageView.visibility = GONE
                placeholderContainer.visibility = GONE
                deleteButton.visibility = GONE
                
                if (loadingGifResId != 0) {
                    loadingIndicator.visibility = GONE
                    loadingGifView.visibility = VISIBLE
                    Glide.with(context).load(loadingGifResId).into(loadingGifView)
                } else if (showLoadingIndicator) {
                    loadingIndicator.visibility = VISIBLE
                    loadingGifView.visibility = GONE
                }
            }
            ImageState.LOADED -> {
                imageView.visibility = VISIBLE
                loadingIndicator.visibility = GONE
                loadingGifView.visibility = GONE
                deleteButton.visibility = if (showDeleteIcon) VISIBLE else GONE
                placeholderContainer.visibility = GONE
            }
            ImageState.ERROR -> {
                imageView.visibility = GONE
                loadingIndicator.visibility = GONE
                loadingGifView.visibility = GONE
                deleteButton.visibility = GONE
                placeholderContainer.visibility = VISIBLE
                placeholderTextView.text = "Failed to load"
            }
        }
        invalidate()
    }

    fun clearImage() {
        Glide.with(context).clear(imageView)
        imageView.setImageDrawable(null)
        currentImageFile = null
        currentImageBitmap = null
        currentImageUri = null
        currentImageUrl = null
        updateState(ImageState.EMPTY)
    }

    fun hasImage(): Boolean = currentState == ImageState.LOADED
    fun getState(): ImageState = currentState
    fun getImageFile(): File? = currentImageFile
    fun getImageBitmap(): Bitmap? = currentImageBitmap
    fun getImageUri(): Uri? = currentImageUri

    // Listeners
    fun setOnDeleteClickListener(listener: (() -> Unit)?) { onDeleteClickListener = listener }
    fun setOnImageClickListener(listener: (() -> Unit)?) { onImageClickListener = listener }
    fun setOnCaptureClickListener(listener: ((PhotoCaptureConfig) -> Unit)?) { onCaptureClickListener = listener }
    fun setOnImageLoadListener(listener: ((Boolean) -> Unit)?) { onImageLoadListener = listener }

    // Configuration
    fun setLabel(text: String) {
        imageLabel = text
        labelTextView.text = text
        labelTextView.visibility = if (text.isNotEmpty()) VISIBLE else GONE
        requestLayout()
    }

    fun setCornerRadius(radius: Float) {
        imageCornerRadius = radius
        updateDeleteButtonStyle()
        updateContainerBackground()
        reloadCurrentImage()
    }

    fun setBorder(width: Float, color: Int) {
        imageBorderWidth = width
        imageBorderColor = color
        borderPaint.strokeWidth = width
        borderPaint.color = color
        invalidate()
    }

    private fun reloadCurrentImage() {
        when {
            currentImageFile != null -> loadFromFile(currentImageFile!!)
            currentImageBitmap != null -> loadFromBitmap(currentImageBitmap!!)
            currentImageUri != null -> loadFromUri(currentImageUri!!)
            currentImageUrl != null -> loadFromUrl(currentImageUrl!!)
        }
    }

    private fun handlePlaceholderClick() {
        if (enableCameraCapture && onCaptureClickListener != null) {
            onCaptureClickListener?.invoke(PhotoCaptureConfig())
        }
    }

    private fun showFullScreenPreview() {
        val builder = FullScreenImageViewer.Builder(context)
        when {
            currentImageFile != null -> builder.setImageFile(currentImageFile!!)
            currentImageBitmap != null -> builder.setImageBitmap(currentImageBitmap!!)
            currentImageUri != null -> builder.setImageUri(currentImageUri!!)
            currentImageUrl != null -> builder.setImageUrl(currentImageUrl!!)
            else -> return
        }
        builder.show()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        var totalHeight = paddingTop + paddingBottom

        if (labelTextView.isVisible) {
            measureChild(labelTextView,
                MeasureSpec.makeMeasureSpec(width - paddingStart - paddingEnd, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            totalHeight += labelTextView.measuredHeight + imageLabelGap.toInt()
        }

        val containerWidth = width - paddingStart - paddingEnd
        val containerHeight = if (imageAspectRatio > 0) {
            (containerWidth / imageAspectRatio).toInt()
        } else {
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize - totalHeight
                MeasureSpec.AT_MOST -> (heightSize - totalHeight).coerceAtMost(containerWidth)
                else -> containerWidth
            }
        }

        measureChild(imageContainer,
            MeasureSpec.makeMeasureSpec(containerWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(containerHeight, MeasureSpec.EXACTLY))

        totalHeight += containerHeight
        setMeasuredDimension(width, totalHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var currentTop = paddingTop

        if (labelTextView.isVisible) {
            labelTextView.layout(paddingStart, currentTop,
                paddingStart + labelTextView.measuredWidth, currentTop + labelTextView.measuredHeight)
            currentTop += labelTextView.measuredHeight + imageLabelGap.toInt()
        }

        imageContainer.layout(paddingStart, currentTop,
            paddingStart + imageContainer.measuredWidth, currentTop + imageContainer.measuredHeight)
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Draw label first (not clipped)
        if (labelTextView.isVisible) {
            drawChild(canvas, labelTextView, drawingTime)
        }

        // Calculate image container area (excluding label)
        val labelHeight = if (labelTextView.isVisible) labelTextView.measuredHeight + imageLabelGap else 0f
        borderRect.set(
            paddingStart.toFloat(),
            paddingTop + labelHeight,
            (width - paddingEnd).toFloat(),
            (height - paddingBottom).toFloat()
        )

        // Clip and draw image container with rounded corners
        if (imageCornerRadius > 0) {
            clipPath.reset()
            clipPath.addRoundRect(borderRect, imageCornerRadius, imageCornerRadius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(clipPath)
            drawChild(canvas, imageContainer, drawingTime)
            canvas.restore()
        } else {
            drawChild(canvas, imageContainer, drawingTime)
        }

        // Draw border AFTER children (on top)
        if (imageBorderWidth > 0) {
            val offset = imageBorderWidth / 2
            borderRect.set(
                paddingStart + offset,
                paddingTop + labelHeight + offset,
                width - paddingEnd - offset,
                height - paddingBottom - offset
            )
            canvas.drawRoundRect(borderRect, imageCornerRadius, imageCornerRadius, borderPaint)
        }

        drawChild(canvas, imageContainer, drawingTime)
    }

    private fun dpToPx(dp: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
}
