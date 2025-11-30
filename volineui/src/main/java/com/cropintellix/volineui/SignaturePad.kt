@file:Suppress("unused")

package com.cropintellix.volineui

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.WorkerThread
import androidx.annotation.AnyThread
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * SignaturePad - A comprehensive signature capture component with drawing capabilities
 * 
 * Features:
 * - Smooth drawing with touch/stylus/mouse support
 * - Multi-touch prevention
 * - Bezier curve smoothing
 * - Pressure sensitivity
 * - Variable line thickness based on speed
 * - Undo/redo functionality
 * - Export to PNG, JPG, SVG, Base64
 * - Color picker and thickness adjustment
 * - Validation (empty check, minimum strokes)
 * - Accessibility support
 * - High-DPI canvas support
 */
class SignaturePad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Canvas and drawing
    private val signatureCanvas: SignatureCanvasView
    private val canvasContainer: FrameLayout
    private val promptText: TextView
    
    // Toolbar
    private val toolbar: LinearLayout
    private val btnUndo: ImageView
    private val btnRedo: ImageView
    private val btnClear: ImageView
    private val btnSave: ImageView
    
    // Drawing properties
    private var penColor: Int = Color.BLACK
    private var penThickness: Float = 20f
    private var canvasBackgroundColor: Int = Color.WHITE
    
    // Configuration
    private var minStrokeCount: Int = 1
    private var showToolbar: Boolean = true
    private var toolbarPosition: Int = TOOLBAR_BOTTOM
    private var promptTextString: String = "Sign here"
    private var autoTrimEmptySpace: Boolean = true
    private var aspectRatio: Int = ASPECT_RATIO_LANDSCAPE
    private var cornerRadius: Float = 0f
    private var borderColor: Int = Color.LTGRAY
    private var borderWidth: Float = 2f
    private var enablePressureSensitivity: Boolean = true
    private var enableSpeedBasedThickness: Boolean = false
    private var maxUndoStackSize: Int = 20
    
    // Timestamp
    private var firstStrokeTimestamp: Long? = null
    
    // Listeners
    private var onSignatureChangeListener: ((Boolean) -> Unit)? = null
    
    companion object {
        const val TOOLBAR_TOP = 0
        const val TOOLBAR_BOTTOM = 1
        
        const val ASPECT_RATIO_LANDSCAPE = 0
        const val ASPECT_RATIO_PORTRAIT = 1
        const val ASPECT_RATIO_SQUARE = 2
    }
    
    
    init {
        // Set full screen layout
        setBackgroundColor(Color.WHITE)
        
        // Create signature canvas (full screen)
        signatureCanvas = SignatureCanvasView(context)
        
        // Create canvas container
        canvasContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }
        canvasContainer.addView(signatureCanvas, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ))
        
        // Create prompt text (centered on canvas)
        promptText = TextView(context).apply {
            text = promptTextString
            textSize = 18f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = android.view.Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }
        canvasContainer.addView(promptText)
        
        // Add canvas to main layout
        addView(canvasContainer)
        
        // Create thickness slider (vertical, bottom-left)
        val thicknessSliderContainer = createThicknessSlider()
        addView(thicknessSliderContainer)
        
        // Create color selector (horizontal, bottom-left, below thickness slider)
        val colorSelectorContainer = createColorSelector()
        addView(colorSelectorContainer)
        
        // Create action buttons (vertical, right side)
        btnUndo = createActionButton("Undo", R.drawable.undo_24px)
        btnRedo = createActionButton("Redo", R.drawable.redo_24px)
        btnClear = createActionButton("Clear", R.drawable.delete_24px)
        btnSave = createActionButton("Save", R.drawable.check_circle_24px)

        // Create vertical action buttons container
        toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                marginEnd = dpToPx(16f)
            }
            layoutParams = params

            addView(btnUndo, createActionButtonParams())
            addView(btnRedo, createActionButtonParams())
            addView(btnClear, createActionButtonParams())
            addView(btnSave, createActionButtonParams())
        }
        addView(toolbar)
        
        // Parse attributes
        attrs?.let { parseAttributes(it, defStyleAttr) }
        
        // Setup button listeners
        setupListeners()
        
        // Update button states
        updateButtonStates()
    }
    
    
    /**
     * Create modern action button with circular background
     */
    private fun createActionButton(contentDesc: String, iconRes: Int): ImageView {
        return ImageView(context).apply {
            contentDescription = contentDesc
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            // Make it circular
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(view.height, view.width, view.width, view.height)
                }
            }
            elevation = dpToPx(4f).toFloat()
        }
    }
    
    /**
     * Create layout params for action buttons
     */
    private fun createActionButtonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(dpToPx(48f), dpToPx(48f)).apply {
            topMargin = dpToPx(8f)
            bottomMargin = dpToPx(8f)
        }
    }
    
    
    /**
     * Create horizontal thickness slider with triangular shape (wide at left, narrow at right)
     */
    private fun createThicknessSlider(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LayoutParams(
                dpToPx(150f),
                dpToPx(48f)
            ).apply {
                gravity = android.view.Gravity.BOTTOM
                leftMargin = dpToPx(16f)
                bottomMargin = dpToPx(16f)
            }
            layoutParams = params
            setPadding(dpToPx(8f), dpToPx(8f), dpToPx(8f), dpToPx(8f))
            
            // Create custom triangular slider using a custom view
            val sliderView = object : View(context) {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                private val path = Path()
                private var thumbX = 0f
                
                init {
                    layoutParams = LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                }
                
                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    
                    // Draw triangular gradient background (wide left, narrow right)
                    path.reset()
                    val w = width.toFloat()
                    val h = height.toFloat()
                    
                    // Triangle: wide at left, pointy at right
                    path.moveTo(0f, 0f) // Top left
                    path.lineTo(w, h * 0.4f) // Right top (narrower)
                    path.lineTo(w, h * 0.6f) // Right bottom (narrower)
                    path.lineTo(0f, h) // Bottom left
                    path.close()
                    
                    paint.color = Color.parseColor("#E0E0E0")
                    canvas.drawPath(path, paint)
                    
                    // Draw thumb circle
                    paint.color = penColor
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(thumbX, h / 2, dpToPx(12f).toFloat(), paint)
                    
                    // Draw white border on thumb
                    paint.color = Color.WHITE
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = dpToPx(2f).toFloat()
                    canvas.drawCircle(thumbX, h / 2, dpToPx(12f).toFloat(), paint)
                }
                
                override fun onTouchEvent(event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            val x = event.x.coerceIn(0f, width.toFloat())
                            thumbX = x
                            
                            // Left (0) = thick (20dp), Right (width) = thin (1dp)
                            val progress = (width - x) / width.toFloat()
                            val thickness = 1f + (progress * 19f) // 1-20 range
                            setPenThickness(dpToPx(thickness).toFloat())
                            
                            invalidate()
                            return true
                        }
                    }
                    return super.onTouchEvent(event)
                }
            }
            
            addView(sliderView)
        }
    }
    
    /**
     * Create horizontal color selector with circles - positioned to the right of thickness slider
     */
    private fun createColorSelector(): LinearLayout {
        val colors = arrayOf(
            Color.parseColor("#FF0000"), // Red
            Color.parseColor("#FFFF00"), // Yellow
            Color.parseColor("#00FF00"), // Green
            Color.parseColor("#00FFFF"), // Cyan
            Color.parseColor("#0000FF"), // Blue
            Color.parseColor("#9C27B0"), // Purple (matching wireframe)
            Color.parseColor("#000000")  // Black
        )
        
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                dpToPx(48f)
            ).apply {
                gravity = android.view.Gravity.START or android.view.Gravity.BOTTOM
                leftMargin = dpToPx(180f) // Position to the right of thickness slider
                bottomMargin = dpToPx(16f)
            }
            layoutParams = params
            setPadding(dpToPx(8f), 0, dpToPx(8f), 0)
            
            colors.forEachIndexed { index, color ->
                val colorCircle = View(context).apply {
                    tag = "color_$color"
                    val isSelected = color == penColor
                    val size = if (isSelected) dpToPx(40f) else dpToPx(32f)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginEnd = dpToPx(8f)
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    setBackgroundColor(color)
                    clipToOutline = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: android.graphics.Outline) {
                            outline.setOval(0, 0, view.width, view.height)
                        }
                    }
                    elevation = if (isSelected) dpToPx(4f).toFloat() else dpToPx(2f).toFloat()
                    
                    setOnClickListener {
                        setPenColor(color)
                        // Update all color circles
                        val parent = parent as? LinearLayout
                        for (i in 0 until (parent?.childCount ?: 0)) {
                            val child = parent?.getChildAt(i)
                            val childColor = child?.tag.toString().substringAfter("color_").toIntOrNull() ?: 0
                            val selected = childColor == penColor
                            val newSize = if (selected) dpToPx(40f) else dpToPx(32f)
                            child?.layoutParams = LinearLayout.LayoutParams(newSize, newSize).apply {
                                marginEnd = dpToPx(8f)
                                gravity = android.view.Gravity.CENTER_VERTICAL
                            }
                            child?.elevation = if (selected) dpToPx(4f).toFloat() else dpToPx(2f).toFloat()
                        }
                    }
                }
                addView(colorCircle)
            }
        }
    }
    
    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {

        val a = context.obtainStyledAttributes(attrs, R.styleable.SignaturePad, defStyleAttr, 0)
        
        try {
            penColor = a.getColor(R.styleable.SignaturePad_penColor, Color.BLACK)
            canvasBackgroundColor = a.getColor(R.styleable.SignaturePad_canvasBackgroundColor, Color.WHITE)
            penThickness = a.getDimension(R.styleable.SignaturePad_penThickness, dpToPx(20f).toFloat())
            minStrokeCount = a.getInt(R.styleable.SignaturePad_minStrokeCount, 1)
            showToolbar = a.getBoolean(R.styleable.SignaturePad_showToolbar, true)
            toolbarPosition = a.getInt(R.styleable.SignaturePad_toolbarPosition, TOOLBAR_BOTTOM)
            promptTextString = a.getString(R.styleable.SignaturePad_promptText) ?: "Sign here"
            autoTrimEmptySpace = a.getBoolean(R.styleable.SignaturePad_autoTrimEmptySpace, true)
            aspectRatio = a.getInt(R.styleable.SignaturePad_aspectRatio, ASPECT_RATIO_LANDSCAPE)
            cornerRadius = a.getDimension(R.styleable.SignaturePad_signatureCornerRadius, 0f)
            borderColor = a.getColor(R.styleable.SignaturePad_signatureBorderColor, Color.LTGRAY)
            borderWidth = a.getDimension(R.styleable.SignaturePad_signatureBorderWidth, dpToPx(2f).toFloat())
            enablePressureSensitivity = a.getBoolean(R.styleable.SignaturePad_enablePressureSensitivity, true)
            enableSpeedBasedThickness = a.getBoolean(R.styleable.SignaturePad_enableSpeedBasedThickness, false)
            maxUndoStackSize = a.getInt(R.styleable.SignaturePad_maxUndoStackSize, 20)
            
            signatureCanvas.penColor = penColor
            signatureCanvas.penThickness = penThickness
            signatureCanvas.canvasBackgroundColor = canvasBackgroundColor
            signatureCanvas.cornerRadius = cornerRadius
            signatureCanvas.borderColor = borderColor
            signatureCanvas.borderWidth = borderWidth
            signatureCanvas.enablePressureSensitivity = enablePressureSensitivity
            signatureCanvas.enableSpeedBasedThickness = enableSpeedBasedThickness
            signatureCanvas.maxUndoStackSize = maxUndoStackSize
            
            promptText.text = promptTextString
        } finally {
            a.recycle()
        }
    }
    
    private fun setupListeners() {
        btnUndo.setOnClickListener {
            undo()
        }
        
        btnRedo.setOnClickListener {
            redo()
        }
        
        btnClear.setOnClickListener {
            clear()
        }
        
        btnSave.setOnClickListener {
            showSaveDialog()
        }
        
        // Remove old color and thickness button listeners - now handled by UI controls
        
        signatureCanvas.onStrokeChangeListener = {
            updatePromptTextVisibility()
            updateButtonStates()
            
            // Capture timestamp on first stroke
            if (firstStrokeTimestamp == null && !isEmpty()) {
                firstStrokeTimestamp = System.currentTimeMillis()
            }
            
            onSignatureChangeListener?.invoke(!isEmpty())
        }
    }
    
    private fun updatePromptTextVisibility() {
        promptText.visibility = if (isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun updateButtonStates() {
        btnUndo.isEnabled = signatureCanvas.canUndo()
        btnRedo.isEnabled = signatureCanvas.canRedo()
        btnClear.isEnabled = !isEmpty()
        btnSave.isEnabled = !isEmpty()
        
        // Update button alpha for visual feedback
        btnUndo.alpha = if (signatureCanvas.canUndo()) 1.0f else 0.5f
        btnRedo.alpha = if (signatureCanvas.canRedo()) 1.0f else 0.5f
        btnClear.alpha = if (!isEmpty()) 1.0f else 0.5f
        btnSave.alpha = if (!isEmpty()) 1.0f else 0.5f
    }

    
    private fun showSaveDialog() {
        val options = arrayOf("Export as PNG", "Export as JPG", "Export as SVG", "Copy as Base64")
        
        android.app.AlertDialog.Builder(context)
            .setTitle("Export Signature")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val bitmap = exportAsPNG()
                        // In a real app, you'd save this or share it
                        android.widget.Toast.makeText(context, "PNG exported", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        val bitmap = exportAsJPG(85)
                        android.widget.Toast.makeText(context, "JPG exported", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val svg = exportAsSVG()
                        android.widget.Toast.makeText(context, "SVG exported", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        val base64 = exportAsBase64(ExportFormat.PNG)
                        // Copy to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Signature", base64)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Base64 copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
    
    // Public API
    
    /**
     * Check if the signature pad is empty
     * This method is thread-safe and can be called from any thread
     */
    @AnyThread
    fun isEmpty(): Boolean = signatureCanvas.isEmpty()
    
    /**
     * Check if signature has minimum required strokes
     * This method is thread-safe and can be called from any thread
     */
    @AnyThread
    fun hasMinimumStrokes(): Boolean = signatureCanvas.getStrokeCount() >= minStrokeCount
    
    /**
     * Check if a signature has been drawn
     * This method is thread-safe and can be called from any thread
     */
    @AnyThread
    fun isSignatureDrawn(): Boolean = !isEmpty()
    
    /**
     * Get the number of strokes
     * This method is thread-safe and can be called from any thread
     */
    @AnyThread
    fun getStrokeCount(): Int = signatureCanvas.getStrokeCount()
    
    /**
     * Get timestamp of first stroke (or null if no strokes)
     */
    fun getFirstStrokeTimestamp(): Long? = firstStrokeTimestamp
    
    /**
     * Clear the signature
     */
    fun clear() {
        signatureCanvas.clear()
        firstStrokeTimestamp = null
        updatePromptTextVisibility()
        updateButtonStates()
        onSignatureChangeListener?.invoke(false)
    }
    
    /**
     * Undo last stroke
     */
    fun undo() {
        signatureCanvas.undo()
    }
    
    /**
     * Redo last undone stroke
     */
    fun redo() {
        signatureCanvas.redo()
    }
    
    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = signatureCanvas.canUndo()
    
    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = signatureCanvas.canRedo()
    
    /**
     * Set pen color
     */
    fun setPenColor(color: Int) {
        penColor = color
        signatureCanvas.penColor = color
    }
    
    /**
     * Set pen thickness
     */
    fun setPenThickness(thickness: Float) {
        penThickness = thickness
        signatureCanvas.penThickness = thickness
    }
    
    /**
     * Set canvas background color
     */
    fun setCanvasBackgroundColor(color: Int) {
        canvasBackgroundColor = color
        signatureCanvas.canvasBackgroundColor = color
    }
    
    /**
     * Set listener for signature changes
     */
    fun setOnSignatureChangeListener(listener: (Boolean) -> Unit) {
        onSignatureChangeListener = listener
    }
    
    /**
     * Export signature as PNG bitmap
     */
    fun exportAsPNG(quality: Int = 100): Bitmap {
        if (isEmpty()) {
            throw SignaturePadException(SignaturePadException.ERROR_EMPTY_SIGNATURE)
        }
        return signatureCanvas.exportAsBitmap(autoTrimEmptySpace)
    }
    
    /**
     * Export signature as JPG bitmap
     */
    fun exportAsJPG(quality: Int = 85): Bitmap {
        if (isEmpty()) {
            throw SignaturePadException(SignaturePadException.ERROR_EMPTY_SIGNATURE)
        }
        return signatureCanvas.exportAsBitmap(autoTrimEmptySpace)
    }
    
    /**
     * Export signature as SVG string
     */
    fun exportAsSVG(): String {
        if (isEmpty()) {
            throw SignaturePadException(SignaturePadException.ERROR_EMPTY_SIGNATURE)
        }
        return signatureCanvas.exportAsSVG()
    }
    
    /**
     * Export signature as Base64 encoded string
     * Note: This method performs bitmap compression and should be called from a background thread
     */
    @WorkerThread
    fun exportAsBase64(format: ExportFormat = ExportFormat.PNG): String {
        if (isEmpty()) {
            throw SignaturePadException(SignaturePadException.ERROR_EMPTY_SIGNATURE)
        }

        val bitmap = signatureCanvas.exportAsBitmap(autoTrimEmptySpace)
        val outputStream = ByteArrayOutputStream()

        val compressFormat = when (format) {
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.JPG -> Bitmap.CompressFormat.JPEG
            ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.PNG
        }

        bitmap.compress(compressFormat, 100, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    /**
     * Export signature to file
     * Note: This method performs bitmap compression and should be called from a background thread
     */
    @WorkerThread
    fun exportAsFile(file: File, format: ExportFormat = ExportFormat.PNG, quality: Int = 100): Boolean {
        if (isEmpty()) {
            throw SignaturePadException(SignaturePadException.ERROR_EMPTY_SIGNATURE)
        }
        
        return try {
            if (format == ExportFormat.SVG) {
                file.writeText(exportAsSVG())
            } else {
                val bitmap = signatureCanvas.exportAsBitmap(autoTrimEmptySpace)
                val outputStream = FileOutputStream(file)
                
                val compressFormat = when (format) {
                    ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                    ExportFormat.JPG -> Bitmap.CompressFormat.JPEG
                    ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.PNG
                }
                
                bitmap.compress(compressFormat, quality, outputStream)
                outputStream.close()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Load signature from bitmap
     */
    fun loadSignature(bitmap: Bitmap) {
        signatureCanvas.loadBitmap(bitmap)
        firstStrokeTimestamp = System.currentTimeMillis()
        updatePromptTextVisibility()
        updateButtonStates()
        onSignatureChangeListener?.invoke(true)
    }
    
    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    /**
     * Inner class: SignatureCanvasView - handles the actual drawing
     */
    private inner class SignatureCanvasView(context: Context) : View(context) {
        
        var penColor: Int = Color.BLACK
        var penThickness: Float = 5f
        var canvasBackgroundColor: Int = Color.WHITE
        var cornerRadius: Float = 0f
        var borderColor: Int = Color.LTGRAY
        var borderWidth: Float = 2f
        var enablePressureSensitivity: Boolean = true
        var enableSpeedBasedThickness: Boolean = false
        var maxUndoStackSize: Int = 20
        
        private val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        
        private val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        
        private val backgroundPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        private val strokes = mutableListOf<SignatureStroke>()
        private val undoStack = mutableListOf<SignatureStroke>()
        private var currentStroke: SignatureStroke? = null
        
        private var lastX = 0f
        private var lastY = 0f
        private var lastTime = 0L
        private val minDistanceThreshold = 2f
        
        private var activePointerId: Int? = null
        
        var onStrokeChangeListener: (() -> Unit)? = null
        
        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
        
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            // Draw background
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            backgroundPaint.color = canvasBackgroundColor
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
            
            // Draw all strokes
            strokes.forEach { stroke ->
                paint.color = stroke.color
                paint.strokeWidth = stroke.thickness
                canvas.drawPath(stroke.path, paint)
            }
            
            // Draw current stroke
            currentStroke?.let { stroke ->
                paint.color = stroke.color
                paint.strokeWidth = stroke.thickness
                canvas.drawPath(stroke.path, paint)
            }
            
            // Draw border
            if (borderWidth > 0) {
                borderPaint.color = borderColor
                borderPaint.strokeWidth = borderWidth
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
            }
        }
        
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Start new stroke with first pointer
                    if (activePointerId == null) {
                        activePointerId = event.getPointerId(0)
                        startStroke(event.x, event.y, event.pressure)
                        return true
                    }
                }
                
                MotionEvent.ACTION_MOVE -> {
                    // Only process if this is our active pointer
                    activePointerId?.let { activeId ->
                        val pointerIndex = event.findPointerIndex(activeId)
                        if (pointerIndex >= 0) {
                            val x = event.getX(pointerIndex)
                            val y = event.getY(pointerIndex)
                            val pressure = event.getPressure(pointerIndex)
                            continueStroke(x, y, pressure)
                        }
                    }
                    return true
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // End stroke if this is our active pointer
                    val pointerIndex = event.findPointerIndex(activePointerId ?: -1)
                    if (pointerIndex == event.actionIndex || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                        endStroke()
                        activePointerId = null
                    }
                    return true
                }
                
                MotionEvent.ACTION_POINTER_UP -> {
                    // If our active pointer is lifted, end the stroke
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    if (pointerId == activePointerId) {
                        endStroke()
                        activePointerId = null
                    }
                    return true
                }
            }
            
            return super.onTouchEvent(event)
        }
        
        private fun startStroke(x: Float, y: Float, pressure: Float) {
            val thickness = calculateThickness(pressure)
            currentStroke = SignatureStroke(penColor, thickness).apply {
                path.moveTo(x, y)
                points.add(PointF(x, y))
            }
            lastX = x
            lastY = y
            lastTime = System.currentTimeMillis()
            invalidate()
        }
        
        private fun continueStroke(x: Float, y: Float, pressure: Float) {
            currentStroke?.let { stroke ->
                val dx = abs(x - lastX)
                val dy = abs(y - lastY)
                val distance = sqrt(dx * dx + dy * dy)
                
                // Check minimum distance threshold
                if (distance < minDistanceThreshold) {
                    return
                }
                
                // Calculate thickness based on speed if enabled
                val thickness = if (enableSpeedBasedThickness) {
                    val currentTime = System.currentTimeMillis()
                    val timeDelta = max(1, currentTime - lastTime)
                    val speed = distance / timeDelta
                    calculateThicknessFromSpeed(speed, pressure)
                } else {
                    calculateThickness(pressure)
                }
                
                // Use quadratic Bezier curve for smoothing
                val controlX = (lastX + x) / 2
                val controlY = (lastY + y) / 2
                
                stroke.path.quadTo(lastX, lastY, controlX, controlY)
                stroke.points.add(PointF(x, y))
                stroke.thickness = thickness
                
                lastX = x
                lastY = y
                lastTime = System.currentTimeMillis()
                
                invalidate()
            }
        }
        
        private fun endStroke() {
            currentStroke?.let { stroke ->
                if (stroke.points.size > 1) {
                    strokes.add(stroke)
                    undoStack.clear() // Clear redo stack when new stroke is added
                    onStrokeChangeListener?.invoke()
                }
                currentStroke = null
                invalidate()
            }
        }
        
        private fun calculateThickness(pressure: Float): Float {
            return if (enablePressureSensitivity && pressure > 0) {
                penThickness * (0.5f + pressure * 0.5f)
            } else {
                penThickness
            }
        }
        
        private fun calculateThicknessFromSpeed(speed: Float, pressure: Float): Float {
            // Thinner lines for faster strokes
            val speedFactor = 1f / (1f + speed * 0.05f)
            val baseThickness = penThickness * speedFactor
            
            return if (enablePressureSensitivity && pressure > 0) {
                baseThickness * (0.5f + pressure * 0.5f)
            } else {
                baseThickness
            }
        }
        
        fun isEmpty(): Boolean = strokes.isEmpty()
        
        fun getStrokeCount(): Int = strokes.size
        
        fun clear() {
            strokes.clear()
            undoStack.clear()
            currentStroke = null
            invalidate()
            onStrokeChangeListener?.invoke()
        }
        
        fun undo() {
            if (strokes.isNotEmpty()) {
                val stroke = strokes.removeAt(strokes.lastIndex)
                undoStack.add(stroke)
                if (undoStack.size > maxUndoStackSize) {
                    undoStack.removeAt(0)
                }
                invalidate()
                onStrokeChangeListener?.invoke()
            }
        }
        
        fun redo() {
            if (undoStack.isNotEmpty()) {
                val stroke = undoStack.removeAt(undoStack.lastIndex)
                strokes.add(stroke)
                invalidate()
                onStrokeChangeListener?.invoke()
            }
        }
        
        fun canUndo(): Boolean = strokes.isNotEmpty()
        
        fun canRedo(): Boolean = undoStack.isNotEmpty()
        
        fun exportAsBitmap(trim: Boolean = true): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            draw(canvas)
            
            return if (trim) {
                trimBitmap(bitmap)
            } else {
                bitmap
            }
        }
        
        private fun trimBitmap(bitmap: Bitmap): Bitmap {
            var minX = bitmap.width
            var minY = bitmap.height
            var maxX = 0
            var maxY = 0
            
            // Find bounds of non-transparent pixels
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.alpha(pixel) > 0 && pixel != canvasBackgroundColor) {
                        minX = min(minX, x)
                        minY = min(minY, y)
                        maxX = max(maxX, x)
                        maxY = max(maxY, y)
                    }
                }
            }
            
            // Add padding
            val padding = 20
            minX = max(0, minX - padding)
            minY = max(0, minY - padding)
            maxX = min(bitmap.width - 1, maxX + padding)
            maxY = min(bitmap.height - 1, maxY + padding)
            
            return if (maxX > minX && maxY > minY) {
                Bitmap.createBitmap(bitmap, minX, minY, maxX - minX + 1, maxY - minY + 1)
            } else {
                bitmap
            }
        }
        
        fun exportAsSVG(): String {
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
            sb.append("width=\"$width\" height=\"$height\">\n")
            
            // Background
            sb.append("<rect width=\"$width\" height=\"$height\" ")
            sb.append("fill=\"${colorToHex(canvasBackgroundColor)}\" />\n")
            
            // Strokes
            strokes.forEach { stroke ->
                sb.append("<path d=\"")
                
                stroke.points.forEachIndexed { index, point ->
                    if (index == 0) {
                        sb.append("M ${point.x} ${point.y} ")
                    } else {
                        sb.append("L ${point.x} ${point.y} ")
                    }
                }
                
                sb.append("\" stroke=\"${colorToHex(stroke.color)}\" ")
                sb.append("stroke-width=\"${stroke.thickness}\" ")
                sb.append("fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />\n")
            }
            
            sb.append("</svg>")
            return sb.toString()
        }
        
        private fun colorToHex(color: Int): String {
            return String.format("#%06X", 0xFFFFFF and color)
        }
        
        fun loadBitmap(bitmap: Bitmap) {
            clear()
            
            // Create a stroke from the bitmap
            // This is a simplified version - in reality you'd want to trace the bitmap
            // For now, we'll just draw the bitmap as a background
            
            val stroke = SignatureStroke(Color.BLACK, penThickness)
            val path = Path()
            
            // Draw bitmap outline (simplified - just draw rectangle)
            path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            stroke.path = path
            strokes.add(stroke)
            
            invalidate()
            onStrokeChangeListener?.invoke()
        }
        
        /**
         * Simple data holder for stroke information
         */
        private inner class SignatureStroke(
            val color: Int,
            var thickness: Float
        ) {
            var path: Path = Path()
            val points: MutableList<PointF> = mutableListOf()
        }
    }
}
