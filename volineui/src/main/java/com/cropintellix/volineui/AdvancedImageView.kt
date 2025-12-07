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
import android.net.Uri
import android.util.AttributeSet
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File

/**
 * AdvancedImageView - A comprehensive image display component with advanced features
 * 
 * Features:
 * - Multiple scale types (fit, fill, crop, center, stretch)
 * - Label support above the image (like InputField and Radio)
 * - Placeholder and error image handling
 * - Loading indicator with shimmer effect
 * - Delete icon on top-right corner
 * - Rounded corners and border support
 * - Full-screen preview on tap (when image is loaded)
 * - Camera capture integration via PhotoCaptureManager
 * - Multiple image source support (URL, file, Bitmap, Base64, drawable, content URI)
 * - GIF animation support via Glide
 * 
 * Usage:
 * ```kotlin
 * // In XML
 * <com.cropintellix.volineui.AdvancedImageView
 *     android:id="@+id/imageView"
 *     android:layout_width="match_parent"
 *     android:layout_height="200dp"
 *     app:imageLabel="Profile Photo"
 *     app:imageCornerRadius="12dp"
 *     app:showDeleteIcon="true"
 *     app:enableCameraCapture="true" />
 * 
 * // Programmatically
 * imageView.loadFromUrl("https://example.com/image.jpg")
 * imageView.loadFromFile(photoFile)
 * imageView.loadFromBitmap(bitmap)
 * 
 * // Camera capture integration
 * imageView.setOnCaptureClickListener { config ->
 *     PhotoCaptureManager.instance.capturePhoto(config) { result ->
 *         if (result is PhotoCaptureResult.Success) {
 *             imageView.loadFromFile(result.file)
 *         }
 *     }
 * }
 * ```
 */
class AdvancedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Scale type enum
    enum class ImageScaleType(val value: Int) {
        FIT(0),      // Scale to fit within bounds, maintaining aspect ratio
        FILL(1),     // Scale to fill bounds, may crop
        CROP(2),     // Center crop
        CENTER(3),   // Center without scaling
        STRETCH(4);  // Stretch to fill, ignoring aspect ratio
        
        companion object {
            fun fromValue(value: Int) = entries.find { it.value == value } ?: FIT
        }
    }

    // State enum
    enum class ImageState {
        EMPTY,      // No image loaded
        LOADING,    // Image is loading
        LOADED,     // Image loaded successfully
        ERROR       // Failed to load image
    }

    // Views
    private val labelTextView: TextView
    private val imageContainer: FrameLayout
    private val imageView: ImageView
    private val loadingIndicator: ProgressBar
    private val deleteButton: ImageView
    private val emptyStateView: FrameLayout
    private val emptyStateIcon: ImageView
    private val emptyStateText: TextView

    // Label properties
    private var imageLabel: String = ""
    private var imageLabelGap: Float = dpToPx(8f)
    private var imageLabelTextSize: Float = dpToPx(14f)
    private var imageLabelTextColor: Int = 0xFF252525.toInt()

    // Image display properties
    private var imageScaleType: ImageScaleType = ImageScaleType.CROP
    private var imageAspectRatio: Float = 0f  // 0 = no fixed ratio
    private var placeholderResId: Int = 0
    private var errorResId: Int = 0

    // Visual styling
    private var imageCornerRadius: Float = 0f
    private var imageBorderWidth: Float = 0f
    private var imageBorderColor: Int = Color.TRANSPARENT
    private var imageBackgroundColor: Int = Color.TRANSPARENT

    // Features
    private var showDeleteIcon: Boolean = false
    private var deleteIconTint: Int = Color.WHITE
    private var showLoadingIndicator: Boolean = true
    private var enableFullScreenPreview: Boolean = true
    private var enableCameraCapture: Boolean = false

    // Empty state
    private var emptyStateTextValue: String = "Tap to add image"
    private var emptyStateIconResId: Int = 0

    // State
    private var currentState: ImageState = ImageState.EMPTY
    private var currentImageFile: File? = null
    private var currentImageBitmap: Bitmap? = null
    private var currentImageUri: Uri? = null

    // Listeners
    private var onDeleteClickListener: (() -> Unit)? = null
    private var onImageClickListener: (() -> Unit)? = null
    private var onCaptureClickListener: ((PhotoCaptureConfig) -> Unit)? = null
    private var onImageLoadListener: ((Boolean) -> Unit)? = null
    private var onFullScreenRequestListener: (() -> Unit)? = null

    // Paint for drawing border
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val clipPath = Path()
    private val clipRect = RectF()

    init {
        // Enable drawing for this ViewGroup
        setWillNotDraw(false)

        // Create label
        labelTextView = TextView(context).apply {
            visibility = GONE
            setTextSize(TypedValue.COMPLEX_UNIT_PX, imageLabelTextSize)
            setTextColor(imageLabelTextColor)
        }
        addView(labelTextView)

        // Create image container (for clipping rounded corners)
        imageContainer = FrameLayout(context).apply {
            clipChildren = true
            clipToPadding = true
        }
        addView(imageContainer)

        // Create main image view
        imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = GONE
        }
        imageContainer.addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Create loading indicator
        loadingIndicator = ProgressBar(context).apply {
            visibility = GONE
            isIndeterminate = true
        }
        imageContainer.addView(loadingIndicator, LayoutParams(
            LayoutParams.WRAP_CONTENT, 
            LayoutParams.WRAP_CONTENT, 
            Gravity.CENTER
        ))

        // Create delete button
        deleteButton = ImageView(context).apply {
            visibility = GONE
            setImageResource(android.R.drawable.ic_delete)
            setPadding(dpToPx(8f).toInt(), dpToPx(8f).toInt(), dpToPx(8f).toInt(), dpToPx(8f).toInt())
            setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
            setOnClickListener { 
                onDeleteClickListener?.invoke()
                if (onDeleteClickListener == null) {
                    clearImage()
                }
            }
        }
        val deleteParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        deleteParams.gravity = Gravity.TOP or Gravity.END
        deleteParams.marginEnd = dpToPx(8f).toInt()
        deleteParams.topMargin = dpToPx(8f).toInt()
        imageContainer.addView(deleteButton, deleteParams)

        // Create empty state view
        emptyStateView = FrameLayout(context).apply {
            visibility = VISIBLE
            isClickable = true
            isFocusable = true
            setOnClickListener { handleEmptyStateClick() }
        }
        imageContainer.addView(emptyStateView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Empty state icon
        emptyStateIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            alpha = 0.5f
        }
        val iconParams = LayoutParams(dpToPx(48f).toInt(), dpToPx(48f).toInt(), Gravity.CENTER)
        iconParams.bottomMargin = dpToPx(24f).toInt()
        emptyStateView.addView(emptyStateIcon, iconParams)

        // Empty state text
        emptyStateText = TextView(context).apply {
            text = emptyStateTextValue
            gravity = Gravity.CENTER
            alpha = 0.7f
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        val textParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        textParams.topMargin = dpToPx(24f).toInt()
        emptyStateView.addView(emptyStateText, textParams)

        // Parse XML attributes
        if (attrs != null) {
            parseAttributes(attrs, defStyleAttr)
        }

        // Setup click listener for image preview
        imageView.setOnClickListener {
            if (currentState == ImageState.LOADED && enableFullScreenPreview) {
                onFullScreenRequestListener?.invoke() ?: showFullScreenPreview()
            }
            onImageClickListener?.invoke()
        }

        updateState(ImageState.EMPTY)
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.AdvancedImageView,
            defStyleAttr,
            0
        )

        try {
            // Label
            imageLabel = typedArray.getString(R.styleable.AdvancedImageView_imageLabel) ?: ""
            if (imageLabel.isNotEmpty()) {
                labelTextView.text = imageLabel
                labelTextView.visibility = VISIBLE
            }

            imageLabelGap = typedArray.getDimension(
                R.styleable.AdvancedImageView_imageLabelGap,
                dpToPx(8f)
            )
            imageLabelTextSize = typedArray.getDimension(
                R.styleable.AdvancedImageView_imageLabelTextSize,
                dpToPx(14f)
            )
            imageLabelTextColor = typedArray.getColor(
                R.styleable.AdvancedImageView_imageLabelTextColor,
                0xFF252525.toInt()
            )

            labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, imageLabelTextSize)
            labelTextView.setTextColor(imageLabelTextColor)

            // Image display
            imageScaleType = ImageScaleType.fromValue(
                typedArray.getInt(R.styleable.AdvancedImageView_imageScaleType, ImageScaleType.CROP.value)
            )
            imageAspectRatio = typedArray.getFloat(R.styleable.AdvancedImageView_imageAspectRatio, 0f)
            placeholderResId = typedArray.getResourceId(R.styleable.AdvancedImageView_placeholderImage, 0)
            errorResId = typedArray.getResourceId(R.styleable.AdvancedImageView_errorImage, 0)

            // Visual styling
            imageCornerRadius = typedArray.getDimension(R.styleable.AdvancedImageView_imageCornerRadius, 0f)
            imageBorderWidth = typedArray.getDimension(R.styleable.AdvancedImageView_imageBorderWidth, 0f)
            imageBorderColor = typedArray.getColor(R.styleable.AdvancedImageView_imageBorderColor, Color.TRANSPARENT)
            imageBackgroundColor = typedArray.getColor(R.styleable.AdvancedImageView_imageBackgroundColor, Color.TRANSPARENT)

            imageContainer.setBackgroundColor(imageBackgroundColor)

            // Features
            showDeleteIcon = typedArray.getBoolean(R.styleable.AdvancedImageView_showDeleteIcon, false)
            deleteIconTint = typedArray.getColor(R.styleable.AdvancedImageView_deleteIconTint, Color.WHITE)
            showLoadingIndicator = typedArray.getBoolean(R.styleable.AdvancedImageView_showLoadingIndicator, true)
            enableFullScreenPreview = typedArray.getBoolean(R.styleable.AdvancedImageView_enableFullScreenPreview, true)
            enableCameraCapture = typedArray.getBoolean(R.styleable.AdvancedImageView_enableCameraCapture, false)

            // Empty state
            emptyStateTextValue = typedArray.getString(R.styleable.AdvancedImageView_emptyStateText) 
                ?: if (enableCameraCapture) "Tap to capture" else "No image"
            emptyStateIconResId = typedArray.getResourceId(R.styleable.AdvancedImageView_emptyStateIcon, 0)

            emptyStateText.text = emptyStateTextValue
            if (emptyStateIconResId != 0) {
                emptyStateIcon.setImageResource(emptyStateIconResId)
            }

            // Apply delete icon tint
            DrawableCompat.setTint(deleteButton.drawable, deleteIconTint)

        } finally {
            typedArray.recycle()
        }

        borderPaint.color = imageBorderColor
        borderPaint.strokeWidth = imageBorderWidth
    }

    /**
     * Load image from URL
     */
    fun loadFromUrl(url: String) {
        if (url.isBlank()) {
            updateState(ImageState.EMPTY)
            return
        }

        updateState(ImageState.LOADING)

        val requestBuilder = Glide.with(context)
            .load(url)
            .listener(createGlideListener())

        applyGlideTransformations(requestBuilder)
            .into(imageView)
    }

    /**
     * Load image from local file
     */
    fun loadFromFile(file: File) {
        if (!file.exists()) {
            updateState(ImageState.ERROR)
            onImageLoadListener?.invoke(false)
            return
        }

        currentImageFile = file
        updateState(ImageState.LOADING)

        val requestBuilder = Glide.with(context)
            .load(file)
            .listener(createGlideListener())

        applyGlideTransformations(requestBuilder)
            .into(imageView)
    }

    /**
     * Load image from Bitmap
     */
    fun loadFromBitmap(bitmap: Bitmap) {
        currentImageBitmap = bitmap
        updateState(ImageState.LOADING)

        val requestBuilder = Glide.with(context)
            .load(bitmap)
            .listener(createGlideListener())

        applyGlideTransformations(requestBuilder)
            .into(imageView)
    }

    /**
     * Load image from Base64 encoded string
     */
    fun loadFromBase64(base64String: String) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            
            updateState(ImageState.LOADING)

            val requestBuilder = Glide.with(context)
                .load(decodedBytes)
                .listener(createGlideListener())

            applyGlideTransformations(requestBuilder)
                .into(imageView)
        } catch (e: Exception) {
            updateState(ImageState.ERROR)
            onImageLoadListener?.invoke(false)
        }
    }

    /**
     * Load image from drawable resource
     */
    fun loadFromDrawable(drawableResId: Int) {
        updateState(ImageState.LOADING)

        val requestBuilder = Glide.with(context)
            .load(drawableResId)
            .listener(createGlideListener())

        applyGlideTransformations(requestBuilder)
            .into(imageView)
    }

    /**
     * Load image from content URI
     */
    fun loadFromUri(uri: Uri) {
        currentImageUri = uri
        updateState(ImageState.LOADING)

        val requestBuilder = Glide.with(context)
            .load(uri)
            .listener(createGlideListener())

        applyGlideTransformations(requestBuilder)
            .into(imageView)
    }

    /**
     * Load image from assets folder
     */
    fun loadFromAssets(assetPath: String) {
        updateState(ImageState.LOADING)

        val requestBuilder = Glide.with(context)
            .load("file:///android_asset/$assetPath")
            .listener(createGlideListener())

        applyGlideTransformations(requestBuilder)
            .into(imageView)
    }

    private fun <T> applyGlideTransformations(
        requestBuilder: com.bumptech.glide.RequestBuilder<T>
    ): com.bumptech.glide.RequestBuilder<T> {
        var builder = requestBuilder

        // Apply placeholder and error images
        if (placeholderResId != 0) {
            builder = builder.placeholder(placeholderResId)
        }
        if (errorResId != 0) {
            builder = builder.error(errorResId)
        }

        // Apply scale type transformation
        val scaleTransform = when (imageScaleType) {
            ImageScaleType.FIT -> FitCenter()
            ImageScaleType.FILL, ImageScaleType.CROP -> CenterCrop()
            ImageScaleType.CENTER -> CenterInside()
            ImageScaleType.STRETCH -> FitCenter()
        }

        // Apply corner radius if set
        if (imageCornerRadius > 0) {
            builder = builder.transform(scaleTransform, RoundedCorners(imageCornerRadius.toInt()))
        } else {
            builder = builder.transform(scaleTransform)
        }

        return builder
    }

    private fun createGlideListener(): RequestListener<Drawable> {
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                post {
                    updateState(ImageState.ERROR)
                    onImageLoadListener?.invoke(false)
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                post {
                    updateState(ImageState.LOADED)
                    onImageLoadListener?.invoke(true)
                    
                    // Start GIF animation if applicable
                    if (resource is GifDrawable) {
                        resource.start()
                    }
                }
                return false
            }
        }
    }

    private fun updateState(newState: ImageState) {
        currentState = newState

        when (newState) {
            ImageState.EMPTY -> {
                imageView.visibility = GONE
                loadingIndicator.visibility = GONE
                deleteButton.visibility = GONE
                emptyStateView.visibility = VISIBLE
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
                emptyStateText.text = "Failed to load image"
            }
        }

        invalidate()
    }

    /**
     * Clear the current image and reset to empty state
     */
    fun clearImage() {
        Glide.with(context).clear(imageView)
        imageView.setImageDrawable(null)
        currentImageFile = null
        currentImageBitmap = null
        currentImageUri = null
        emptyStateText.text = emptyStateTextValue
        updateState(ImageState.EMPTY)
    }

    /**
     * Get the current image file (if loaded from file)
     */
    fun getImageFile(): File? = currentImageFile

    /**
     * Get the current image bitmap (if loaded from bitmap)
     */
    fun getImageBitmap(): Bitmap? = currentImageBitmap

    /**
     * Get the current image URI (if loaded from URI)
     */
    fun getImageUri(): Uri? = currentImageUri

    /**
     * Check if an image is currently loaded
     */
    fun hasImage(): Boolean = currentState == ImageState.LOADED

    /**
     * Get current state
     */
    fun getState(): ImageState = currentState

    // Listener setters

    /**
     * Set listener for delete button click
     */
    fun setOnDeleteClickListener(listener: (() -> Unit)?) {
        onDeleteClickListener = listener
    }

    /**
     * Set listener for image click (when image is loaded)
     */
    fun setOnImageClickListener(listener: (() -> Unit)?) {
        onImageClickListener = listener
    }

    /**
     * Set listener for capture click (when empty and camera capture is enabled)
     * The listener receives a PhotoCaptureConfig that can be used with PhotoCaptureManager
     */
    fun setOnCaptureClickListener(listener: ((PhotoCaptureConfig) -> Unit)?) {
        onCaptureClickListener = listener
    }

    /**
     * Set listener for image load completion
     */
    fun setOnImageLoadListener(listener: ((Boolean) -> Unit)?) {
        onImageLoadListener = listener
    }

    /**
     * Set listener for full-screen preview request
     * If not set, default preview will be shown
     */
    fun setOnFullScreenRequestListener(listener: (() -> Unit)?) {
        onFullScreenRequestListener = listener
    }

    // Configuration methods

    /**
     * Set the label text
     */
    fun setLabel(text: String) {
        imageLabel = text
        labelTextView.text = text
        labelTextView.visibility = if (text.isNotEmpty()) VISIBLE else GONE
        requestLayout()
    }

    /**
     * Set corner radius programmatically
     */
    fun setCornerRadius(radius: Float) {
        imageCornerRadius = radius
        invalidate()
        // Reload image to apply new corner radius
        reloadCurrentImage()
    }

    /**
     * Set border properties
     */
    fun setBorder(width: Float, color: Int) {
        imageBorderWidth = width
        imageBorderColor = color
        borderPaint.strokeWidth = width
        borderPaint.color = color
        invalidate()
    }

    /**
     * Set scale type
     */
    fun setImageScaleType(scaleType: ImageScaleType) {
        imageScaleType = scaleType
        reloadCurrentImage()
    }

    /**
     * Enable or disable delete icon
     */
    fun setShowDeleteIcon(show: Boolean) {
        showDeleteIcon = show
        if (currentState == ImageState.LOADED) {
            deleteButton.visibility = if (show) VISIBLE else GONE
        }
    }

    /**
     * Enable or disable camera capture on empty state click
     */
    fun setEnableCameraCapture(enable: Boolean) {
        enableCameraCapture = enable
        emptyStateText.text = if (enable) "Tap to capture" else "No image"
    }

    private fun reloadCurrentImage() {
        when {
            currentImageFile != null -> loadFromFile(currentImageFile!!)
            currentImageBitmap != null -> loadFromBitmap(currentImageBitmap!!)
            currentImageUri != null -> loadFromUri(currentImageUri!!)
        }
    }

    private fun handleEmptyStateClick() {
        if (enableCameraCapture && onCaptureClickListener != null) {
            val config = PhotoCaptureConfig()
            onCaptureClickListener?.invoke(config)
        }
    }

    private fun showFullScreenPreview() {
        // Launch FullScreenImageViewer with current image
        val builder = FullScreenImageViewer.Builder(context)
        
        when {
            currentImageFile != null -> builder.setImageFile(currentImageFile!!)
            currentImageBitmap != null -> builder.setImageBitmap(currentImageBitmap!!)
            currentImageUri != null -> builder.setImageUri(currentImageUri!!)
            else -> return // No image to show
        }
        
        builder.show()
    }

    // Layout methods

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
            totalHeight += labelTextView.measuredHeight + imageLabelGap.toInt()
        }

        // Calculate image container dimensions
        val containerWidth = width - paddingStart - paddingEnd
        val containerHeight = if (imageAspectRatio > 0) {
            (containerWidth / imageAspectRatio).toInt()
        } else {
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize - totalHeight
                MeasureSpec.AT_MOST -> (heightSize - totalHeight).coerceAtMost(containerWidth)
                else -> containerWidth // Default to square
            }
        }

        // Measure image container
        measureChild(
            imageContainer,
            MeasureSpec.makeMeasureSpec(containerWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(containerHeight, MeasureSpec.EXACTLY)
        )

        totalHeight += containerHeight

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
            currentTop += labelTextView.measuredHeight + imageLabelGap.toInt()
        }

        // Layout image container
        imageContainer.layout(
            paddingStart,
            currentTop,
            paddingStart + imageContainer.measuredWidth,
            currentTop + imageContainer.measuredHeight
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw border around image container if configured
        if (imageBorderWidth > 0 && imageBorderColor != Color.TRANSPARENT) {
            val labelHeight = if (labelTextView.isVisible) {
                labelTextView.measuredHeight + imageLabelGap
            } else 0f

            val borderOffset = imageBorderWidth / 2
            clipRect.set(
                paddingStart + borderOffset,
                paddingTop + labelHeight + borderOffset,
                width - paddingEnd - borderOffset,
                height - paddingBottom - borderOffset
            )

            if (imageCornerRadius > 0) {
                canvas.drawRoundRect(clipRect, imageCornerRadius, imageCornerRadius, borderPaint)
            } else {
                canvas.drawRect(clipRect, borderPaint)
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
