package com.cropintellix.volineui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.CycleInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Advanced custom input field with extensive features:
 * - Real-time validation
 * - Multiple input types
 * - Visual states (error, success, disabled, loading, etc.)
 * - Animations (focus, shake, etc.)
 * - Optional features (clear icon, character counter, prefix/suffix icons)
 * - Input masking
 * - Configuration persistence
 */
class InputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Child views
    private val labelTextView: TextView
    private val inputEditText: EditText
    private val errorTextView: TextView
    private val counterTextView: TextView
    private var prefixIconView: ImageView? = null
    private var suffixIconView: ImageView? = null
    private var clearIconView: ImageView? = null
    private var passwordToggleView: ImageView? = null
    private var loadingProgressBar: ProgressBar? = null

    // Paint for border
    private val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderRect = RectF()

    // Configuration properties
    private var cornerRadius: Float = dpToPx(8f)
    private var borderColor: Int = 0xFFCCCCCC.toInt()
    private var borderWidth: Float = dpToPx(1f)
    private var focusedBorderColor: Int = 0xFF2196F3.toInt()
    private var focusedBorderWidth: Float = dpToPx(2f)
    private var bgColor: Int = 0xFFFFFFFF.toInt()
    
    // State colors
    private var errorColor: Int = 0xFFE53935.toInt()
    private var successColor: Int = 0xFF43A047.toInt()
    private var disabledColor: Int = 0xFF9E9E9E.toInt()
    private var loadingColor: Int = 0xFF2196F3.toInt()

    // Feature toggles
    private var showClearIcon: Boolean = false
    private var showCharacterCounter: Boolean = false
    private var enableValidation: Boolean = false
    private var isMultiLine: Boolean = false
    private var isReadOnly: Boolean = false

    // Validation
    private var validationType: ValidationType = ValidationType.NONE
    private var customValidationPattern: String = ""

    // Input masking
    private var inputMask: String = ""
    private var maskCharacter: Char = '#'
    private var inputMaskWatcher: InputMask? = null

    // Spacing
    private var labelGap: Float = dpToPx(5f)
    private var iconPadding: Float = dpToPx(12f)
    private var verticalPadding: Float = dpToPx(12f)
    private var horizontalPadding: Float = dpToPx(16f)

    // Current state
    private var currentState: FieldState = FieldState.NORMAL
    private var currentBorderColor: Int = borderColor
    private var currentBorderWidth: Float = borderWidth

    // Max length
    private var maxLength: Int = -1

    // Password visibility
    private var isPasswordVisible: Boolean = false
    private var isPasswordField: Boolean = false
    
    // Custom icon resources
    private var clearIconRes: Int = 0
    private var passwordToggleIconRes: Int = 0
    private var passwordToggleVisibleIconRes: Int = 0

    init {
        setWillNotDraw(false)
        
        // Create label
        labelTextView = TextView(context).apply {
            visibility = View.GONE
            textSize = 14f
            setTextColor(0xFF757575.toInt())
        }
        
        // Create input
        inputEditText = EditText(context).apply {
            background = null
            textSize = 16f
            setTextColor(0xFF212121.toInt())
            setPadding(
                horizontalPadding.toInt(),
                verticalPadding.toInt(),
                horizontalPadding.toInt(),
                verticalPadding.toInt()
            )
        }
        
        // Create error text
        errorTextView = TextView(context).apply {
            visibility = View.GONE
            textSize = 12f
            setTextColor(errorColor)
        }
        
        // Create counter
        counterTextView = TextView(context).apply {
            visibility = View.GONE
            textSize = 12f
            setTextColor(0xFF757575.toInt())
            gravity = Gravity.END
        }

        // Parse attributes
        if (attrs != null) {
            parseAttributes(attrs, defStyleAttr)
        }

        // Add views
        addView(labelTextView)
        addView(inputEditText)
        addView(errorTextView)
        addView(counterTextView)

        // Setup listeners
        setupListeners()
        
        // Apply initial state
        updateState(currentState)
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.InputField,
            defStyleAttr,
            0
        )

        try {
            // Text configuration
            val text = typedArray.getString(R.styleable.InputField_android_text)
            val hint = typedArray.getString(R.styleable.InputField_android_hint)
            val label = typedArray.getString(R.styleable.InputField_label)

            inputEditText.setText(text)
            inputEditText.hint = hint

            if (!label.isNullOrEmpty()) {
                labelTextView.text = label
                labelTextView.visibility = View.VISIBLE
            } else if (!hint.isNullOrEmpty()) {
                labelTextView.text = hint
                labelTextView.visibility = View.VISIBLE
            }

            // Input type
            val androidInputType = typedArray.getInt(
                R.styleable.InputField_android_inputType,
                InputType.TYPE_CLASS_TEXT
            )
            inputEditText.inputType = androidInputType
            
            // Check if password type
            isPasswordField = (androidInputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) ||
                    (androidInputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD != 0) ||
                    (androidInputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0)

            // Max length
            maxLength = typedArray.getInt(R.styleable.InputField_android_maxLength, -1)
            if (maxLength > 0) {
                inputEditText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
            }

            // Visual customization
            cornerRadius = typedArray.getDimension(
                R.styleable.InputField_cornerRadius,
                dpToPx(8f)
            )
            borderColor = typedArray.getColor(
                R.styleable.InputField_borderColor,
                0xFFCCCCCC.toInt()
            )
            borderWidth = typedArray.getDimension(
                R.styleable.InputField_borderWidth,
                dpToPx(1f)
            )
            focusedBorderColor = typedArray.getColor(
                R.styleable.InputField_focusedBorderColor,
                0xFF2196F3.toInt()
            )
            focusedBorderWidth = typedArray.getDimension(
                R.styleable.InputField_focusedBorderWidth,
                dpToPx(2f)
            )
            bgColor = typedArray.getColor(
                R.styleable.InputField_backgroundColor,
                0xFFFFFFFF.toInt()
            )

            // State colors
            errorColor = typedArray.getColor(R.styleable.InputField_errorColor, 0xFFE53935.toInt())
            successColor = typedArray.getColor(R.styleable.InputField_successColor, 0xFF43A047.toInt())
            disabledColor = typedArray.getColor(R.styleable.InputField_disabledColor, 0xFF9E9E9E.toInt())
            loadingColor = typedArray.getColor(R.styleable.InputField_loadingColor, 0xFF2196F3.toInt())

            // Feature toggles
            showClearIcon = typedArray.getBoolean(R.styleable.InputField_showClearIcon, false)
            showCharacterCounter = typedArray.getBoolean(R.styleable.InputField_showCharacterCounter, false)
            enableValidation = typedArray.getBoolean(R.styleable.InputField_enableValidation, false)
            isMultiLine = typedArray.getBoolean(R.styleable.InputField_multiLine, false)
            isReadOnly = typedArray.getBoolean(R.styleable.InputField_readOnly, false)

            val enabled = typedArray.getBoolean(R.styleable.InputField_android_enabled, true)
            setEnabled(enabled)

            // Icons
            val prefixIconRes = typedArray.getResourceId(R.styleable.InputField_prefixIcon, 0)
            if (prefixIconRes != 0) {
                setPrefixIcon(ContextCompat.getDrawable(context, prefixIconRes))
            }

            val suffixIconRes = typedArray.getResourceId(R.styleable.InputField_suffixIcon, 0)
            if (suffixIconRes != 0) {
                setSuffixIcon(ContextCompat.getDrawable(context, suffixIconRes))
            }
            
            // Custom clear and password toggle icons
            clearIconRes = typedArray.getResourceId(R.styleable.InputField_clearIcon, 0)
            passwordToggleIconRes = typedArray.getResourceId(R.styleable.InputField_passwordToggleIcon, 0)
            passwordToggleVisibleIconRes = typedArray.getResourceId(R.styleable.InputField_passwordToggleVisibleIcon, 0)

            // Validation
            val validationTypeInt = typedArray.getInt(R.styleable.InputField_validationType, 0)
            validationType = ValidationType.values()[validationTypeInt]
            customValidationPattern = typedArray.getString(
                R.styleable.InputField_customValidationPattern
            ) ?: ""

            // Input masking
            inputMask = typedArray.getString(R.styleable.InputField_inputMask) ?: ""
            val maskCharStr = typedArray.getString(R.styleable.InputField_maskCharacter)
            if (!maskCharStr.isNullOrEmpty()) {
                maskCharacter = maskCharStr[0]
            }

            // Spacing
            labelGap = typedArray.getDimension(R.styleable.InputField_labelGap, dpToPx(5f))
            iconPadding = typedArray.getDimension(R.styleable.InputField_iconPadding, dpToPx(12f))
            verticalPadding = typedArray.getDimension(R.styleable.InputField_verticalPadding, dpToPx(12f))
            horizontalPadding = typedArray.getDimension(R.styleable.InputField_horizontalPadding, dpToPx(16f))
            
            // Update EditText padding
            inputEditText.setPadding(
                horizontalPadding.toInt(),
                verticalPadding.toInt(),
                horizontalPadding.toInt(),
                verticalPadding.toInt()
            )

            // Text appearance
            val textSize = typedArray.getDimension(R.styleable.InputField_android_textSize, -1f)
            if (textSize > 0) {
                inputEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            }

            val textColor = typedArray.getColor(R.styleable.InputField_android_textColor, -1)
            if (textColor != -1) {
                inputEditText.setTextColor(textColor)
            }

            val labelTextSize = typedArray.getDimension(R.styleable.InputField_labelTextSize, -1f)
            if (labelTextSize > 0) {
                labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize)
            }

            val labelTextColor = typedArray.getColor(R.styleable.InputField_labelTextColor, -1)
            if (labelTextColor != -1) {
                labelTextView.setTextColor(labelTextColor)
            }

            val hintTextColor = typedArray.getColor(R.styleable.InputField_hintTextColor, -1)
            if (hintTextColor != -1) {
                inputEditText.setHintTextColor(hintTextColor)
            }

            val errorTextSize = typedArray.getDimension(R.styleable.InputField_errorTextSize, -1f)
            if (errorTextSize > 0) {
                errorTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, errorTextSize)
            }

            val counterTextSize = typedArray.getDimension(R.styleable.InputField_counterTextSize, -1f)
            if (counterTextSize > 0) {
                counterTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, counterTextSize)
            }

        } finally {
            typedArray.recycle()
        }

        // Apply multi-line
        if (isMultiLine) {
            inputEditText.inputType = inputEditText.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            inputEditText.maxLines = 5
            inputEditText.gravity = Gravity.TOP or Gravity.START
        }

        // Apply read-only
        if (isReadOnly) {
            inputEditText.isFocusable = true
            inputEditText.isFocusableInTouchMode = true
            inputEditText.isClickable = true
            inputEditText.keyListener = null
        }

        // Setup input mask
        if (inputMask.isNotEmpty()) {
            inputMaskWatcher = InputMask(inputMask, maskCharacter)
            inputEditText.addTextChangedListener(inputMaskWatcher)
        }

        // Setup password toggle
        if (isPasswordField && !isPasswordToggleSetup()) {
            setupPasswordToggle()
        }

        // Setup clear icon
        if (showClearIcon && clearIconView == null) {
            setupClearIcon()
        }

        // Setup character counter
        if (showCharacterCounter) {
            counterTextView.visibility = View.VISIBLE
            updateCharacterCounter()
        }

        currentBorderColor = borderColor
        currentBorderWidth = borderWidth
    }

    private fun setupListeners() {
        inputEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Clear error when field receives focus, allowing user to correct
                if (currentState == FieldState.ERROR) {
                    clearError()
                }
                if (currentState != FieldState.DISABLED) {
                    updateState(FieldState.FOCUSED)
                }
            } else if (!hasFocus && currentState == FieldState.FOCUSED) {
                updateState(FieldState.NORMAL)
                // Validate on focus lost if validation is enabled
                if (enableValidation) {
                    validate()
                }
            }
        }

        inputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Update clear icon visibility
                clearIconView?.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                
                // Update character counter
                if (showCharacterCounter) {
                    updateCharacterCounter()
                }

                // Real-time validation if enabled
                if (enableValidation && currentState != FieldState.ERROR) {
                    validate()
                }
            }
        })
    }

    private fun setupClearIcon() {
        clearIconView = ImageView(context).apply {
            setImageDrawable(createClearIconDrawable())
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = if (inputEditText.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            setOnClickListener {
                inputEditText.text?.clear()
            }
        }
        addView(clearIconView)
    }

    private fun setupPasswordToggle() {
        passwordToggleView = ImageView(context).apply {
            setImageDrawable(createPasswordVisibilityDrawable(false))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                updatePasswordVisibility()
            }
        }
        addView(passwordToggleView)
    }

    private fun isPasswordToggleSetup(): Boolean {
        return passwordToggleView != null
    }

    private fun updatePasswordVisibility() {
        if (isPasswordVisible) {
            inputEditText.transformationMethod = null
            passwordToggleView?.setImageDrawable(createPasswordVisibilityDrawable(true))
        } else {
            inputEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            passwordToggleView?.setImageDrawable(createPasswordVisibilityDrawable(false))
        }
        // Move cursor to end
        inputEditText.setSelection(inputEditText.text?.length ?: 0)
    }

    private fun updateCharacterCounter() {
        val current = inputEditText.text?.length ?: 0
        counterTextView.text = if (maxLength > 0) {
            "$current / $maxLength"
        } else {
            "$current"
        }
    }

    private fun validate(): Boolean {
        val text = inputEditText.text?.toString() ?: ""
        
        val isValid = when (validationType) {
            ValidationType.EMAIL -> InputValidator.isValidEmail(text)
            ValidationType.PHONE -> InputValidator.isValidPhone(text)
            ValidationType.URL -> InputValidator.isValidUrl(text)
            ValidationType.CUSTOM -> {
                if (customValidationPattern.isNotEmpty()) {
                    InputValidator.isValidCustomPattern(text, customValidationPattern)
                } else {
                    true
                }
            }
            ValidationType.NONE -> true
        }

        if (!isValid && text.isNotEmpty()) {
            // Don't show error automatically, just return false
            return false
        } else if (isValid && text.isNotEmpty() && currentState == FieldState.ERROR) {
            // Clear error state if validation passes
            clearError()
            updateState(FieldState.SUCCESS)
        }

        return isValid
    }

    /**
     * Show error message with shake animation
     */
    fun showError(errorMessage: String) {
        errorTextView.text = errorMessage
        errorTextView.visibility = View.VISIBLE
        updateState(FieldState.ERROR)
        shakeAnimation()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        errorTextView.text = ""
        errorTextView.visibility = View.GONE
        if (currentState == FieldState.ERROR) {
            updateState(FieldState.NORMAL)
        }
    }

    /**
     * Set loading state
     */
    fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            if (loadingProgressBar == null) {
                loadingProgressBar = ProgressBar(context).apply {
                    indeterminateDrawable.setTint(loadingColor)
                }
                addView(loadingProgressBar)
            }
            loadingProgressBar?.visibility = View.VISIBLE
            updateState(FieldState.LOADING)
            inputEditText.isEnabled = false
        } else {
            loadingProgressBar?.visibility = View.GONE
            inputEditText.isEnabled = true
            updateState(FieldState.NORMAL)
        }
    }

    /**
     * Set success state
     */
    fun setSuccess() {
        clearError()
        updateState(FieldState.SUCCESS)
    }

    /**
     * Text property for convenient access
     */
    var text: String
        @JvmName("getTextValue")
        get() = inputEditText.text?.toString() ?: ""
        @JvmName("setTextValue")
        set(value) {
            inputEditText.setText(value)
        }
    
    /**
     * Get current text
     * For Java compatibility - Kotlin users should use text property
     */
    fun getText(): String {
        return inputEditText.text?.toString() ?: ""
    }

    /**
     * Set text programmatically
     * For Java compatibility - Kotlin users should use text property
     */
    fun setText(newText: String) {
        inputEditText.setText(newText)
    }

    /**
     * Set prefix icon
     */
    fun setPrefixIcon(drawable: Drawable?) {
        if (drawable != null) {
            if (prefixIconView == null) {
                prefixIconView = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                addView(prefixIconView)
            }
            prefixIconView?.setImageDrawable(drawable)
            prefixIconView?.visibility = View.VISIBLE
            requestLayout()
        } else {
            prefixIconView?.visibility = View.GONE
        }
    }

    /**
     * Set suffix icon
     */
    fun setSuffixIcon(drawable: Drawable?) {
        if (drawable != null) {
            if (suffixIconView == null) {
                suffixIconView = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                addView(suffixIconView)
            }
            suffixIconView?.setImageDrawable(drawable)
            suffixIconView?.visibility = View.VISIBLE
            requestLayout()
        } else {
            suffixIconView?.visibility = View.GONE
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        inputEditText.isEnabled = enabled
        if (!enabled) {
            updateState(FieldState.DISABLED)
        } else if (currentState == FieldState.DISABLED) {
            updateState(FieldState.NORMAL)
        }
    }

    private fun updateState(newState: FieldState) {
        currentState = newState

        val targetColor = when (newState) {
            FieldState.ERROR -> errorColor
            FieldState.SUCCESS -> successColor
            FieldState.DISABLED -> disabledColor
            FieldState.LOADING -> loadingColor
            FieldState.FOCUSED -> focusedBorderColor
            else -> borderColor
        }

        val targetWidth = when (newState) {
            FieldState.FOCUSED -> focusedBorderWidth
            else -> borderWidth
        }

        animateBorder(targetColor, targetWidth)
    }

    private fun animateBorder(targetColor: Int, targetWidth: Float) {
        // Color animation
        val colorAnimator = ValueAnimator.ofArgb(currentBorderColor, targetColor)
        colorAnimator.duration = 200
        colorAnimator.addUpdateListener { animator ->
            currentBorderColor = animator.animatedValue as Int
            invalidate()
        }
        colorAnimator.start()

        // Width animation
        val widthAnimator = ValueAnimator.ofFloat(currentBorderWidth, targetWidth)
        widthAnimator.duration = 200
        widthAnimator.addUpdateListener { animator ->
            currentBorderWidth = animator.animatedValue as Float
            invalidate()
        }
        widthAnimator.start()
    }

    private fun shakeAnimation() {
        val animator = ObjectAnimator.ofFloat(this, "translationX", 0f, 20f, 0f)
        animator.duration = 300
        animator.interpolator = CycleInterpolator(3f)
        animator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        var totalHeight = 0

        // Measure label
        if (labelTextView.visibility == View.VISIBLE) {
            measureChild(
                labelTextView,
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            totalHeight += labelTextView.measuredHeight + labelGap.toInt()
        }

        // Measure input (with icons)
        val iconSize = dpToPx(24f).toInt()
        var inputWidth = width
        
        if (prefixIconView?.visibility == View.VISIBLE) {
            inputWidth -= iconSize + iconPadding.toInt()
        }
        
        val rightIconsCount = listOfNotNull(
            suffixIconView?.takeIf { it.visibility == View.VISIBLE },
            clearIconView?.takeIf { it.visibility == View.VISIBLE },
            passwordToggleView?.takeIf { it.visibility == View.VISIBLE },
            loadingProgressBar?.takeIf { it.visibility == View.VISIBLE }
        ).size
        
        inputWidth -= rightIconsCount * (iconSize + iconPadding.toInt())

        measureChild(
            inputEditText,
            MeasureSpec.makeMeasureSpec(inputWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        
        val inputHeight = inputEditText.measuredHeight.coerceAtLeast(dpToPx(48f).toInt())
        totalHeight += inputHeight

        // Measure error text
        if (errorTextView.visibility == View.VISIBLE) {
            measureChild(
                errorTextView,
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            totalHeight += errorTextView.measuredHeight + dpToPx(4f).toInt()
        }

        // Measure counter
        if (counterTextView.visibility == View.VISIBLE) {
            measureChild(
                counterTextView,
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            totalHeight += counterTextView.measuredHeight + dpToPx(4f).toInt()
        }

        setMeasuredDimension(width, totalHeight + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var currentTop = paddingTop
        val contentWidth = right - left - paddingStart - paddingEnd

        // Layout label
        if (labelTextView.visibility == View.VISIBLE) {
            labelTextView.layout(
                paddingStart,
                currentTop,
                paddingStart + contentWidth,
                currentTop + labelTextView.measuredHeight
            )
            currentTop += labelTextView.measuredHeight + labelGap.toInt()
        }

        // Layout input field and icons
        val iconSize = dpToPx(24f).toInt()
        val inputTop = currentTop
        val inputHeight = inputEditText.measuredHeight.coerceAtLeast(dpToPx(48f).toInt())
        
        var leftOffset = paddingStart
        var rightOffset = right - left - paddingEnd

        // Layout prefix icon with proper spacing
        if (prefixIconView?.visibility == View.VISIBLE) {
            val iconTop = inputTop + (inputHeight - iconSize) / 2
            val iconMargin = dpToPx(8f).toInt()
            prefixIconView?.layout(
                leftOffset + iconMargin,
                iconTop,
                leftOffset + iconMargin + iconSize,
                iconTop + iconSize
            )
            leftOffset += iconSize + iconMargin * 2
        }

        // Layout right icons (from right to left) with proper spacing
        val rightIcons = mutableListOf<View>()
        loadingProgressBar?.takeIf { it.visibility == View.VISIBLE }?.let { rightIcons.add(it) }
        passwordToggleView?.takeIf { it.visibility == View.VISIBLE }?.let { rightIcons.add(it) }
        clearIconView?.takeIf { it.visibility == View.VISIBLE }?.let { rightIcons.add(it) }
        suffixIconView?.takeIf { it.visibility == View.VISIBLE }?.let { rightIcons.add(it) }

        val iconMargin = dpToPx(8f).toInt()
        for (icon in rightIcons) {
            val iconTop = inputTop + (inputHeight - iconSize) / 2
            icon.layout(
                rightOffset - iconSize - iconMargin,
                iconTop,
                rightOffset - iconMargin,
                iconTop + iconSize
            )
            rightOffset -= iconSize + iconMargin * 2
        }

        // Layout input
        inputEditText.layout(
            leftOffset,
            inputTop,
            rightOffset,
            inputTop + inputHeight
        )
        currentTop += inputHeight

        // Layout error text
        if (errorTextView.visibility == View.VISIBLE) {
            currentTop += dpToPx(4f).toInt()
            errorTextView.layout(
                paddingStart,
                currentTop,
                paddingStart + contentWidth,
                currentTop + errorTextView.measuredHeight
            )
            currentTop += errorTextView.measuredHeight
        }

        // Layout counter
        if (counterTextView.visibility == View.VISIBLE) {
            currentTop += dpToPx(4f).toInt()
            counterTextView.layout(
                paddingStart,
                currentTop,
                paddingStart + contentWidth,
                currentTop + counterTextView.measuredHeight
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        // Calculate input field bounds
        val inputTop = if (labelTextView.visibility == View.VISIBLE) {
            labelTextView.bottom.toFloat() + labelGap
        } else {
            paddingTop.toFloat()
        }
        
        val inputHeight = inputEditText.measuredHeight.coerceAtLeast(dpToPx(48f).toInt())

        // Set border rect with proper inset for stroke width
        // The stroke is drawn centered on the path, so we need to inset by half the stroke width
        val halfStroke = currentBorderWidth / 2f
        borderRect.set(
            paddingStart.toFloat() + halfStroke,
            inputTop + halfStroke,
            (width - paddingEnd).toFloat() - halfStroke,
            inputTop + inputHeight - halfStroke
        )

        // Draw background first
        backgroundPaint.color = bgColor
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, backgroundPaint)

        // Draw border on top with proper stroke width
        borderPaint.color = currentBorderColor
        borderPaint.strokeWidth = currentBorderWidth
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint)
        
        // Draw children after border
        super.onDraw(canvas)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.text = inputEditText.text?.toString() ?: ""
        savedState.errorText = errorTextView.text?.toString() ?: ""
        savedState.currentState = currentState.ordinal
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            inputEditText.setText(state.text)
            if (state.errorText.isNotEmpty()) {
                showError(state.errorText)
            }
            updateState(FieldState.values()[state.currentState])
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        var text: String = ""
        var errorText: String = ""
        var currentState: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            text = parcel.readString() ?: ""
            errorText = parcel.readString() ?: ""
            currentState = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(text)
            out.writeString(errorText)
            out.writeInt(currentState)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    private fun createClearIconDrawable(): Drawable {
        // Use custom icon if provided, otherwise use Material Design icon
        return if (clearIconRes != 0) {
            ContextCompat.getDrawable(context, clearIconRes)!!
        } else {
            // Use Material Design clear icon
            ContextCompat.getDrawable(context, R.drawable.ic_clear)!!
        }
    }

    private fun createPasswordVisibilityDrawable(isVisible: Boolean): Drawable {
        // Use custom icons if provided
        return if (isVisible) {
            if (passwordToggleVisibleIconRes != 0) {
                ContextCompat.getDrawable(context, passwordToggleVisibleIconRes)!!
            } else if (passwordToggleIconRes != 0) {
                ContextCompat.getDrawable(context, passwordToggleIconRes)!!
            } else {
                // Use Material Design visibility icon (eye open)
                ContextCompat.getDrawable(context, R.drawable.ic_visibility)!!
            }
        } else {
            if (passwordToggleIconRes != 0) {
                ContextCompat.getDrawable(context, passwordToggleIconRes)!!
            } else {
                // Use Material Design visibility_off icon (eye closed)
                ContextCompat.getDrawable(context, R.drawable.ic_visibility_off)!!
            }
        }
    }
}
