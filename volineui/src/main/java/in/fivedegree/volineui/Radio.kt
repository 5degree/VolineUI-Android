@file:Suppress("unused")

package `in`.fivedegree.volineui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import `in`.fivedegree.volineui.radio.RadioOptionsException

/**
 * Modern radio/segmented control component with sliding pill indicator.
 * 
 * Features:
 * - Horizontal layout with multiple selectable options
 * - Animated sliding pill indicator with 3D effect
 * - Dynamic color customization for selected/unselected states
 * - Touch and swipe gesture support
 * - Haptic feedback
 * - Disabled states (global and per-option)
 * - Label support
 * - Programmatic and XML configuration
 * 
 * Requires minimum 2 options, throws RadioOptionsException otherwise.
 */
class Radio @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    // Label
    private val labelTextView: TextView
    private var radioLabel: String = ""
    private var radioLabelGap: Float = dpToPx(5f)
    private var radioLabelTextSize: Float = dpToPx(14f)
    private var radioLabelTextColor: Int = 0xFF252525.toInt()

    // Option data
    private var options: List<String> = emptyList()
    private var selectedIndex: Int = 0
    private val optionViews: MutableList<TextView> = mutableListOf()
    private val disabledOptions: MutableSet<Int> = mutableSetOf()

    // Visual properties
    private var selectedBackgroundColor: Int = 0
    private var unselectedBackgroundColor: Int = 0xFFF5F5F5.toInt()
    private var selectedTextColor: Int = 0xFFFFFFFF.toInt()
    private var unselectedTextColor: Int = 0xFF757575.toInt()
    private var containerBackgroundColor: Int = 0xFFF5F5F5.toInt()
    private var dividerColor: Int = 0x40000000
    
    // Sizing & spacing
    private var componentHeight: Float = dpToPx(48f)
    private var gap: Float = dpToPx(4f)
    private var optionSpacing: Float = dpToPx(4f)
    private var cornerRadius: Float = dpToPx(24f)
    
    // Visual effects
    private var showDividers: Boolean = false
    private var pillElevation: Float = dpToPx(4f)
    private var dividerWidth: Float = dpToPx(1f)
    
    // Behavior
    private var enableHapticFeedback: Boolean = true
    private var animationDuration: Long = 300
    private var enableSwipeGesture: Boolean = false
    private var isComponentEnabled: Boolean = true
    
    // Text appearance
    private var textSize: Float = dpToPx(14f)
    private var fontFamily: Int = 0
    private var selectedTextStyle: Int = 0
    private var unselectedTextStyle: Int = 0
    
    // Animation state
    private var pillAnimatedPosition: Float = 0f
    private var pillAnimator: ValueAnimator? = null
    
    // Paint objects for drawing
    private val containerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pillGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    // Rect objects for drawing
    private val containerRect = RectF()
    private val pillRect = RectF()
    
    // Gesture detector
    private val gestureDetector: GestureDetector
    private var initialTouchX = 0f
    private var isDragging = false
    private var dragStartedOnPill = false
    
    // Value change listener
    private var onValueChangeListener: ((String, Int) -> Unit)? = null

    init {
        setWillNotDraw(false)
        
        // Create label
        labelTextView = TextView(context).apply {
            visibility = GONE
            textSize = 14f
            setTextColor(radioLabelTextColor)
        }
        addView(labelTextView)
        
        // Get primary color from theme
        selectedBackgroundColor = getPrimaryColor()
        
        // Setup gesture detector
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                handleTap(e.x, e.y)
                return true
            }
            
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                if (enableSwipeGesture && isComponentEnabled) {
                    handleSwipe(distanceX)
                }
                return true
            }
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                if (enableSwipeGesture && isComponentEnabled) {
                    if (velocityX < -1000 && selectedIndex < options.size - 1) {
                        setSelectedIndex(selectedIndex + 1, true)
                    } else if (velocityX > 1000 && selectedIndex > 0) {
                        setSelectedIndex(selectedIndex - 1, true)
                    }
                }
                return true
            }
        })
        
        // Parse XML attributes
        if (attrs != null) {
            parseAttributes(attrs, defStyleAttr)
        }
        
        // Setup initial state
        if (options.isNotEmpty()) {
            setupOptions()
        }
    }

    private fun getPrimaryColor(): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        
        // Try to get colorPrimary from theme
        return if (theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            // Fallback to a default blue color
            0xFF2196F3.toInt()
        }
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.Radio,
            defStyleAttr,
            0
        )

        try {
            // Label
            radioLabel = typedArray.getString(R.styleable.Radio_radioLabel) ?: ""
            if (radioLabel.isNotEmpty()) {
                labelTextView.text = radioLabel
                labelTextView.visibility = VISIBLE
            }
            
            radioLabelGap = typedArray.getDimension(
                R.styleable.Radio_radioLabelGap,
                dpToPx(5f)
            )
            radioLabelTextSize = typedArray.getDimension(
                R.styleable.Radio_radioLabelTextSize,
                dpToPx(14f)
            )
            radioLabelTextColor = typedArray.getColor(
                R.styleable.Radio_radioLabelTextColor,
                0xFF252525.toInt()
            )
            
            labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, radioLabelTextSize)
            labelTextView.setTextColor(radioLabelTextColor)
            
            // Options configuration
            val optionsArrayId = typedArray.getResourceId(R.styleable.Radio_options, 0)
            if (optionsArrayId != 0) {
                val optionsArray = resources.getStringArray(optionsArrayId)
                setOptions(optionsArray.toList())
            }
            
            selectedIndex = typedArray.getInt(R.styleable.Radio_selectedIndex, 0)

            // Colors
            selectedBackgroundColor = typedArray.getColor(
                R.styleable.Radio_selectedBackgroundColor,
                getPrimaryColor()
            )
            unselectedBackgroundColor = typedArray.getColor(
                R.styleable.Radio_unselectedBackgroundColor,
                0xFFF5F5F5.toInt()
            )
            selectedTextColor = typedArray.getColor(
                R.styleable.Radio_selectedTextColor,
                0xFFFFFFFF.toInt()
            )
            unselectedTextColor = typedArray.getColor(
                R.styleable.Radio_unselectedTextColor,
                0xFF757575.toInt()
            )
            containerBackgroundColor = typedArray.getColor(
                R.styleable.Radio_containerBackgroundColor,
                unselectedBackgroundColor
            )
            dividerColor = typedArray.getColor(
                R.styleable.Radio_dividerColor,
                0x40000000
            )

            // Sizing & spacing
            componentHeight = typedArray.getDimension(
                R.styleable.Radio_height,
                dpToPx(48f)
            )
            gap = typedArray.getDimension(
                R.styleable.Radio_gap,
                dpToPx(4f)
            )
            optionSpacing = typedArray.getDimension(
                R.styleable.Radio_optionSpacing,
                dpToPx(4f)
            )
            cornerRadius = typedArray.getDimension(
                R.styleable.Radio_cornerRadius,
                dpToPx(24f)
            )

            // Visual effects
            showDividers = typedArray.getBoolean(R.styleable.Radio_showDividers, false)
            pillElevation = typedArray.getDimension(
                R.styleable.Radio_pillElevation,
                dpToPx(4f)
            )
            dividerWidth = typedArray.getDimension(
                R.styleable.Radio_dividerWidth,
                dpToPx(1f)
            )

            // Behavior
            enableHapticFeedback = typedArray.getBoolean(
                R.styleable.Radio_enableHapticFeedback,
                true
            )
            animationDuration = typedArray.getInt(
                R.styleable.Radio_animationDuration,
                300
            ).toLong()
            enableSwipeGesture = typedArray.getBoolean(
                R.styleable.Radio_enableSwipeGesture,
                false
            )
            isComponentEnabled = typedArray.getBoolean(
                R.styleable.Radio_android_enabled,
                true
            )

            // Text appearance
            textSize = typedArray.getDimension(
                R.styleable.Radio_android_textSize,
                dpToPx(14f)
            )
            fontFamily = typedArray.getResourceId(R.styleable.Radio_android_fontFamily, 0)
            
            selectedTextStyle = typedArray.getInt(R.styleable.Radio_selectedTextStyle, 0)
            unselectedTextStyle = typedArray.getInt(R.styleable.Radio_unselectedTextStyle, 0)

        } finally {
            typedArray.recycle()
        }
        
        // Update paint colors
        containerPaint.color = containerBackgroundColor
        dividerPaint.color = dividerColor
    }

    /**
     * Set options programmatically
     * @throws in.fivedegree.volineui.radio.RadioOptionsException if options list has fewer than 2 items
     */
    fun setOptions(options: List<String>) {
        if (options.isEmpty()) {
            throw RadioOptionsException.emptyOptions()
        }
        if (options.size < 2) {
            throw RadioOptionsException.insufficientOptions(options.size)
        }
        
        this.options = options
        
        // Reset state
        selectedIndex = 0
        disabledOptions.clear()
        
        // Setup UI
        setupOptions()
    }

    private fun setupOptions() {
        // Clear existing option views (keep label)
        for (view in optionViews) {
            removeView(view)
        }
        optionViews.clear()
        
        // Create option views
        for ((index, option) in options.withIndex()) {
            val textView = TextView(context).apply {
                text = option
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_PX, this@Radio.textSize)
                
                // Apply font family
                if (fontFamily != 0) {
                    val typeface = ResourcesCompat.getFont(context, fontFamily)
                    setTypeface(typeface)
                }
                
                // Initial color
                setTextColor(
                    if (index == selectedIndex) selectedTextColor
                    else unselectedTextColor
                )
                
                // Apply text style
                applyTextStyle(this, if (index == selectedIndex) selectedTextStyle else unselectedTextStyle)
            }
            
            optionViews.add(textView)
            addView(textView)
        }
        
        // Initialize pill position
        pillAnimatedPosition = selectedIndex.toFloat()
        
        requestLayout()
        invalidate()
    }

    private fun applyTextStyle(textView: TextView, style: Int) {
        val typeface = when (style) {
            1 -> android.graphics.Typeface.DEFAULT_BOLD
            2 -> android.graphics.Typeface.create(textView.typeface, android.graphics.Typeface.ITALIC)
            3 -> android.graphics.Typeface.create(textView.typeface, android.graphics.Typeface.BOLD_ITALIC)
            else -> android.graphics.Typeface.DEFAULT
        }
        textView.typeface = typeface
    }

    private fun handleTap(x: Float, y: Float) {
        if (!isComponentEnabled || options.isEmpty()) return
        
        val labelHeight = if (labelTextView.isVisible) {
            labelTextView.measuredHeight + radioLabelGap.toInt()
        } else 0

        // Check if tap is within container bounds
        if (y < paddingTop + labelHeight || y > paddingTop + labelHeight + componentHeight) {
            return
        }
        
        // Calculate which option was tapped
        val contentWidth = width - paddingStart - paddingEnd - gap * 2
        val availableWidth = contentWidth - (options.size - 1) * optionSpacing
        val optionWidth = availableWidth / options.size
        
        val relativeX = x - paddingStart - gap
        val tappedIndex = (relativeX / (optionWidth + optionSpacing)).toInt()
        
        if (tappedIndex in options.indices && !disabledOptions.contains(tappedIndex)) {
            setSelectedIndex(tappedIndex, true)
        }
    }

    private fun handleSwipe(distanceX: Float) {
        // Swipe left = next option, swipe right = previous option
        if (distanceX > 50 && selectedIndex < options.size - 1) {
            setSelectedIndex(selectedIndex + 1, true)
        } else if (distanceX < -50 && selectedIndex > 0) {
            setSelectedIndex(selectedIndex - 1, true)
        }
    }

    /**
     * Set selected index programmatically
     * @param index Index to select
     * @param animated Whether to animate the transition
     */
    fun setSelectedIndex(index: Int, animated: Boolean = true) {
        if (index !in options.indices || index == selectedIndex || disabledOptions.contains(index)) {
            return
        }
        
        val oldIndex = selectedIndex
        selectedIndex = index
        
        // Haptic feedback
        if (enableHapticFeedback) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        
        // Update text colors and styles
        updateOptionColors(oldIndex, selectedIndex)
        
        // Animate pill
        if (animated) {
            animatePillTo(selectedIndex)
        } else {
            pillAnimatedPosition = selectedIndex.toFloat()
            invalidate()
        }
        
        // Notify listener
        onValueChangeListener?.invoke(options[selectedIndex], selectedIndex)
    }

    private fun updateOptionColors(oldIndex: Int, newIndex: Int) {
        // Animate color transition for old and new selections
        if (oldIndex in optionViews.indices) {
            val oldView = optionViews[oldIndex]
            animateTextColor(oldView, selectedTextColor, unselectedTextColor)
            applyTextStyle(oldView, unselectedTextStyle)
        }
        
        if (newIndex in optionViews.indices) {
            val newView = optionViews[newIndex]
            animateTextColor(newView, unselectedTextColor, selectedTextColor)
            applyTextStyle(newView, selectedTextStyle)
        }
    }

    private fun animateTextColor(textView: TextView, fromColor: Int, toColor: Int) {
        ValueAnimator.ofArgb(fromColor, toColor).apply {
            duration = animationDuration / 2
            addUpdateListener { animator ->
                textView.setTextColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun animatePillTo(targetIndex: Int) {
        pillAnimator?.cancel()
        
        pillAnimator = ValueAnimator.ofFloat(pillAnimatedPosition, targetIndex.toFloat()).apply {
            duration = animationDuration
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animator ->
                pillAnimatedPosition = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /**
     * Get currently selected value
     */
    fun getSelectedValue(): String {
        return if (selectedIndex in options.indices) options[selectedIndex] else ""
    }

    /**
     * Get currently selected index
     */
    fun getSelectedIndex(): Int = selectedIndex

    /**
     * Set value change listener
     */
    fun setOnValueChangeListener(listener: (String, Int) -> Unit) {
        this.onValueChangeListener = listener
    }

    /**
     * Enable/disable entire component
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        isComponentEnabled = enabled
        alpha = if (enabled) 1.0f else 0.6f
        invalidate()
    }

    /**
     * Enable/disable individual option
     */
    fun setOptionEnabled(index: Int, enabled: Boolean) {
        if (index !in options.indices) return
        
        if (enabled) {
            disabledOptions.remove(index)
        } else {
            disabledOptions.add(index)
            // If disabled option is selected, move to next available
            if (index == selectedIndex) {
                val nextIndex = options.indices.firstOrNull { it != index && !disabledOptions.contains(it) }
                if (nextIndex != null) {
                    setSelectedIndex(nextIndex, true)
                }
            }
        }
        
        optionViews.getOrNull(index)?.alpha = if (enabled) 1.0f else 0.4f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isComponentEnabled) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.x
                isDragging = false
                
                // Check if touch started on the pill
                if (enableSwipeGesture) {
                    dragStartedOnPill = isTouchOnPill(event.x, event.y)
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (enableSwipeGesture && dragStartedOnPill) {
                    handleDrag(event.x, event.y)
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && enableSwipeGesture) {
                    // Snap to nearest option
                    val nearestIndex = pillAnimatedPosition.toInt().coerceIn(0, options.size - 1)
                    if (nearestIndex != selectedIndex && !disabledOptions.contains(nearestIndex)) {
                        setSelectedIndex(nearestIndex, true)
                    } else {
                        // Snap back to selected
                        animatePillTo(selectedIndex)
                    }
                    isDragging = false
                    dragStartedOnPill = false
                    return true
                }
                isDragging = false
                dragStartedOnPill = false
            }
        }
        
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
    
    private fun isTouchOnPill(x: Float, y: Float): Boolean {
        val labelHeight = if (labelTextView.isVisible) {
            labelTextView.measuredHeight + radioLabelGap.toInt()
        } else 0
        
        val containerTop = paddingTop + labelHeight
        
        // Check if touch is within container bounds vertically
        if (y < containerTop || y > containerTop + componentHeight) {
            return false
        }
        
        // Calculate pill bounds
        val contentWidth = width - paddingStart - paddingEnd - gap * 2
        val availableWidth = contentWidth - (options.size - 1) * optionSpacing
        val optionWidth = availableWidth / options.size
        
        val pillLeft = paddingStart + gap + selectedIndex * (optionWidth + optionSpacing)
        val pillRight = pillLeft + optionWidth
        
        return x in pillLeft..pillRight
    }
    
    private fun handleDrag(x: Float, y: Float) {
        isDragging = true
        
        val contentWidth = width - paddingStart - paddingEnd - gap * 2
        val availableWidth = contentWidth - (options.size - 1) * optionSpacing
        val optionWidth = availableWidth / options.size
        
        // Calculate which position the drag is at
        val relativeX = (x - paddingStart - gap).coerceIn(0f, availableWidth)
        val dragPosition = (relativeX / (optionWidth + optionSpacing)).coerceIn(0f, (options.size - 1).toFloat())
        
        // Update pill position smoothly
        pillAnimatedPosition = dragPosition
        invalidate()
        
        // Update text colors based on drag position
        updateTextColorsForDrag(dragPosition)
    }
    
    private fun updateTextColorsForDrag(position: Float) {
        // Update colors for all options based on proximity to drag position
        for (i in optionViews.indices) {
            val distance = kotlin.math.abs(position - i)
            val color = if (distance < 0.5f) {
                // Close to this option - use selected color
                val fraction = 1f - distance * 2  // 0.0 to 1.0
                blendColors(unselectedTextColor, selectedTextColor, fraction)
            } else {
                unselectedTextColor
            }
            optionViews[i].setTextColor(color)
        }
    }
    
    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1 - ratio
        
        val r = ((color1 shr 16 and 0xFF) * inverseRatio + (color2 shr 16 and 0xFF) * ratio).toInt()
        val g = ((color1 shr 8 and 0xFF) * inverseRatio + (color2 shr 8 and 0xFF) * ratio).toInt()
        val b = ((color1 and 0xFF) * inverseRatio + (color2 and 0xFF) * ratio).toInt()
        val a = ((color1 shr 24 and 0xFF) * inverseRatio + (color2 shr 24 and 0xFF) * ratio).toInt()
        
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

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
            totalHeight += labelTextView.measuredHeight + radioLabelGap.toInt()
        }
        
        // Add component height
        totalHeight += componentHeight.toInt()
        
        // Calculate available width for options
        val contentWidth = width - paddingStart - paddingEnd - gap * 2
        val availableWidth = contentWidth - (options.size - 1) * optionSpacing
        val optionWidth = if (options.isNotEmpty()) availableWidth / options.size else 0f
        
        // Measure each option view
        for (optionView in optionViews) {
            measureChild(
                optionView,
                MeasureSpec.makeMeasureSpec(optionWidth.toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(componentHeight.toInt(), MeasureSpec.EXACTLY)
            )
        }
        
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
            currentTop += labelTextView.measuredHeight + radioLabelGap.toInt()
        }
        
        if (options.isEmpty()) return
        
        val contentWidth = right - left - paddingStart - paddingEnd - gap * 2
        val availableWidth = contentWidth - (options.size - 1) * optionSpacing
        val optionWidth = availableWidth / options.size
        
        // Layout option views
        var currentLeft = paddingStart + gap
        for (optionView in optionViews) {
            optionView.layout(
                currentLeft.toInt(),
                currentTop,
                (currentLeft + optionWidth).toInt(),
                currentTop + componentHeight.toInt()
            )
            currentLeft += optionWidth + optionSpacing
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (options.isEmpty()) return
        
        val labelHeight = if (labelTextView.isVisible) {
            labelTextView.measuredHeight + radioLabelGap
        } else 0f
        
        val containerTop = paddingTop + labelHeight
        val containerBottom = containerTop + componentHeight
        
        // Draw container background
        containerRect.set(
            paddingStart.toFloat(),
            containerTop,
            width - paddingEnd.toFloat(),
            containerBottom
        )
        canvas.drawRoundRect(containerRect, cornerRadius, cornerRadius, containerPaint)
        
        // Draw dividers between unselected options
        if (showDividers) {
            drawDividers(canvas, containerTop)
        }
        
        // Draw sliding pill with 3D effect
        drawPill(canvas, containerTop)
    }

    private fun drawDividers(canvas: Canvas, containerTop: Float) {
        val contentWidth = width - paddingStart - paddingEnd - gap * 2
        val availableWidth = contentWidth - (options.size - 1) * optionSpacing
        val optionWidth = availableWidth / options.size
        
        var currentX = paddingStart + gap + optionWidth + optionSpacing / 2
        
        for (i in 0 until options.size - 1) {
            // Calculate divider opacity based on pill position
            val leftProgress = 1f - kotlin.math.abs(pillAnimatedPosition - i).coerceIn(0f, 1f)
            val rightProgress = 1f - kotlin.math.abs(pillAnimatedPosition - (i + 1)).coerceIn(0f, 1f)
            val alpha = (1f - kotlin.math.max(leftProgress, rightProgress)).coerceIn(0f, 1f)
            
            if (alpha > 0.01f) {
                dividerPaint.alpha = (alpha * 255).toInt()
                
                val dividerTop = containerTop + componentHeight * 0.25f
                val dividerBottom = containerTop + componentHeight * 0.75f
                
                canvas.drawRect(
                    currentX - dividerWidth / 2,
                    dividerTop,
                    currentX + dividerWidth / 2,
                    dividerBottom,
                    dividerPaint
                )
            }
            
            currentX += optionWidth + optionSpacing
        }
    }

    private fun drawPill(canvas: Canvas, containerTop: Float) {
        val contentWidth = width - paddingStart - paddingEnd - gap * 2
        val availableWidth = contentWidth - (options.size - 1) * optionSpacing
        val optionWidth = availableWidth / options.size
        
        val pillLeft = paddingStart + gap + pillAnimatedPosition * (optionWidth + optionSpacing)
        val pillTop = containerTop + gap
        val pillRight = pillLeft + optionWidth
        val pillBottom = containerTop + componentHeight - gap
        
        pillRect.set(pillLeft, pillTop, pillRight, pillBottom)
        
        // Create 3D gradient effect
        val gradientShader = LinearGradient(
            pillLeft,
            pillTop,
            pillLeft,
            pillBottom,
            intArrayOf(
                lightenColor(selectedBackgroundColor, 0.2f),  // Lighter at top (highlight)
                selectedBackgroundColor,                         // Base color in middle
                darkenColor(selectedBackgroundColor, 0.1f)     // Darker at bottom (shadow)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        pillGradientPaint.shader = gradientShader
        pillGradientPaint.setShadowLayer(pillElevation, 0f, pillElevation / 2, 0x40000000)
        
        // Draw the 3D pill
        canvas.drawRoundRect(pillRect, cornerRadius - gap, cornerRadius - gap, pillGradientPaint)
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xFF) * (1 + factor)).toInt().coerceAtMost(255)
        val g = ((color shr 8 and 0xFF) * (1 + factor)).toInt().coerceAtMost(255)
        val b = ((color and 0xFF) * (1 + factor)).toInt().coerceAtMost(255)
        val a = color shr 24 and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xFF) * (1 - factor)).toInt().coerceAtLeast(0)
        val g = ((color shr 8 and 0xFF) * (1 - factor)).toInt().coerceAtLeast(0)
        val b = ((color and 0xFF) * (1 - factor)).toInt().coerceAtLeast(0)
        val a = color shr 24 and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
