@file:Suppress("unused")

package `in`.fivedegree.volineui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.core.view.ViewCompat
import `in`.fivedegree.volineui.ruler.RulerHapticMode
import `in`.fivedegree.volineui.ruler.RulerIndicatorStyle
import `in`.fivedegree.volineui.ruler.RulerLabelPosition
import `in`.fivedegree.volineui.ruler.RulerOrientation
import `in`.fivedegree.volineui.ruler.RulerScale
import `in`.fivedegree.volineui.ruler.RulerTickShape
import `in`.fivedegree.volineui.ruler.RulerValueDisplayMode
import `in`.fivedegree.volineui.ruler.RulerValuePickerDefaults
import `in`.fivedegree.volineui.ruler.RulerValuePickerException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Horizontal or vertical ruler that the user drags to select a numeric value.
 *
 * Features:
 * - Horizontal and vertical orientation, reversed scales, and RTL support
 * - Minor/major ticks with automatic label spacing
 * - Optional center indicator (line, triangle, bubble) and value bubble/header
 * - Snapping, fling physics, tap-to-seek, and programmatic animation
 * - Decimal values with configurable formatting and units
 * - Haptic feedback on steps or major steps
 * - Keyboard, D-pad, and accessibility support (range info, set progress)
 * - Disabled / read-only states, fading edges, and extensive styling
 * - State restoration across configuration changes
 *
 * @see RulerValuePickerDefaults
 * @see `in.fivedegree.volineui.compose.RulerValuePicker`
 */
class RulerValuePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    // ── Scale ──────────────────────────────────────────────────────────

    private var _minValue: Double = 0.0
    private var _maxValue: Double = 100.0
    private var _step: Double = 1.0
    private var _majorStep: Double = 10.0
    private var _decimals: Int = RulerValuePickerDefaults.AutoDecimals

    /** Inclusive minimum value of the scale. */
    var minValue: Double
        get() = _minValue
        set(value) {
            _minValue = value
            rebuildScale()
        }

    /** Maximum value of the scale. */
    var maxValue: Double
        get() = _maxValue
        set(value) {
            _maxValue = value
            rebuildScale()
        }

    /** Distance between two minor ticks. */
    var step: Double
        get() = _step
        set(value) {
            _step = value
            rebuildScale()
        }

    /** Distance between two labeled major ticks. Must be a multiple of [step]. */
    var majorStep: Double
        get() = _majorStep
        set(value) {
            _majorStep = value
            rebuildScale()
        }

    /** Decimal places used for display, or -1 to derive them from [step]. */
    var decimals: Int
        get() = _decimals
        set(value) {
            _decimals = value
            rebuildScale()
        }

    private lateinit var scale: RulerScale

    private var currentValue: Double = 0.0
    private var scrollPx: Double = 0.0
    private var maxScrollPx: Double = 0.0

    /** Optional suffix appended to the selected value, for example "kg". */
    var unit: String? = null
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Overrides formatting of the selected value. When null, the value is
     * formatted using [decimals] and [unit].
     */
    var valueFormatter: ((Double) -> String)? = null
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Overrides formatting of major tick labels. When null, labels are
     * formatted using [decimals].
     */
    var tickLabelFormatter: ((Double) -> String)? = null
        set(value) {
            field = value
            invalidate()
        }

    /** Current selected value. Assigning a value snaps it when enabled. */
    var value: Double
        get() = currentValue
        set(newValue) = setValue(newValue, animate = false)

    /** Current value formatted for display, including the unit when present. */
    val formattedValue: String
        get() = formatValue(currentValue)

    // ── Layout & behavior ──────────────────────────────────────────────

    /** Orientation of the ruler. */
    var orientation: RulerOrientation = RulerOrientation.HORIZONTAL
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
                invalidate()
            }
        }

    /** When true the scale values increase from end to start. */
    var reversed: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /** When true values are rounded to [step] while dragging. */
    var snapToStep: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                rebuildScale()
            }
        }

    /** When true the user cannot change the value. */
    var readOnly: Boolean = false

    /** Enables fling physics after a drag. */
    var enableFling: Boolean = true

    /** Enables haptic feedback while crossing steps. */
    var enableHaptics: Boolean = true

    /** Haptic feedback policy. */
    var hapticMode: RulerHapticMode = RulerHapticMode.STEP
        set(value) {
            field = value
            invalidate()
        }

    /** Duration of snap/programmatic animations, in milliseconds. */
    var animationDuration: Int = RulerValuePickerDefaults.AnimationDuration

    /** Minimum time between two haptic ticks, in milliseconds. */
    var hapticThrottleMs: Long = RulerValuePickerDefaults.HapticThrottleMs

    // ── Tick appearance ────────────────────────────────────────────────

    var showMinorTicks: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var showMajorTicks: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var tickShape: RulerTickShape = RulerTickShape.LINE
        set(value) {
            field = value
            invalidate()
        }

    var showLabels: Boolean = true
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var labelPosition: RulerLabelPosition = RulerLabelPosition.ABOVE
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var showSelectedLabel: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var showSelectedTickHighlight: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var showBaseline: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var showCenterIndicator: Boolean = true
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var indicatorStyle: RulerIndicatorStyle = RulerIndicatorStyle.LINE
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var showValue: Boolean = true
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var valueDisplayMode: RulerValueDisplayMode = RulerValueDisplayMode.BUBBLE
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var showFadingEdges: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    // ── Dimensions (pixels) ────────────────────────────────────────────

    var tickSpacing: Float = dp(RulerValuePickerDefaults.TickSpacing.value)
        set(value) {
            val newValue = value.coerceAtLeast(1f)
            if (field != newValue) {
                field = newValue
                rebuildScale()
            }
        }

    var minLabelSpacing: Float = dp(RulerValuePickerDefaults.MinLabelSpacing.value)
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

    var fadeWidth: Float = dp(RulerValuePickerDefaults.FadeWidth.value)
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

    var minorTickWidth: Float = dp(RulerValuePickerDefaults.MinorTickWidth.value)
        set(value) {
            field = value
            invalidate()
        }

    var minorTickHeight: Float = dp(RulerValuePickerDefaults.MinorTickHeight.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var majorTickWidth: Float = dp(RulerValuePickerDefaults.MajorTickWidth.value)
        set(value) {
            field = value
            invalidate()
        }

    var majorTickHeight: Float = dp(RulerValuePickerDefaults.MajorTickHeight.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var selectedTickWidth: Float = dp(RulerValuePickerDefaults.SelectedTickWidth.value)
        set(value) {
            field = value
            invalidate()
        }

    var selectedTickHeight: Float = dp(RulerValuePickerDefaults.SelectedTickHeight.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var tickCornerRadius: Float = dp(RulerValuePickerDefaults.TickCornerRadius.value)
        set(value) {
            field = value
            invalidate()
        }

    var dotRadius: Float = dp(RulerValuePickerDefaults.DotRadius.value)
        set(value) {
            field = value
            invalidate()
        }

    var labelTextSize: Float = sp(RulerValuePickerDefaults.LabelTextSize.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var labelGap: Float = dp(RulerValuePickerDefaults.LabelGap.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var alternateLabelGap: Float = dp(RulerValuePickerDefaults.AlternateLabelGap.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var valueTextSize: Float = sp(RulerValuePickerDefaults.ValueTextSize.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var headerTextSize: Float = sp(RulerValuePickerDefaults.HeaderTextSize.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var headerGap: Float = dp(RulerValuePickerDefaults.HeaderGap.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var indicatorWidth: Float = dp(RulerValuePickerDefaults.IndicatorWidth.value)
        set(value) {
            field = value
            invalidate()
        }

    var indicatorHeight: Float = dp(RulerValuePickerDefaults.IndicatorHeight.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var triangleWidth: Float = dp(RulerValuePickerDefaults.TriangleWidth.value)
        set(value) {
            field = value
            invalidate()
        }

    var triangleHeight: Float = dp(RulerValuePickerDefaults.TriangleHeight.value)
        set(value) {
            field = value
            invalidate()
        }

    var bubbleCornerRadius: Float = dp(RulerValuePickerDefaults.BubbleCornerRadius.value)
        set(value) {
            field = value
            invalidate()
        }

    var bubbleHorizontalPadding: Float = dp(RulerValuePickerDefaults.BubbleHorizontalPadding.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var bubbleVerticalPadding: Float = dp(RulerValuePickerDefaults.BubbleVerticalPadding.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var bubbleGap: Float = dp(RulerValuePickerDefaults.BubbleGap.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var baselineWidth: Float = dp(RulerValuePickerDefaults.BaselineWidth.value)
        set(value) {
            field = value
            invalidate()
        }

    var minimumLength: Float = dp(RulerValuePickerDefaults.MinimumLength.value)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    // ── Colors ─────────────────────────────────────────────────────────

    var tickColor: Int = 0xFFBDBDBD.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var majorTickColor: Int = 0xFF9E9E9E.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var selectedTickColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var labelColor: Int = 0xFF757575.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var selectedLabelColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var indicatorColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var baselineColor: Int = 0xFFE0E0E0.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var bubbleColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var bubbleTextColor: Int = 0xFFFFFFFF.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var headerTextColor: Int = 0xFF212121.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var containerColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    /** Alpha multiplier applied to all colors when the view is disabled. */
    var disabledAlpha: Float = RulerValuePickerDefaults.DisabledAlpha
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    // ── Listeners ──────────────────────────────────────────────────────

    private var onValueChangeListener: ((Double) -> Unit)? = null
    private var onValueChangeFinishedListener: ((Double) -> Unit)? = null
    private var onDragStartListener: (() -> Unit)? = null
    private var onDragEndListener: (() -> Unit)? = null

    /** Called every time [value] changes, including during animations. */
    fun setOnValueChangeListener(listener: ((Double) -> Unit)?) {
        onValueChangeListener = listener
    }

    /** Called when a gesture or animation settles on a final value. */
    fun setOnValueChangeFinishedListener(listener: ((Double) -> Unit)?) {
        onValueChangeFinishedListener = listener
    }

    /** Called when the user starts dragging the ruler. */
    fun setOnDragStartListener(listener: (() -> Unit)?) {
        onDragStartListener = listener
    }

    /** Called when the user lifts their finger from the ruler. */
    fun setOnDragEndListener(listener: (() -> Unit)?) {
        onDragEndListener = listener
    }

    // ── Internal state ─────────────────────────────────────────────────

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val scroller = Scroller(context)
    private var scrollAnimator: ValueAnimator? = null
    private var velocityTracker: android.view.VelocityTracker? = null

    private var isDragging = false
    private var isFlinging = false
    private var downCoord = 0f
    private var lastCoord = 0f
    private var totalDrag = 0f
    private var dragNotified = false
    private var lastHapticStep = Long.MIN_VALUE
    private var lastHapticTime = 0L

    private val containerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tickFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bubbleRect = RectF()
    private val bubblePath = Path()
    private val fadeXfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

    private val flingRunnable = object : Runnable {
        override fun run() {
            if (scroller.computeScrollOffset()) {
                applyScroll(scroller.currX.toDouble(), notify = true)
                postOnAnimation(this)
            } else {
                isFlinging = false
                settleAfterFling()
            }
        }
    }

    init {
        setWillNotDraw(false)
        isFocusable = true
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES

        val primary = getPrimaryColor()
        selectedTickColor = primary
        selectedLabelColor = primary
        indicatorColor = primary
        bubbleColor = primary

        if (contentDescription == null) {
            contentDescription = "Ruler value picker"
        }

        if (attrs != null) {
            parseAttributes(attrs, defStyleAttr)
        }
        rebuildScale()
        updateAccessibilityState()
    }

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Updates the entire scale at once. Prefer this over assigning [minValue],
     * [maxValue], [step], [majorStep], and [decimals] individually to avoid
     * transient invalid configurations.
     *
     * @throws RulerValuePickerException when the configuration is invalid
     */
    fun setScale(
        minValue: Double,
        maxValue: Double,
        step: Double = _step,
        majorStep: Double = _majorStep,
        decimals: Int = _decimals,
    ) {
        _minValue = minValue
        _maxValue = maxValue
        _step = step
        _majorStep = majorStep
        _decimals = decimals
        rebuildScale()
    }

    /**
     * Sets the selected value.
     *
     * @param value target value, coerced/snapped to the scale
     * @param animate whether to animate to the value
     */
    fun setValue(value: Double, animate: Boolean = false) {
        val target = if (snapToStep) scale.snap(value) else scale.coerce(value)
        val targetScroll = scrollForValue(target)
        if (animate) {
            animateScrollTo(targetScroll, notifyFinished = true)
        } else {
            cancelAnimations()
            applyScroll(targetScroll, notify = false)
            updateValue(target)
            invalidate()
        }
    }

    /** Snaps the current value to the nearest step. */
    fun snap() = setValue(scale.snap(currentValue), animate = true)

    /** Moves the selection by [steps] discrete steps. */
    fun scrollBy(steps: Long) {
        val target = scale.valueForStep(scale.stepIndexFor(currentValue) + steps)
        setValue(target, animate = true)
    }

    /** Stops any running snap, fling, or programmatic animation. */
    fun stopAnimation() {
        cancelAnimations()
    }

    // ── Scale management ───────────────────────────────────────────────

    private fun rebuildScale() {
        scale = RulerScale(_minValue, _maxValue, _step, _majorStep, _decimals)
        if (tickSpacing <= 0f) {
            throw RulerValuePickerException.invalidTickSpacing(tickSpacing)
        }
        maxScrollPx = scale.totalSteps * tickSpacing.toDouble()
        currentValue = if (snapToStep) scale.snap(currentValue) else scale.coerce(currentValue)
        scrollPx = scrollForValue(currentValue)
        lastHapticStep = scale.stepIndexFor(currentValue)
        updateAccessibilityState()
        requestLayout()
        invalidate()
    }

    private fun scrollForValue(value: Double): Double =
        scale.fractionalStepIndex(value) * tickSpacing.toDouble()

    private fun valueAtScroll(scroll: Double): Double =
        scale.valueForFractionalStep(scroll / tickSpacing.toDouble())

    private fun formatValue(value: Double): String {
        valueFormatter?.let { return it(value) }
        return scale.formatWithUnit(value, unit)
    }

    private fun formatTick(value: Double): String =
        tickLabelFormatter?.invoke(value) ?: scale.format(value)

    // ── Scroll / value updates ─────────────────────────────────────────

    private fun applyScroll(newScroll: Double, notify: Boolean) {
        val clamped = newScroll.coerceIn(0.0, maxScrollPx)
        scrollPx = clamped
        if (notify) {
            val raw = valueAtScroll(clamped)
            updateValue(if (snapToStep) scale.snap(raw) else scale.coerce(raw))
        }
        invalidate()
    }

    private fun updateValue(newValue: Double) {
        val coerced = if (snapToStep) scale.snap(newValue) else scale.coerce(newValue)
        if (abs(coerced - currentValue) < VALUE_EPSILON) return
        currentValue = coerced
        onValueChangeListener?.invoke(coerced)
        handleHaptic(coerced)
        updateAccessibilityState()
    }

    private fun handleHaptic(value: Double) {
        if (!isEnabled || !enableHaptics || hapticMode == RulerHapticMode.NONE) return
        val stepIndex = scale.stepIndexFor(value)
        if (stepIndex == lastHapticStep) return
        lastHapticStep = stepIndex
        if (hapticMode == RulerHapticMode.MAJOR_STEP && !scale.isMajorStep(stepIndex)) return
        val now = SystemClock.uptimeMillis()
        if (now - lastHapticTime < hapticThrottleMs) return
        lastHapticTime = now
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun updateAccessibilityState() {
        if (::scale.isInitialized) {
            ViewCompat.setStateDescription(
                this,
                "${formatValue(currentValue)}, range ${scale.format(scale.minValue)} " +
                    "to ${scale.format(scale.effectiveMaxValue)}",
            )
        }
        invalidate()
    }

    private fun notifyFinished() {
        onValueChangeFinishedListener?.invoke(currentValue)
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED)
    }

    private fun animateScrollTo(target: Double, notifyFinished: Boolean) {
        cancelAnimations()
        val targetClamped = target.coerceIn(0.0, maxScrollPx)
        val distance = abs(targetClamped - scrollPx)
        if (distance < MIN_ANIMATION_DISTANCE_PX) {
            applyScroll(targetClamped, notify = true)
            if (notifyFinished) notifyFinished()
            return
        }
        val duration = animationDuration.toLong().coerceAtLeast(1L)
        scrollAnimator = ValueAnimator.ofFloat(scrollPx.toFloat(), targetClamped.toFloat()).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                applyScroll((animator.animatedValue as Float).toDouble(), notify = true)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    scrollAnimator = null
                    if (notifyFinished) notifyFinished()
                }
            })
            start()
        }
    }

    private fun cancelAnimations() {
        scrollAnimator?.cancel()
        scrollAnimator = null
        if (isFlinging || !scroller.isFinished) {
            scroller.forceFinished(true)
            removeCallbacks(flingRunnable)
        }
        isFlinging = false
    }

    private fun settleAfterFling() {
        if (snapToStep) {
            val snapped = scale.snap(valueAtScroll(scrollPx))
            animateScrollTo(scrollForValue(snapped), notifyFinished = true)
        } else {
            notifyFinished()
        }
    }

    private fun startFling(velocityX: Float, velocityY: Float) {
        val rawVelocity = if (orientation == RulerOrientation.HORIZONTAL) velocityX else velocityY
        val scrollVelocity = -rawVelocity * directionSign
        if (!enableFling || abs(scrollVelocity) < MIN_FLING_VELOCITY) {
            settleAfterFling()
            return
        }
        isFlinging = true
        scroller.fling(
            scrollPx.roundToInt(),
            0,
            scrollVelocity.roundToInt(),
            0,
            0,
            maxScrollPx.roundToInt().coerceAtLeast(0),
            0,
            0,
        )
        postOnAnimation(flingRunnable)
    }

    // ── Touch handling ─────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || readOnly) return false

        if (velocityTracker == null) {
            velocityTracker = android.view.VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelAnimations()
                isDragging = false
                dragNotified = false
                totalDrag = 0f
                downCoord = primaryCoordinate(event)
                lastCoord = downCoord
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val coord = primaryCoordinate(event)
                val delta = coord - lastCoord
                totalDrag += delta
                if (!isDragging && abs(totalDrag) > touchSlop) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    if (!dragNotified) {
                        dragNotified = true
                        onDragStartListener?.invoke()
                    }
                }
                if (isDragging) {
                    applyScroll(scrollPx - delta * directionSign, notify = true)
                    lastCoord = coord
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val wasDragging = isDragging
                isDragging = false
                if (wasDragging) {
                    onDragEndListener?.invoke()
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    val vy = velocityTracker?.yVelocity ?: 0f
                    startFling(vx, vy)
                } else {
                    performClick()
                    handleTap(primaryCoordinate(event))
                }
                recycleVelocityTracker()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                val wasDragging = isDragging
                isDragging = false
                if (wasDragging) {
                    onDragEndListener?.invoke()
                    settleAfterFling()
                }
                recycleVelocityTracker()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun primaryCoordinate(event: MotionEvent): Float =
        if (orientation == RulerOrientation.HORIZONTAL) event.x else event.y

    private fun handleTap(coordinate: Float) {
        val length = if (orientation == RulerOrientation.HORIZONTAL) {
            (width - paddingLeft - paddingRight).toFloat()
        } else {
            (height - paddingTop - paddingBottom).toFloat()
        }
        if (length <= 0f) return
        val centerIndex = scrollPx / tickSpacing.toDouble()
        val center = length / 2f + if (orientation == RulerOrientation.HORIZONTAL) {
            paddingLeft.toFloat()
        } else {
            paddingTop.toFloat()
        }
        val targetIndex = centerIndex +
            (coordinate - center) * directionSign / tickSpacing.toDouble()
        val rawValue = scale.valueForFractionalStep(targetIndex)
        val target = if (snapToStep) scale.snap(rawValue) else scale.coerce(rawValue)
        animateScrollTo(scrollForValue(target), notifyFinished = true)
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isEnabled || readOnly) return super.onKeyDown(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN -> {
                scrollBy(-1L)
                true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP -> {
                scrollBy(1L)
                true
            }

            KeyEvent.KEYCODE_PAGE_DOWN -> {
                scrollBy(-scale.majorEvery)
                true
            }

            KeyEvent.KEYCODE_PAGE_UP -> {
                scrollBy(scale.majorEvery)
                true
            }

            KeyEvent.KEYCODE_MOVE_HOME -> {
                setValue(scale.minValue, animate = true)
                true
            }

            KeyEvent.KEYCODE_MOVE_END -> {
                setValue(scale.effectiveMaxValue, animate = true)
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    // ── Accessibility ──────────────────────────────────────────────────

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = RulerValuePicker::class.java.name
        info.rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(
            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
            scale.minValue.toFloat(),
            scale.effectiveMaxValue.toFloat(),
            currentValue.toFloat(),
        )
        if (isEnabled && !readOnly) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS)
            }
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        if (!isEnabled || readOnly) return super.performAccessibilityAction(action, arguments)
        return when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> {
                scrollBy(1L)
                true
            }

            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> {
                scrollBy(-1L)
                true
            }

            AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id -> {
                val target = arguments?.getFloat(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,
                    currentValue.toFloat(),
                )?.toDouble() ?: currentValue
                setValue(target, animate = true)
                true
            }

            else -> super.performAccessibilityAction(action, arguments)
        }
    }

    // ── Measurement ────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val horizontal = orientation == RulerOrientation.HORIZONTAL
        val contentHeight = desiredContentHeight()
        val contentWidth = desiredContentWidth()

        val desiredWidth = if (horizontal) {
            minimumLength.toInt() + paddingLeft + paddingRight
        } else {
            contentWidth.toInt() + paddingLeft + paddingRight
        }
        val desiredHeight = if (horizontal) {
            contentHeight.toInt() + paddingTop + paddingBottom
        } else {
            minimumLength.toInt() + paddingTop + paddingBottom
        }

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    private fun activeIndicatorHeight(): Float =
        if (showCenterIndicator && indicatorStyle != RulerIndicatorStyle.NONE) {
            indicatorHeight
        } else {
            0f
        }

    private fun tickAreaHeight(): Float = maxOf(
        majorTickHeight,
        selectedTickHeight,
        activeIndicatorHeight(),
    )

    private fun desiredContentHeight(): Float {
        var height = 0f
        val mode = effectiveValueMode()
        if (mode == RulerValueDisplayMode.HEADER) {
            height += headerTextHeight() + headerGap
        }
        if (mode == RulerValueDisplayMode.BUBBLE) {
            height += valueTextHeight() + bubbleVerticalPadding * 2f + bubbleGap
        }
        val labels = showLabels && labelPosition != RulerLabelPosition.NONE
        if (labels && labelPosition != RulerLabelPosition.BELOW) {
            height += labelTextHeight() + labelGap
            if (labelPosition == RulerLabelPosition.ALTERNATE) {
                height += alternateLabelGap + labelTextHeight()
            }
        }
        height += tickAreaHeight()
        if (labels && labelPosition == RulerLabelPosition.BELOW) {
            height += labelGap + labelTextHeight()
        }
        return height
    }

    private fun desiredContentWidth(): Float {
        var width = tickAreaHeight()
        if (showLabels) {
            width += labelGap + labelTextHeight() * 3f
            if (labelPosition == RulerLabelPosition.ALTERNATE) {
                width += alternateLabelGap + labelTextHeight() * 1.5f
            }
        }
        return width
    }

    // ── Drawing ────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        containerPaint.color = if (isEnabled) containerColor else withAlpha(containerColor)
        if (Color.alpha(containerPaint.color) > 0) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), containerPaint)
        }

        val contentLeft = paddingLeft.toFloat()
        val contentRight = width - paddingRight.toFloat()
        val contentTop = paddingTop.toFloat()
        val contentBottom = height - paddingBottom.toFloat()
        if (contentRight <= contentLeft || contentBottom <= contentTop) return

        val layer = if (showFadingEdges && fadeWidth > 0f) {
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        } else {
            -1
        }

        canvas.save()
        canvas.clipRect(contentLeft, contentTop, contentRight, contentBottom)

        if (orientation == RulerOrientation.HORIZONTAL) {
            drawHorizontal(canvas, contentLeft, contentTop, contentRight, contentBottom)
        } else {
            drawVertical(canvas, contentLeft, contentTop, contentRight, contentBottom)
        }

        canvas.restore()

        if (layer != -1) {
            drawFadeMask(canvas, contentLeft, contentTop, contentRight, contentBottom)
            canvas.restoreToCount(layer)
        }
    }

    private fun drawHorizontal(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        val centerX = (left + right) / 2f
        var cursor = top
        val mode = effectiveValueMode()
        val labels = showLabels && labelPosition != RulerLabelPosition.NONE

        if (mode == RulerValueDisplayMode.HEADER) {
            drawHeader(canvas, centerX, cursor)
            cursor += headerTextHeight() + headerGap
        }

        var drawBubble = false
        if (mode == RulerValueDisplayMode.BUBBLE) {
            drawBubble = true
            val text = formatValue(currentValue)
            valuePaint.textSize = valueTextSize
            val textWidth = valuePaint.measureText(text)
            val textHeight = valueTextHeight()
            val bubbleWidth = (textWidth + bubbleHorizontalPadding * 2f)
                .coerceAtMost(right - left)
            val bubbleHeight = textHeight + bubbleVerticalPadding * 2f
            bubbleRect.set(
                centerX - bubbleWidth / 2f,
                cursor,
                centerX + bubbleWidth / 2f,
                cursor + bubbleHeight,
            )
            cursor += bubbleHeight + bubbleGap
        }

        val labelHeight = labelTextHeight()
        var labelAreaTop = cursor
        if (labels && labelPosition != RulerLabelPosition.BELOW) {
            labelAreaTop = cursor
            if (labelPosition == RulerLabelPosition.ALTERNATE) {
                cursor += labelHeight * 2f + alternateLabelGap + labelGap
            } else {
                cursor += labelHeight + labelGap
            }
        }

        val tickTop = cursor
        val tickBottom = tickTop + tickAreaHeight()
        val labelTopBelow = tickBottom + labelGap

        if (showBaseline) {
            tickPaint.color = colorFor(baselineColor)
            tickPaint.strokeWidth = baselineWidth
            canvas.drawLine(left, tickBottom, right, tickBottom, tickPaint)
        }

        val labelsBelow = labels && labelPosition == RulerLabelPosition.BELOW
        val centerIndex = scrollPx / tickSpacing.toDouble()
        val halfSpan = (right - left) / 2f / tickSpacing + 2f
        val start = floor(centerIndex - halfSpan).toLong()
        val end = ceil(centerIndex + halfSpan).toLong()
        val selectedIndex = scale.stepIndexFor(currentValue)
        val skip = scale.labelSkipFactor(tickSpacing, minLabelSpacing)

        for (index in start..end) {
            if (index < 0L || index > scale.totalSteps) continue
            val x = centerX + ((index - centerIndex) * tickSpacing * directionSign).toFloat()
            if (x < left - tickSpacing || x > right + tickSpacing) continue

            val isMajor = scale.isMajorStep(index)
            val isSelected = index == selectedIndex && showSelectedTickHighlight
            if (!drawTickHorizontal(canvas, x, tickBottom, isMajor, isSelected)) continue

            if (labels && scale.showsLabel(index, skip) && x >= left && x <= right) {
                val text = formatTick(scale.valueForStep(index))
                labelPaint.textSize = labelTextSize
                val selected = showSelectedLabel && index == selectedIndex
                labelPaint.color = colorFor(
                    if (selected) selectedLabelColor else labelColor,
                )
                labelPaint.textAlign = Paint.Align.CENTER
                val baseline = if (labelsBelow) {
                    labelTopBelow - labelPaint.fontMetrics.top
                } else if (labelPosition == RulerLabelPosition.ALTERNATE) {
                    val row = if ((index / scale.majorEvery) % 2L == 0L) 0 else 1
                    val rowTop = labelAreaTop +
                        row * (labelHeight + alternateLabelGap)
                    rowTop - labelPaint.fontMetrics.top
                } else {
                    labelAreaTop - labelPaint.fontMetrics.top
                }
                canvas.drawText(text, x, baseline, labelPaint)
            }
        }

        drawIndicatorHorizontal(canvas, centerX, tickBottom, tickTop)

        if (drawBubble) {
            drawBubbleText(canvas, centerX)
        }
    }

    private fun drawTickHorizontal(
        canvas: Canvas,
        x: Float,
        baseline: Float,
        isMajor: Boolean,
        isSelected: Boolean,
    ): Boolean {
        if (isMajor && !showMajorTicks) return false
        if (!isMajor && !showMinorTicks) return false

        val color = when {
            isSelected -> selectedTickColor
            isMajor -> majorTickColor
            else -> tickColor
        }
        val width = when {
            isSelected -> selectedTickWidth
            isMajor -> majorTickWidth
            else -> minorTickWidth
        }
        val height = when {
            isSelected -> selectedTickHeight
            isMajor -> majorTickHeight
            else -> minorTickHeight
        }
        tickPaint.color = colorFor(color)

        when (tickShape) {
            RulerTickShape.LINE -> {
                tickPaint.strokeWidth = width
                canvas.drawLine(x, baseline - height, x, baseline, tickPaint)
            }

            RulerTickShape.ROUNDED_LINE -> {
                tickFillPaint.color = colorFor(color)
                canvas.drawRoundRect(
                    x - width / 2f,
                    baseline - height,
                    x + width / 2f,
                    baseline,
                    tickCornerRadius,
                    tickCornerRadius,
                    tickFillPaint,
                )
            }

            RulerTickShape.DOT -> {
                tickFillPaint.color = colorFor(color)
                val radius = if (isSelected) {
                    max(dotRadius, width)
                } else if (isMajor) {
                    max(dotRadius, width)
                } else {
                    dotRadius
                }
                canvas.drawCircle(x, baseline - radius, radius, tickFillPaint)
            }
        }
        return true
    }

    private fun drawIndicatorHorizontal(
        canvas: Canvas,
        centerX: Float,
        baseline: Float,
        tickTop: Float,
    ) {
        if (!showCenterIndicator || indicatorStyle == RulerIndicatorStyle.NONE) return
        tickPaint.color = colorFor(indicatorColor)
        tickPaint.strokeWidth = indicatorWidth
        canvas.drawLine(
            centerX,
            baseline - indicatorHeight,
            centerX,
            baseline + indicatorWidth,
            tickPaint,
        )
        if (indicatorStyle == RulerIndicatorStyle.TRIANGLE) {
            tickFillPaint.color = colorFor(indicatorColor)
            canvas.drawPath(
                trianglePath(
                    centerX - triangleWidth / 2f,
                    baseline - triangleHeight,
                    centerX + triangleWidth / 2f,
                    baseline - triangleHeight,
                    centerX,
                    baseline,
                ),
                tickFillPaint,
            )
        }
    }

    private fun drawVertical(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        var cursor = top
        val mode = effectiveValueMode()
        val labels = showLabels && labelPosition != RulerLabelPosition.NONE
        val labelsOnRight = labelPosition == RulerLabelPosition.BELOW

        if (mode == RulerValueDisplayMode.HEADER) {
            drawHeader(canvas, (left + right) / 2f, cursor)
            cursor += headerTextHeight() + headerGap
        }

        val centerY = (cursor + bottom) / 2f
        val tickLength = tickAreaHeight()
        val baselineX = if (labelsOnRight) left else right
        val sign = if (labelsOnRight) 1f else -1f
        val labelX = baselineX + sign * (tickLength + labelGap)

        if (showBaseline) {
            tickPaint.color = colorFor(baselineColor)
            tickPaint.strokeWidth = baselineWidth
            canvas.drawLine(baselineX, cursor, baselineX, bottom, tickPaint)
        }

        val centerIndex = scrollPx / tickSpacing.toDouble()
        val halfSpan = (bottom - cursor) / 2f / tickSpacing + 2f
        val start = floor(centerIndex - halfSpan).toLong()
        val end = ceil(centerIndex + halfSpan).toLong()
        val selectedIndex = scale.stepIndexFor(currentValue)
        val skip = scale.labelSkipFactor(tickSpacing, minLabelSpacing)

        for (index in start..end) {
            if (index < 0L || index > scale.totalSteps) continue
            val y = centerY +
                ((index - centerIndex) * tickSpacing * directionSign).toFloat()
            if (y < cursor - tickSpacing || y > bottom + tickSpacing) continue

            val isMajor = scale.isMajorStep(index)
            val isSelected = index == selectedIndex && showSelectedTickHighlight
            if (!drawTickVertical(canvas, baselineX, y, sign, isMajor, isSelected)) continue

            if (labels && scale.showsLabel(index, skip) && y >= cursor && y <= bottom) {
                val text = formatTick(scale.valueForStep(index))
                labelPaint.textSize = labelTextSize
                val selected = showSelectedLabel && index == selectedIndex
                labelPaint.color = colorFor(
                    if (selected) selectedLabelColor else labelColor,
                )
                labelPaint.textAlign = if (labelsOnRight) Paint.Align.LEFT else Paint.Align.RIGHT
                var x = labelX
                if (labelPosition == RulerLabelPosition.ALTERNATE) {
                    val row = if ((index / scale.majorEvery) % 2L == 0L) 0 else 1
                    x += sign * row * (alternateLabelGap + labelTextHeight())
                }
                val baseline = y - (labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2f
                canvas.drawText(text, x, baseline, labelPaint)
            }
        }

        drawIndicatorVertical(canvas, centerY, baselineX, sign)
    }

    private fun drawTickVertical(
        canvas: Canvas,
        baselineX: Float,
        y: Float,
        sign: Float,
        isMajor: Boolean,
        isSelected: Boolean,
    ): Boolean {
        if (isMajor && !showMajorTicks) return false
        if (!isMajor && !showMinorTicks) return false

        val color = when {
            isSelected -> selectedTickColor
            isMajor -> majorTickColor
            else -> tickColor
        }
        val width = when {
            isSelected -> selectedTickWidth
            isMajor -> majorTickWidth
            else -> minorTickWidth
        }
        val length = when {
            isSelected -> selectedTickHeight
            isMajor -> majorTickHeight
            else -> minorTickHeight
        }
        tickPaint.color = colorFor(color)

        when (tickShape) {
            RulerTickShape.LINE -> {
                tickPaint.strokeWidth = width
                canvas.drawLine(baselineX, y, baselineX + sign * length, y, tickPaint)
            }

            RulerTickShape.ROUNDED_LINE -> {
                tickFillPaint.color = colorFor(color)
                val leftX = minOf(baselineX, baselineX + sign * length)
                val rightX = maxOf(baselineX, baselineX + sign * length)
                canvas.drawRoundRect(
                    leftX,
                    y - width / 2f,
                    rightX,
                    y + width / 2f,
                    tickCornerRadius,
                    tickCornerRadius,
                    tickFillPaint,
                )
            }

            RulerTickShape.DOT -> {
                tickFillPaint.color = colorFor(color)
                val radius = max(dotRadius, width)
                canvas.drawCircle(baselineX + sign * radius, y, radius, tickFillPaint)
            }
        }
        return true
    }

    private fun drawIndicatorVertical(
        canvas: Canvas,
        centerY: Float,
        baselineX: Float,
        sign: Float,
    ) {
        if (!showCenterIndicator || indicatorStyle == RulerIndicatorStyle.NONE) return
        tickPaint.color = colorFor(indicatorColor)
        tickPaint.strokeWidth = indicatorWidth
        canvas.drawLine(
            baselineX,
            centerY,
            baselineX + sign * indicatorHeight,
            centerY,
            tickPaint,
        )
        if (indicatorStyle == RulerIndicatorStyle.TRIANGLE) {
            tickFillPaint.color = colorFor(indicatorColor)
            canvas.drawPath(
                trianglePath(
                    baselineX + sign * triangleHeight,
                    centerY - triangleWidth / 2f,
                    baselineX + sign * triangleHeight,
                    centerY + triangleWidth / 2f,
                    baselineX,
                    centerY,
                ),
                tickFillPaint,
            )
        }
    }

    private fun drawHeader(canvas: Canvas, centerX: Float, top: Float) {
        headerPaint.textSize = headerTextSize
        headerPaint.color = colorFor(headerTextColor)
        headerPaint.textAlign = Paint.Align.CENTER
        val baseline = top - headerPaint.fontMetrics.top
        canvas.drawText(formatValue(currentValue), centerX, baseline, headerPaint)
    }

    private fun drawBubbleText(canvas: Canvas, centerX: Float) {
        tickFillPaint.color = colorFor(bubbleColor)
        canvas.drawRoundRect(
            bubbleRect,
            bubbleCornerRadius,
            bubbleCornerRadius,
            tickFillPaint,
        )
        bubblePath.reset()
        bubblePath.moveTo(centerX - triangleWidth / 2f, bubbleRect.bottom - 1f)
        bubblePath.lineTo(centerX + triangleWidth / 2f, bubbleRect.bottom - 1f)
        bubblePath.lineTo(centerX, bubbleRect.bottom + triangleHeight)
        bubblePath.close()
        canvas.drawPath(bubblePath, tickFillPaint)

        valuePaint.textSize = valueTextSize
        valuePaint.color = colorFor(bubbleTextColor)
        valuePaint.textAlign = Paint.Align.CENTER
        val text = formatValue(currentValue)
        val baseline = bubbleRect.centerY() -
            (valuePaint.fontMetrics.ascent + valuePaint.fontMetrics.descent) / 2f
        canvas.drawText(text, centerX, baseline, valuePaint)
    }

    private fun drawFadeMask(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        val horizontal = orientation == RulerOrientation.HORIZONTAL
        val maxFade = if (horizontal) (right - left) / 2f else (bottom - top) / 2f
        val fade = fadeWidth.coerceIn(0f, maxFade)
        if (fade <= 0f) return

        fadePaint.xfermode = fadeXfermode
        if (horizontal) {
            fadePaint.shader = LinearGradient(
                left,
                0f,
                left + fade,
                0f,
                Color.TRANSPARENT,
                Color.BLACK,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(left, top, left + fade, bottom, fadePaint)

            fadePaint.shader = LinearGradient(
                right - fade,
                0f,
                right,
                0f,
                Color.BLACK,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(right - fade, top, right, bottom, fadePaint)
        } else {
            fadePaint.shader = LinearGradient(
                0f,
                top,
                0f,
                top + fade,
                Color.TRANSPARENT,
                Color.BLACK,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(left, top, right, top + fade, fadePaint)

            fadePaint.shader = LinearGradient(
                0f,
                bottom - fade,
                0f,
                bottom,
                Color.BLACK,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(left, bottom - fade, right, bottom, fadePaint)
        }
        fadePaint.shader = null
        fadePaint.xfermode = null
    }

    private fun trianglePath(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ): Path {
        bubblePath.reset()
        bubblePath.moveTo(x1, y1)
        bubblePath.lineTo(x2, y2)
        bubblePath.lineTo(x3, y3)
        bubblePath.close()
        return bubblePath
    }

    private fun effectiveValueMode(): RulerValueDisplayMode {
        if (!showValue) return RulerValueDisplayMode.NONE
        val requested = when {
            valueDisplayMode != RulerValueDisplayMode.NONE -> valueDisplayMode
            indicatorStyle == RulerIndicatorStyle.BUBBLE -> RulerValueDisplayMode.BUBBLE
            else -> RulerValueDisplayMode.NONE
        }
        return if (orientation == RulerOrientation.VERTICAL &&
            requested == RulerValueDisplayMode.BUBBLE
        ) {
            RulerValueDisplayMode.HEADER
        } else {
            requested
        }
    }

    // ── Paint helpers ──────────────────────────────────────────────────

    private fun colorFor(color: Int): Int = if (isEnabled) color else withAlpha(color)

    private fun withAlpha(color: Int): Int {
        val alpha = (Color.alpha(color) * disabledAlpha).roundToInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun labelTextHeight(): Float {
        labelPaint.textSize = labelTextSize
        return labelPaint.fontMetrics.descent - labelPaint.fontMetrics.ascent
    }

    private fun valueTextHeight(): Float {
        valuePaint.textSize = valueTextSize
        return valuePaint.fontMetrics.descent - valuePaint.fontMetrics.ascent
    }

    private fun headerTextHeight(): Float {
        headerPaint.textSize = headerTextSize
        return headerPaint.fontMetrics.descent - headerPaint.fontMetrics.ascent
    }

    private val directionSign: Double
        get() {
            var sign = if (reversed) -1.0 else 1.0
            if (orientation == RulerOrientation.HORIZONTAL &&
                layoutDirection == LAYOUT_DIRECTION_RTL
            ) {
                sign = -sign
            }
            return sign
        }

    // ── Attribute parsing ──────────────────────────────────────────────

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(
            attrs,
            R.styleable.RulerValuePicker,
            defStyleAttr,
            0,
        )
        try {
            _minValue = ta.getFloat(R.styleable.RulerValuePicker_rulerMinValue, 0f).toDouble()
            _maxValue = ta.getFloat(R.styleable.RulerValuePicker_rulerMaxValue, 100f).toDouble()
            _step = ta.getFloat(R.styleable.RulerValuePicker_rulerStep, 1f).toDouble()
            _majorStep = ta.getFloat(R.styleable.RulerValuePicker_rulerMajorStep, 10f).toDouble()
            _decimals = ta.getInt(
                R.styleable.RulerValuePicker_rulerDecimals,
                RulerValuePickerDefaults.AutoDecimals,
            )
            currentValue = ta.getFloat(R.styleable.RulerValuePicker_rulerValue, _minValue.toFloat())
                .toDouble()
            unit = ta.getString(R.styleable.RulerValuePicker_rulerUnit)
            isEnabled = ta.getBoolean(R.styleable.RulerValuePicker_android_enabled, true)

            orientation = enumValue(
                ta.getInt(R.styleable.RulerValuePicker_rulerOrientation, 0),
                RulerOrientation.entries,
            )
            reversed = ta.getBoolean(R.styleable.RulerValuePicker_rulerReversed, false)
            snapToStep = ta.getBoolean(R.styleable.RulerValuePicker_rulerSnapToStep, true)
            readOnly = ta.getBoolean(R.styleable.RulerValuePicker_rulerReadOnly, false)
            enableFling = ta.getBoolean(R.styleable.RulerValuePicker_rulerEnableFling, true)
            enableHaptics = ta.getBoolean(R.styleable.RulerValuePicker_rulerEnableHaptics, true)
            hapticMode = enumValue(
                ta.getInt(R.styleable.RulerValuePicker_rulerHapticMode, 1),
                RulerHapticMode.entries,
            )
            tickShape = enumValue(
                ta.getInt(R.styleable.RulerValuePicker_rulerTickShape, 0),
                RulerTickShape.entries,
            )
            labelPosition = enumValue(
                ta.getInt(R.styleable.RulerValuePicker_rulerLabelPosition, 0),
                RulerLabelPosition.entries,
            )
            indicatorStyle = enumValue(
                ta.getInt(R.styleable.RulerValuePicker_rulerIndicatorStyle, 0),
                RulerIndicatorStyle.entries,
            )
            valueDisplayMode = enumValue(
                ta.getInt(R.styleable.RulerValuePicker_rulerValueDisplayMode, 1),
                RulerValueDisplayMode.entries,
            )

            showMinorTicks = ta.getBoolean(R.styleable.RulerValuePicker_rulerShowMinorTicks, true)
            showMajorTicks = ta.getBoolean(R.styleable.RulerValuePicker_rulerShowMajorTicks, true)
            showLabels = ta.getBoolean(R.styleable.RulerValuePicker_rulerShowLabels, true)
            showSelectedLabel = ta.getBoolean(
                R.styleable.RulerValuePicker_rulerShowSelectedLabel,
                true,
            )
            showSelectedTickHighlight = ta.getBoolean(
                R.styleable.RulerValuePicker_rulerShowSelectedTick,
                true,
            )
            showBaseline = ta.getBoolean(R.styleable.RulerValuePicker_rulerShowBaseline, true)
            showCenterIndicator = ta.getBoolean(
                R.styleable.RulerValuePicker_rulerShowIndicator,
                true,
            )
            showValue = ta.getBoolean(R.styleable.RulerValuePicker_rulerShowValue, true)
            showFadingEdges = ta.getBoolean(
                R.styleable.RulerValuePicker_rulerShowFadingEdges,
                true,
            )

            val primary = selectedTickColor
            tickColor = ta.getColor(R.styleable.RulerValuePicker_rulerTickColor, tickColor)
            majorTickColor = ta.getColor(
                R.styleable.RulerValuePicker_rulerMajorTickColor,
                majorTickColor,
            )
            selectedTickColor = ta.getColor(
                R.styleable.RulerValuePicker_rulerSelectedTickColor,
                primary,
            )
            labelColor = ta.getColor(R.styleable.RulerValuePicker_rulerLabelColor, labelColor)
            selectedLabelColor = ta.getColor(
                R.styleable.RulerValuePicker_rulerSelectedLabelColor,
                primary,
            )
            indicatorColor = ta.getColor(
                R.styleable.RulerValuePicker_rulerIndicatorColor,
                primary,
            )
            baselineColor = ta.getColor(
                R.styleable.RulerValuePicker_rulerBaselineColor,
                baselineColor,
            )
            bubbleColor = ta.getColor(R.styleable.RulerValuePicker_rulerBubbleColor, primary)
            bubbleTextColor = ta.getColor(
                R.styleable.RulerValuePicker_rulerBubbleTextColor,
                bubbleTextColor,
            )
            headerTextColor = ta.getColor(
                R.styleable.RulerValuePicker_rulerHeaderTextColor,
                headerTextColor,
            )
            containerColor = ta.getColor(
                R.styleable.RulerValuePicker_rulerContainerColor,
                containerColor,
            )
            disabledAlpha = ta.getFloat(
                R.styleable.RulerValuePicker_rulerDisabledAlpha,
                disabledAlpha,
            )

            tickSpacing = ta.getDimension(
                R.styleable.RulerValuePicker_rulerTickSpacing,
                tickSpacing,
            )
            minLabelSpacing = ta.getDimension(
                R.styleable.RulerValuePicker_rulerMinLabelSpacing,
                minLabelSpacing,
            )
            fadeWidth = ta.getDimension(R.styleable.RulerValuePicker_rulerFadeWidth, fadeWidth)
            minorTickWidth = ta.getDimension(
                R.styleable.RulerValuePicker_rulerMinorTickWidth,
                minorTickWidth,
            )
            minorTickHeight = ta.getDimension(
                R.styleable.RulerValuePicker_rulerMinorTickHeight,
                minorTickHeight,
            )
            majorTickWidth = ta.getDimension(
                R.styleable.RulerValuePicker_rulerMajorTickWidth,
                majorTickWidth,
            )
            majorTickHeight = ta.getDimension(
                R.styleable.RulerValuePicker_rulerMajorTickHeight,
                majorTickHeight,
            )
            selectedTickWidth = ta.getDimension(
                R.styleable.RulerValuePicker_rulerSelectedTickWidth,
                selectedTickWidth,
            )
            selectedTickHeight = ta.getDimension(
                R.styleable.RulerValuePicker_rulerSelectedTickHeight,
                selectedTickHeight,
            )
            tickCornerRadius = ta.getDimension(
                R.styleable.RulerValuePicker_rulerTickCornerRadius,
                tickCornerRadius,
            )
            dotRadius = ta.getDimension(R.styleable.RulerValuePicker_rulerDotRadius, dotRadius)
            labelTextSize = ta.getDimension(
                R.styleable.RulerValuePicker_rulerLabelTextSize,
                labelTextSize,
            )
            labelGap = ta.getDimension(R.styleable.RulerValuePicker_rulerLabelGap, labelGap)
            alternateLabelGap = ta.getDimension(
                R.styleable.RulerValuePicker_rulerAlternateLabelGap,
                alternateLabelGap,
            )
            valueTextSize = ta.getDimension(
                R.styleable.RulerValuePicker_rulerValueTextSize,
                valueTextSize,
            )
            headerTextSize = ta.getDimension(
                R.styleable.RulerValuePicker_rulerHeaderTextSize,
                headerTextSize,
            )
            headerGap = ta.getDimension(R.styleable.RulerValuePicker_rulerHeaderGap, headerGap)
            indicatorWidth = ta.getDimension(
                R.styleable.RulerValuePicker_rulerIndicatorWidth,
                indicatorWidth,
            )
            indicatorHeight = ta.getDimension(
                R.styleable.RulerValuePicker_rulerIndicatorHeight,
                indicatorHeight,
            )
            triangleWidth = ta.getDimension(
                R.styleable.RulerValuePicker_rulerTriangleWidth,
                triangleWidth,
            )
            triangleHeight = ta.getDimension(
                R.styleable.RulerValuePicker_rulerTriangleHeight,
                triangleHeight,
            )
            bubbleCornerRadius = ta.getDimension(
                R.styleable.RulerValuePicker_rulerBubbleCornerRadius,
                bubbleCornerRadius,
            )
            bubbleHorizontalPadding = ta.getDimension(
                R.styleable.RulerValuePicker_rulerBubbleHorizontalPadding,
                bubbleHorizontalPadding,
            )
            bubbleVerticalPadding = ta.getDimension(
                R.styleable.RulerValuePicker_rulerBubbleVerticalPadding,
                bubbleVerticalPadding,
            )
            bubbleGap = ta.getDimension(R.styleable.RulerValuePicker_rulerBubbleGap, bubbleGap)
            baselineWidth = ta.getDimension(
                R.styleable.RulerValuePicker_rulerBaselineWidth,
                baselineWidth,
            )
            minimumLength = ta.getDimension(
                R.styleable.RulerValuePicker_rulerMinimumLength,
                minimumLength,
            )
            animationDuration = ta.getInt(
                R.styleable.RulerValuePicker_rulerAnimationDuration,
                animationDuration,
            ).coerceAtLeast(1)
            hapticThrottleMs = ta.getInt(
                R.styleable.RulerValuePicker_rulerHapticThrottle,
                hapticThrottleMs.toInt(),
            ).toLong().coerceAtLeast(0L)
        } finally {
            ta.recycle()
        }
    }

    private fun <T> enumValue(index: Int, values: List<T>): T =
        values[index.coerceIn(0, values.lastIndex)]

    // ── Utilities & lifecycle ──────────────────────────────────────────

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        cancelAnimations()
        recycleVelocityTracker()
        super.onDetachedFromWindow()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState).also { it.value = currentValue }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            setValue(state.value, animate = false)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        var value: Double = 0.0

        constructor(superState: Parcelable?) : super(superState)
        private constructor(source: Parcel) : super(source) {
            value = source.readDouble()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeDouble(value)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }

    private fun getPrimaryColor(): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(
                android.R.attr.colorPrimary,
                typedValue,
                true,
            )
        ) {
            typedValue.data
        } else {
            0xFF2196F3.toInt()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    private companion object {
        const val VALUE_EPSILON = 1e-9
        const val MIN_FLING_VELOCITY = 60f
        const val MIN_ANIMATION_DISTANCE_PX = 0.5
    }
}
