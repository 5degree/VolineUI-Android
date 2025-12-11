@file:Suppress("unused")

package com.cropintellix.volineui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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
    private val deleteButton: ImageView
    private val emptyStateView: FrameLayout
    private val emptyStateIcon: ImageView
    private val emptyStateText: TextView

    // Properties
    private var imageLabel: String = ""
    private var imageLabelGap: Float = dpToPx(8f)
    private var imageLabelTextSize: Float = dpToPx(14f)
    private var imageLabelTextColor: Int = 0xFF252525.toInt()
    private var imageScaleType: ImageScaleType = ImageScaleType.CROP
    private var imageAspectRatio: Float = 0f
    private var placeholderResId: Int = 0
    private var errorResId: Int = 0
    private var imageSrcResId: Int = 0
    private var imageCornerRadius: Float = 0f
    private var imageBorderWidth: Float = dpToPx(1f)
    private var imageBorderColor: Int = 0xFFCCCCCC.toInt()  // Default #cccccc
    private var imageBackgroundColor: Int = Color.WHITE     // Default white
    private var showDeleteIcon: Boolean = false
    private var deleteIconTint: Int = 0xFFE53935.toInt()   // Error red
    private var deleteIconBgColor: Int = 0x33E53935.toInt() // Transparent red
    private var deleteIconCornerRadius: Float = 0f          // Matches imageCornerRadius
    private var showLoadingIndicator: Boolean = true
    private var enableFullScreenPreview: Boolean = true
    private var enableCameraCapture: Boolean = false
    private var emptyStateTextValue: String = "No image"

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

    // Paint for border and clipping
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val clipPath = Path()
    private val clipRect = RectF()

    init {
        setWillNotDraw(false)
        clipChildren = true
        clipToPadding = true

        // Label
        labelTextView = TextView(context).apply {
            visibility = GONE
            setTextSize(TypedValue.COMPLEX_UNIT_PX, imageLabelTextSize)
            setTextColor(imageLabelTextColor)
        }
        addView(labelTextView)

        // Image container with clipping
        imageContainer = FrameLayout(context).apply {
            clipChildren = true
            clipToPadding = true
            setBackgroundColor(imageBackgroundColor)
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

        // Delete button - styled red
        deleteButton = ImageView(context).apply {
            visibility = GONE
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(deleteIconTint)
            setPadding(dpToPx(6f).toInt(), dpToPx(6f).toInt(), dpToPx(6f).toInt(), dpToPx(6f).toInt())
            setOnClickListener {
                onDeleteClickListener?.invoke()
                clearImage()  // Always clear after delete click
            }
        }
        val deleteParams = LayoutParams(dpToPx(28f).toInt(), dpToPx(28f).toInt())
        deleteParams.gravity = Gravity.TOP or Gravity.END
        deleteParams.marginEnd = dpToPx(6f).toInt()
        deleteParams.topMargin = dpToPx(6f).toInt()
        imageContainer.addView(deleteButton, deleteParams)

        // Empty state
        emptyStateView = FrameLayout(context).apply {
            visibility = VISIBLE
            isClickable = true
            isFocusable = true
            setOnClickListener { handleEmptyStateClick() }
        }
        imageContainer.addView(emptyStateView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        emptyStateIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            alpha = 0.35f
            setColorFilter(0xFF666666.toInt())
        }
        val iconParams = LayoutParams(dpToPx(36f).toInt(), dpToPx(36f).toInt(), Gravity.CENTER)
        iconParams.bottomMargin = dpToPx(12f).toInt()
        emptyStateView.addView(emptyStateIcon, iconParams)

        emptyStateText = TextView(context).apply {
            text = emptyStateTextValue
            gravity = Gravity.CENTER
            alpha = 0.5f
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(0xFF666666.toInt())
        }
        val textParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        textParams.topMargin = dpToPx(12f).toInt()
        emptyStateView.addView(emptyStateText, textParams)

        // Parse attributes
        if (attrs != null) parseAttributes(attrs, defStyleAttr)

        // Click listener for fullscreen
        imageView.setOnClickListener {
            if (currentState == ImageState.LOADED && enableFullScreenPreview) {
                showFullScreenPreview()
            }
            onImageClickListener?.invoke()
        }

        updateDeleteButtonStyle()
        updateState(ImageState.EMPTY)
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AdvancedImageView, defStyleAttr, 0)

        try {
            imageLabel = typedArray.getString(R.styleable.AdvancedImageView_imageLabel) ?: ""
            if (imageLabel.isNotEmpty()) {
                labelTextView.text = imageLabel
                labelTextView.visibility = VISIBLE
            }

            imageLabelGap = typedArray.getDimension(R.styleable.AdvancedImageView_imageLabelGap, dpToPx(8f))
            imageLabelTextSize = typedArray.getDimension(R.styleable.AdvancedImageView_imageLabelTextSize, dpToPx(14f))
            imageLabelTextColor = typedArray.getColor(R.styleable.AdvancedImageView_imageLabelTextColor, 0xFF252525.toInt())
            labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, imageLabelTextSize)
            labelTextView.setTextColor(imageLabelTextColor)

            imageScaleType = ImageScaleType.fromValue(typedArray.getInt(R.styleable.AdvancedImageView_imageScaleType, ImageScaleType.CROP.value))
            imageAspectRatio = typedArray.getFloat(R.styleable.AdvancedImageView_imageAspectRatio, 0f)
            placeholderResId = typedArray.getResourceId(R.styleable.AdvancedImageView_placeholderImage, 0)
            errorResId = typedArray.getResourceId(R.styleable.AdvancedImageView_errorImage, 0)
            imageSrcResId = typedArray.getResourceId(R.styleable.AdvancedImageView_imageSrc, 0)

            imageCornerRadius = typedArray.getDimension(R.styleable.AdvancedImageView_imageCornerRadius, 0f)
            imageBorderWidth = typedArray.getDimension(R.styleable.AdvancedImageView_imageBorderWidth, dpToPx(1f))
            imageBorderColor = typedArray.getColor(R.styleable.AdvancedImageView_imageBorderColor, 0xFFCCCCCC.toInt())
            imageBackgroundColor = typedArray.getColor(R.styleable.AdvancedImageView_imageBackgroundColor, Color.WHITE)
            imageContainer.setBackgroundColor(imageBackgroundColor)

            showDeleteIcon = typedArray.getBoolean(R.styleable.AdvancedImageView_showDeleteIcon, false)
            deleteIconTint = typedArray.getColor(R.styleable.AdvancedImageView_deleteIconTint, 0xFFE53935.toInt())
            showLoadingIndicator = typedArray.getBoolean(R.styleable.AdvancedImageView_showLoadingIndicator, true)
            enableFullScreenPreview = typedArray.getBoolean(R.styleable.AdvancedImageView_enableFullScreenPreview, true)
            enableCameraCapture = typedArray.getBoolean(R.styleable.AdvancedImageView_enableCameraCapture, false)

            emptyStateTextValue = typedArray.getString(R.styleable.AdvancedImageView_emptyStateText)
                ?: if (enableCameraCapture) "Tap to capture" else "No image"
            emptyStateText.text = emptyStateTextValue

            val emptyIconRes = typedArray.getResourceId(R.styleable.AdvancedImageView_emptyStateIcon, 0)
            if (emptyIconRes != 0) emptyStateIcon.setImageResource(emptyIconRes)

            // Delete icon corner radius matches image corner radius (scaled down)
            deleteIconCornerRadius = (imageCornerRadius * 0.4f).coerceAtLeast(dpToPx(4f))

        } finally {
            typedArray.recycle()
        }

        borderPaint.color = imageBorderColor
        borderPaint.strokeWidth = imageBorderWidth

        // Show placeholder if provided
        if (placeholderResId != 0) {
            emptyStateIcon.setImageResource(placeholderResId)
            emptyStateIcon.alpha = 1f
            emptyStateIcon.clearColorFilter()
            emptyStateText.visibility = GONE
        }

        // Load image from XML if specified
        if (imageSrcResId != 0) {
            post { loadFromDrawable(imageSrcResId) }
        }
    }

    private fun updateDeleteButtonStyle() {
        deleteButton.setColorFilter(deleteIconTint)
        
        // Create rounded background
        val bg = GradientDrawable().apply {
            setColor(deleteIconBgColor)
            cornerRadius = deleteIconCornerRadius
        }
        deleteButton.background = bg
    }

    // Load methods

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
        
        var request = Glide.with(context).load(source)
        
        if (placeholderResId != 0) request = request.placeholder(placeholderResId)
        if (errorResId != 0) request = request.error(errorResId)
        
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
                deleteButton.visibility = GONE
                emptyStateView.visibility = VISIBLE
                // Restore original empty state (with placeholder if provided)
                if (placeholderResId != 0) {
                    emptyStateIcon.setImageResource(placeholderResId)
                    emptyStateIcon.alpha = 1f
                    emptyStateIcon.clearColorFilter()
                    emptyStateText.visibility = GONE
                } else {
                    emptyStateIcon.setImageResource(android.R.drawable.ic_menu_camera)
                    emptyStateIcon.alpha = 0.35f
                    emptyStateIcon.setColorFilter(0xFF666666.toInt())
                    emptyStateText.text = emptyStateTextValue
                    emptyStateText.visibility = VISIBLE
                }
            }
            ImageState.LOADING -> {
                imageView.visibility = GONE
                loadingIndicator.visibility = if (showLoadingIndicator) VISIBLE else GONE
                deleteButton.visibility = GONE
                emptyStateView.visibility = GONE
            }
            ImageState.LOADED -> {
                imageView.visibility = VISIBLE
                loadingIndicator.visibility = GONE
                deleteButton.visibility = if (showDeleteIcon) VISIBLE else GONE
                emptyStateView.visibility = GONE
            }
            ImageState.ERROR -> {
                imageView.visibility = GONE
                loadingIndicator.visibility = GONE
                deleteButton.visibility = GONE
                emptyStateView.visibility = VISIBLE
                emptyStateText.text = "Failed to load"
                emptyStateText.visibility = VISIBLE
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
        deleteIconCornerRadius = (radius * 0.4f).coerceAtLeast(dpToPx(4f))
        updateDeleteButtonStyle()
        reloadCurrentImage()
    }

    fun setBorder(width: Float, color: Int) {
        imageBorderWidth = width
        imageBorderColor = color
        borderPaint.strokeWidth = width
        borderPaint.color = color
        invalidate()
    }

    fun setShowDeleteIcon(show: Boolean) {
        showDeleteIcon = show
        if (currentState == ImageState.LOADED) {
            deleteButton.visibility = if (show) VISIBLE else GONE
        }
    }

    fun setEnableCameraCapture(enable: Boolean) {
        enableCameraCapture = enable
        if (placeholderResId == 0) {
            emptyStateText.text = if (enable) "Tap to capture" else "No image"
        }
    }

    private fun reloadCurrentImage() {
        when {
            currentImageFile != null -> loadFromFile(currentImageFile!!)
            currentImageBitmap != null -> loadFromBitmap(currentImageBitmap!!)
            currentImageUri != null -> loadFromUri(currentImageUri!!)
            currentImageUrl != null -> loadFromUrl(currentImageUrl!!)
        }
    }

    private fun handleEmptyStateClick() {
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
        // Clip content to rounded corners
        if (imageCornerRadius > 0) {
            val labelHeight = if (labelTextView.isVisible) labelTextView.measuredHeight + imageLabelGap else 0f
            clipPath.reset()
            clipRect.set(
                paddingStart.toFloat(),
                paddingTop + labelHeight,
                (width - paddingEnd).toFloat(),
                (height - paddingBottom).toFloat()
            )
            clipPath.addRoundRect(clipRect, imageCornerRadius, imageCornerRadius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(clipPath)
            super.dispatchDraw(canvas)
            canvas.restore()
        } else {
            super.dispatchDraw(canvas)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw border
        if (imageBorderWidth > 0 && imageBorderColor != Color.TRANSPARENT) {
            val labelHeight = if (labelTextView.isVisible) labelTextView.measuredHeight + imageLabelGap else 0f
            val offset = imageBorderWidth / 2
            clipRect.set(paddingStart + offset, paddingTop + labelHeight + offset,
                width - paddingEnd - offset, height - paddingBottom - offset)
            if (imageCornerRadius > 0) {
                canvas.drawRoundRect(clipRect, imageCornerRadius, imageCornerRadius, borderPaint)
            } else {
                canvas.drawRect(clipRect, borderPaint)
            }
        }
    }

    private fun dpToPx(dp: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
}
