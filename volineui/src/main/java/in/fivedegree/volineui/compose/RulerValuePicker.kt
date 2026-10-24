@file:Suppress("unused")

package `in`.fivedegree.volineui.compose

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import `in`.fivedegree.volineui.ruler.RulerHapticMode
import `in`.fivedegree.volineui.ruler.RulerIndicatorStyle
import `in`.fivedegree.volineui.ruler.RulerLabelPosition
import `in`.fivedegree.volineui.ruler.RulerOrientation
import `in`.fivedegree.volineui.ruler.RulerScale
import `in`.fivedegree.volineui.ruler.RulerTickShape
import `in`.fivedegree.volineui.ruler.RulerValueDisplayMode
import `in`.fivedegree.volineui.ruler.RulerValuePickerColors
import `in`.fivedegree.volineui.ruler.RulerValuePickerDefaults
import `in`.fivedegree.volineui.ruler.RulerValuePickerDimensions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * State holder for [RulerValuePicker].
 *
 * Create an instance with [rememberRulerValuePickerState]. The scale
 * configuration is immutable; to change it, create a new state with different
 * parameters. The current [value] survives configuration changes through
 * [Saver] as long as the scale configuration stays the same.
 *
 * @param minValue Smallest selectable value
 * @param maxValue Largest selectable value
 * @param step Distance between two consecutive ticks
 * @param majorStep Distance between two major ticks. Must be a multiple of [step]
 * @param decimals Number of decimal places used for display, or
 * [RulerValuePickerDefaults.AutoDecimals] to derive it from the scale
 * @param snapToStep When `true`, values always land on a discrete step
 * @param unit Optional suffix appended to the selected value, for example "kg"
 * @param initialValue Value selected when the state is created
 */
@Stable
class RulerValuePickerState internal constructor(
    val minValue: Double,
    val maxValue: Double,
    val step: Double,
    val majorStep: Double,
    val decimals: Int,
    val snapToStep: Boolean,
    val unit: String?,
    initialValue: Double,
) {

    internal val scale: RulerScale = RulerScale(
        minValue = minValue,
        maxValue = maxValue,
        step = step,
        majorStep = majorStep,
        decimals = decimals,
    )

    /** The currently selected value. */
    var value by mutableDoubleStateOf(scale.coerce(initialValue))
        internal set

    /**
     * Increments only when [setValue] or [scrollBy] is called programmatically.
     * The composable uses this to animate to externally requested values
     * without fighting user gestures.
     */
    internal var externalChangeVersion by mutableIntStateOf(0)
        private set

    /** The highest reachable value, which may be lower than [maxValue]. */
    val effectiveMaxValue: Double
        get() = scale.effectiveMaxValue

    /** The selected value formatted with [unit] and the configured decimals. */
    val formattedValue: String
        get() = scale.formatWithUnit(value, unit)

    /**
     * Selects [value], clamping it to the valid range and snapping it to the
     * nearest step when [snapToStep] is `true`.
     */
    fun setValue(value: Double) {
        this.value = if (snapToStep) scale.snap(value) else scale.coerce(value)
        externalChangeVersion++
    }

    /** Moves the selection by [steps] discrete steps. */
    fun scrollBy(steps: Long) {
        setValue(scale.valueForStep(scale.stepIndexFor(value) + steps))
    }

    /** Snaps the current value to the nearest step. */
    fun snap() {
        setValue(scale.snap(value))
    }

    /** Formats [value] using the configured decimals and unit. */
    fun format(value: Double): String = scale.formatWithUnit(value, unit)

    internal fun updateFromInteraction(newValue: Double) {
        value = newValue
    }

    companion object {
        /** Saves and restores a [RulerValuePickerState] across configuration changes. */
        val Saver: Saver<RulerValuePickerState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.value,
                    state.minValue,
                    state.maxValue,
                    state.step,
                    state.majorStep,
                    state.decimals,
                    state.snapToStep,
                    state.unit.orEmpty(),
                )
            },
            restore = { values ->
                RulerValuePickerState(
                    minValue = values[1] as Double,
                    maxValue = values[2] as Double,
                    step = values[3] as Double,
                    majorStep = values[4] as Double,
                    decimals = values[5] as Int,
                    snapToStep = values[6] as Boolean,
                    unit = (values[7] as String).takeIf { it.isNotEmpty() },
                    initialValue = values[0] as Double,
                )
            },
        )
    }
}

/**
 * Creates and remembers a [RulerValuePickerState].
 *
 * @param minValue Smallest selectable value
 * @param maxValue Largest selectable value
 * @param step Distance between two consecutive ticks
 * @param majorStep Distance between two major ticks
 * @param initialValue Value selected when the state is first created
 * @param decimals Number of decimal places used for display
 * @param snapToStep When `true`, values always land on a discrete step
 * @param unit Optional suffix appended to the selected value
 */
@Composable
fun rememberRulerValuePickerState(
    minValue: Double = 0.0,
    maxValue: Double = 100.0,
    step: Double = 1.0,
    majorStep: Double = 10.0,
    initialValue: Double = minValue,
    decimals: Int = RulerValuePickerDefaults.AutoDecimals,
    snapToStep: Boolean = true,
    unit: String? = null,
): RulerValuePickerState = rememberSaveable(
    minValue,
    maxValue,
    step,
    majorStep,
    decimals,
    snapToStep,
    unit,
    saver = RulerValuePickerState.Saver,
) {
    RulerValuePickerState(
        minValue = minValue,
        maxValue = maxValue,
        step = step,
        majorStep = majorStep,
        decimals = decimals,
        snapToStep = snapToStep,
        unit = unit,
        initialValue = initialValue,
    )
}

/**
 * A draggable ruler picker that selects a numeric value.
 *
 * The component draws a ruler with minor and major ticks, optional labels, a
 * fixed center indicator, and an optional value bubble or header. Drag to
 * scroll, fling to move quickly, or tap a tick to select it.
 *
 * Compose equivalent of `in.fivedegree.volineui.RulerValuePicker`.
 *
 * @param state State holder created with [rememberRulerValuePickerState]
 * @param modifier Modifier applied to the picker. Use it to control the size
 * @param enabled Whether the picker accepts user input
 * @param readOnly Whether the value can only be changed programmatically
 * @param orientation Horizontal or vertical ruler
 * @param reversed When `true`, the smallest value is on the right/bottom
 * @param tickShape Shape used to draw ticks
 * @param labelPosition Where tick labels are drawn
 * @param indicatorStyle Visual style of the fixed center indicator
 * @param valueDisplayMode How the selected value is displayed
 * @param showMinorTicks Whether minor ticks are drawn
 * @param showMajorTicks Whether major ticks are drawn
 * @param showLabels Whether tick labels are drawn
 * @param showSelectedLabel Whether the label nearest the center is highlighted
 * @param showSelectedTick Whether the tick nearest the center is highlighted
 * @param showBaseline Whether the baseline is drawn
 * @param showIndicator Whether the center indicator is drawn
 * @param showValue Whether the selected value is displayed
 * @param showFadingEdges Whether content fades out at the ends of the ruler
 * @param enableFling Whether releasing with velocity continues the scroll
 * @param enableHaptics Whether haptic feedback is performed while scrolling
 * @param hapticMode When haptics are performed
 * @param colors Colors used by the picker
 * @param dimensions Dimensions used by the picker
 * @param valueFormatter Optional formatter overriding [RulerValuePickerState.formattedValue]
 * @param contentDescription Accessibility description of the picker
 * @param onValueChange Called whenever the value changes, including animations
 * @param onValueChangeFinished Called when a gesture or animation settles
 */
@Composable
fun RulerValuePicker(
    state: RulerValuePickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    orientation: RulerOrientation = RulerOrientation.HORIZONTAL,
    reversed: Boolean = false,
    tickShape: RulerTickShape = RulerTickShape.LINE,
    labelPosition: RulerLabelPosition = RulerLabelPosition.ABOVE,
    indicatorStyle: RulerIndicatorStyle = RulerIndicatorStyle.LINE,
    valueDisplayMode: RulerValueDisplayMode = RulerValueDisplayMode.BUBBLE,
    showMinorTicks: Boolean = true,
    showMajorTicks: Boolean = true,
    showLabels: Boolean = true,
    showSelectedLabel: Boolean = true,
    showSelectedTick: Boolean = true,
    showBaseline: Boolean = true,
    showIndicator: Boolean = true,
    showValue: Boolean = true,
    showFadingEdges: Boolean = true,
    enableFling: Boolean = true,
    enableHaptics: Boolean = true,
    hapticMode: RulerHapticMode = RulerHapticMode.STEP,
    colors: RulerValuePickerColors = RulerValuePickerDefaults.colors(),
    dimensions: RulerValuePickerDimensions = RulerValuePickerDefaults.dimensions(),
    valueFormatter: ((Double) -> String)? = null,
    contentDescription: String? = null,
    onValueChange: ((Double) -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val visuals = RulerVisuals(
        orientation = orientation,
        reversed = reversed,
        tickShape = tickShape,
        labelPosition = labelPosition,
        indicatorStyle = indicatorStyle,
        valueDisplayMode = valueDisplayMode,
        showMinorTicks = showMinorTicks,
        showMajorTicks = showMajorTicks,
        showLabels = showLabels,
        showSelectedLabel = showSelectedLabel,
        showSelectedTick = showSelectedTick,
        showBaseline = showBaseline,
        showIndicator = showIndicator,
        showValue = showValue,
        showFadingEdges = showFadingEdges,
        colors = colors,
        dimensions = dimensions,
    )

    val scale = state.scale
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    val tickSpacingPx = with(density) { dimensions.tickSpacing.toPx() }
    val directionSign = when {
        orientation == RulerOrientation.HORIZONTAL &&
            layoutDirection == LayoutDirection.Rtl -> if (reversed) 1f else -1f
        reversed -> -1f
        else -> 1f
    }
    val totalSteps = scale.totalSteps
    val totalStepsF = totalSteps.toFloat()

    var offsetSteps by remember(state) {
        mutableFloatStateOf(scale.fractionalStepIndex(state.value).toFloat())
    }
    val animatable = remember(state) { Animatable(offsetSteps) }
    val interaction = remember(state) { InteractionState() }
    val animationJob = remember(state) { JobHolder() }
    val hapticState = remember(state) { HapticState() }
    val paints = remember { RulerPaintCache() }

    fun beginInteraction(): Int {
        interaction.generation++
        interaction.active = true
        return interaction.generation
    }

    fun endInteraction(generation: Int) {
        if (interaction.generation == generation) {
            interaction.active = false
        }
    }

    LaunchedEffect(animatable, totalStepsF) {
        animatable.updateBounds(0f, totalStepsF)
    }

    LaunchedEffect(animatable) {
        snapshotFlow { animatable.value }.collect {
            offsetSteps = it
        }
    }

    LaunchedEffect(enabled, enableHaptics, hapticMode, dimensions.hapticThrottleMs) {
        snapshotFlow { offsetSteps }.collect {
            val rawValue = scale.valueForFractionalStep(offsetSteps.toDouble())
            val newValue = if (state.snapToStep) scale.snap(rawValue) else rawValue
            if (abs(newValue - state.value) < VALUE_EPSILON) return@collect
            state.updateFromInteraction(newValue)
            currentOnValueChange?.invoke(newValue)
            if (enabled && enableHaptics && hapticMode != RulerHapticMode.NONE) {
                val stepIndex = scale.stepIndexFor(newValue)
                if (stepIndex == hapticState.lastStep) return@collect
                hapticState.lastStep = stepIndex
                if (hapticMode == RulerHapticMode.MAJOR_STEP && !scale.isMajorStep(stepIndex)) {
                    return@collect
                }
                val now = android.os.SystemClock.uptimeMillis()
                if (now - hapticState.lastTime < dimensions.hapticThrottleMs) return@collect
                hapticState.lastTime = now
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    LaunchedEffect(state.externalChangeVersion, dimensions.animationDuration) {
        if (interaction.active) return@LaunchedEffect
        val target = scale.fractionalStepIndex(state.value).toFloat()
        if (abs(offsetSteps - target) < VALUE_EPSILON) return@LaunchedEffect
        val generation = beginInteraction()
        try {
            animatable.snapTo(offsetSteps)
            animatable.animateTo(target, tween(dimensions.animationDuration))
            offsetSteps = animatable.value
            currentOnValueChangeFinished?.invoke()
        } finally {
            endInteraction(generation)
        }
    }

    suspend fun settle(generation: Int) {
        try {
            animatable.snapTo(offsetSteps)
            if (state.snapToStep) {
                val target = offsetSteps.roundToLong().coerceIn(0L, totalSteps).toFloat()
                if (abs(target - offsetSteps) > 1e-4f) {
                    animatable.animateTo(target, tween(dimensions.animationDuration))
                }
            }
            offsetSteps = animatable.value
            currentOnValueChangeFinished?.invoke()
        } finally {
            endInteraction(generation)
        }
    }

    suspend fun fling(initialVelocitySteps: Float, generation: Int) {
        try {
            animatable.snapTo(offsetSteps)
            animatable.animateDecay(
                initialVelocity = initialVelocitySteps,
                animationSpec = exponentialDecay(frictionMultiplier = 2.2f),
            )
            offsetSteps = animatable.value
            settle(generation)
        } finally {
            endInteraction(generation)
        }
    }

    val defaultWidth = if (orientation == RulerOrientation.HORIZONTAL) {
        dimensions.minimumLength
    } else {
        dimensions.preferredWidth
    }
    val defaultHeight = if (orientation == RulerOrientation.HORIZONTAL) {
        dimensions.preferredHeight
    } else {
        dimensions.minimumLength
    }

    BoxWithConstraints(modifier) {
        val targetWidth = if (constraints.hasBoundedWidth) maxWidth else defaultWidth
        val targetHeight = if (constraints.hasBoundedHeight) maxHeight else defaultHeight
        val horizontal = orientation == RulerOrientation.HORIZONTAL
        val fading = showFadingEdges && dimensions.fadeWidth > 0.dp

        Canvas(
            modifier = Modifier
                .size(targetWidth, targetHeight)
                .clipToBounds()
                .then(
                    if (fading) {
                        Modifier.graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                    } else {
                        Modifier
                    },
                )
                .pointerInput(
                    state,
                    enabled,
                    readOnly,
                    orientation,
                    reversed,
                    layoutDirection,
                    tickSpacingPx,
                    enableFling,
                    enableHaptics,
                    hapticMode,
                    dimensions.animationDuration,
                    dimensions.hapticThrottleMs,
                ) {
                    animationJob.job?.cancel()
                    interaction.generation++
                    interaction.active = false
                    if (!enabled || readOnly) return@pointerInput
                    var gestureGeneration: Int? = null
                    var animationLaunched = false
                    try {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            var lastPosition = down.position
                            var totalDrag = 0f
                            var dragging = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break

                                val delta = if (horizontal) {
                                    change.position.x - lastPosition.x
                                } else {
                                    change.position.y - lastPosition.y
                                }
                                lastPosition = change.position
                                tracker.addPosition(change.uptimeMillis, change.position)
                                totalDrag += delta

                                if (!dragging && abs(totalDrag) > viewConfiguration.touchSlop) {
                                    dragging = true
                                    gestureGeneration = beginInteraction()
                                    animationJob.job?.cancel()
                                }
                                if (dragging) {
                                    change.consume()
                                    offsetSteps = (
                                        offsetSteps -
                                            delta / tickSpacingPx * directionSign
                                        ).coerceIn(0f, totalStepsF)
                                }
                            }

                            if (dragging) {
                                val generation = gestureGeneration ?: beginInteraction()
                                val velocity = tracker.calculateVelocity()
                                val velocitySteps = -(
                                    if (horizontal) velocity.x else velocity.y
                                    ) / tickSpacingPx * directionSign
                                animationLaunched = true
                                animationJob.job = scope.launch {
                                    if (
                                        enableFling &&
                                        abs(velocitySteps) * tickSpacingPx >
                                        MIN_FLING_VELOCITY_PX
                                    ) {
                                        fling(velocitySteps, generation)
                                    } else {
                                        settle(generation)
                                    }
                                }
                            } else {
                                val generation = beginInteraction()
                                val length = if (horizontal) {
                                    size.width.toFloat()
                                } else {
                                    size.height.toFloat()
                                }
                                val position = if (horizontal) {
                                    down.position.x
                                } else {
                                    down.position.y
                                }
                                val target = (
                                    offsetSteps +
                                        (position - length / 2f) / tickSpacingPx * directionSign
                                    ).coerceIn(0f, totalStepsF)
                                animationLaunched = true
                                animationJob.job = scope.launch {
                                    try {
                                        animatable.snapTo(offsetSteps)
                                        animatable.animateTo(
                                            if (state.snapToStep) {
                                                target
                                                    .roundToLong()
                                                    .coerceIn(0L, totalSteps)
                                                    .toFloat()
                                            } else {
                                                target
                                            },
                                            tween(dimensions.animationDuration),
                                        )
                                        offsetSteps = animatable.value
                                        currentOnValueChangeFinished?.invoke()
                                    } finally {
                                        endInteraction(generation)
                                    }
                                }
                            }
                        }
                    } finally {
                        val generation = gestureGeneration
                        if (generation != null && !animationLaunched) {
                            endInteraction(generation)
                        }
                    }
                }
                .semantics {
                    val description = contentDescription
                    if (description != null) {
                        this.contentDescription = description
                    }
                    stateDescription = valueFormatter?.invoke(state.value)
                        ?: state.formattedValue
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = state.value.toFloat(),
                        range = state.minValue.toFloat()..scale.effectiveMaxValue.toFloat(),
                        steps = if (state.snapToStep) {
                            (totalSteps - 1L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                        } else {
                            0
                        },
                    )
                    if (!enabled) disabled()
                    if (enabled && !readOnly) {
                        setProgress { target ->
                            state.setValue(target.toDouble())
                            true
                        }
                        customActions = listOf(
                            CustomAccessibilityAction("Increase value") {
                                state.scrollBy(1L)
                                true
                            },
                            CustomAccessibilityAction("Decrease value") {
                                state.scrollBy(-1L)
                                true
                            },
                        )
                    }
                },
        ) {
            val valueText = valueFormatter?.invoke(state.value) ?: state.formattedValue
            RulerDrawer(
                drawScope = this,
                state = state,
                visuals = visuals,
                enabled = enabled,
                value = state.value,
                valueText = valueText,
                paints = paints,
                offsetSteps = offsetSteps,
                directionSign = directionSign,
            ).draw()
        }
    }
}

@Immutable
private data class RulerVisuals(
    val orientation: RulerOrientation,
    val reversed: Boolean,
    val tickShape: RulerTickShape,
    val labelPosition: RulerLabelPosition,
    val indicatorStyle: RulerIndicatorStyle,
    val valueDisplayMode: RulerValueDisplayMode,
    val showMinorTicks: Boolean,
    val showMajorTicks: Boolean,
    val showLabels: Boolean,
    val showSelectedLabel: Boolean,
    val showSelectedTick: Boolean,
    val showBaseline: Boolean,
    val showIndicator: Boolean,
    val showValue: Boolean,
    val showFadingEdges: Boolean,
    val colors: RulerValuePickerColors,
    val dimensions: RulerValuePickerDimensions,
) {
    fun effectiveValueMode(): RulerValueDisplayMode {
        if (!showValue) return RulerValueDisplayMode.NONE
        if (orientation == RulerOrientation.VERTICAL) {
            return when (valueDisplayMode) {
                RulerValueDisplayMode.BUBBLE -> RulerValueDisplayMode.HEADER
                RulerValueDisplayMode.HEADER -> RulerValueDisplayMode.HEADER
                RulerValueDisplayMode.NONE -> {
                    if (indicatorStyle == RulerIndicatorStyle.BUBBLE) {
                        RulerValueDisplayMode.HEADER
                    } else {
                        RulerValueDisplayMode.NONE
                    }
                }
            }
        }
        if (valueDisplayMode != RulerValueDisplayMode.NONE) return valueDisplayMode
        if (indicatorStyle == RulerIndicatorStyle.BUBBLE) return RulerValueDisplayMode.BUBBLE
        return RulerValueDisplayMode.NONE
    }
}

private class RulerPaintCache {
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val label = Paint(Paint.ANTI_ALIAS_FLAG)
    val value = Paint(Paint.ANTI_ALIAS_FLAG)
    val header = Paint(Paint.ANTI_ALIAS_FLAG)
    val container = Paint(Paint.ANTI_ALIAS_FLAG)
}

private class JobHolder(var job: Job? = null)

private class HapticState(var lastStep: Long = Long.MIN_VALUE, var lastTime: Long = 0L)

private class InteractionState {
    var active = false
    var generation = 0
}

private class RulerDrawer(
    private val drawScope: DrawScope,
    private val state: RulerValuePickerState,
    private val visuals: RulerVisuals,
    private val enabled: Boolean,
    private val value: Double,
    private val valueText: String,
    private val paints: RulerPaintCache,
    private val offsetSteps: Float,
    private val directionSign: Float,
) {

    private val scale: RulerScale = state.scale
    private val canvas = drawScope.drawContext.canvas.nativeCanvas
    private val width = drawScope.size.width
    private val height = drawScope.size.height

    private val tickSpacingPx = with(drawScope) { visuals.dimensions.tickSpacing.toPx() }
    private val minLabelSpacingPx = with(drawScope) { visuals.dimensions.minLabelSpacing.toPx() }
    private val fadeWidthPx = with(drawScope) { visuals.dimensions.fadeWidth.toPx() }

    private val minorTickWidth = with(drawScope) { visuals.dimensions.minorTickWidth.toPx() }
    private val minorTickHeight = with(drawScope) { visuals.dimensions.minorTickHeight.toPx() }
    private val majorTickWidth = with(drawScope) { visuals.dimensions.majorTickWidth.toPx() }
    private val majorTickHeight = with(drawScope) { visuals.dimensions.majorTickHeight.toPx() }
    private val selectedTickWidth = with(drawScope) { visuals.dimensions.selectedTickWidth.toPx() }
    private val selectedTickHeight = with(drawScope) { visuals.dimensions.selectedTickHeight.toPx() }
    private val tickCornerRadius = with(drawScope) { visuals.dimensions.tickCornerRadius.toPx() }
    private val dotRadius = with(drawScope) { visuals.dimensions.dotRadius.toPx() }
    private val labelTextSizePx = with(drawScope) { visuals.dimensions.labelTextSize.toPx() }
    private val labelGap = with(drawScope) { visuals.dimensions.labelGap.toPx() }
    private val alternateLabelGap = with(drawScope) { visuals.dimensions.alternateLabelGap.toPx() }
    private val valueTextSizePx = with(drawScope) { visuals.dimensions.valueTextSize.toPx() }
    private val headerTextSizePx = with(drawScope) { visuals.dimensions.headerTextSize.toPx() }
    private val headerGap = with(drawScope) { visuals.dimensions.headerGap.toPx() }
    private val indicatorWidth = with(drawScope) { visuals.dimensions.indicatorWidth.toPx() }
    private val indicatorHeight = with(drawScope) { visuals.dimensions.indicatorHeight.toPx() }
    private val triangleWidth = with(drawScope) { visuals.dimensions.triangleWidth.toPx() }
    private val triangleHeight = with(drawScope) { visuals.dimensions.triangleHeight.toPx() }
    private val bubbleCornerRadius = with(drawScope) {
        visuals.dimensions.bubbleCornerRadius.toPx()
    }
    private val bubbleHorizontalPadding = with(drawScope) {
        visuals.dimensions.bubbleHorizontalPadding.toPx()
    }
    private val bubbleVerticalPadding = with(drawScope) {
        visuals.dimensions.bubbleVerticalPadding.toPx()
    }
    private val bubbleGap = with(drawScope) { visuals.dimensions.bubbleGap.toPx() }
    private val baselineWidth = with(drawScope) { visuals.dimensions.baselineWidth.toPx() }

    private val mode = visuals.effectiveValueMode()
    private val labels = visuals.showLabels && visuals.labelPosition != RulerLabelPosition.NONE
    private val labelSkip = scale.labelSkipFactor(tickSpacingPx, minLabelSpacingPx)
    private val selectedIndex = scale.stepIndexFor(value)

    fun draw() {
        paints.container.color = visuals.colors.containerColor(enabled).toArgb()
        if (android.graphics.Color.alpha(paints.container.color) > 0) {
            canvas.drawRect(0f, 0f, width, height, paints.container)
        }

        if (visuals.orientation == RulerOrientation.HORIZONTAL) {
            drawHorizontal()
        } else {
            drawVertical()
        }

        if (visuals.showFadingEdges) {
            drawFadeMask()
        }
    }

    private fun drawHorizontal() {
        val centerX = width / 2f
        val contentHeight = horizontalContentHeight()
        var cursor = ((height - contentHeight) / 2f).coerceAtLeast(0f)

        if (mode == RulerValueDisplayMode.HEADER) {
            drawHeader(centerX, cursor)
            cursor += headerTextHeight() + headerGap
        }

        var bubbleRect: Rect? = null
        if (mode == RulerValueDisplayMode.BUBBLE) {
            bubbleRect = computeBubbleRect(centerX, cursor)
            cursor += bubbleRect.height + bubbleGap
        }

        val labelHeight = labelTextHeight()
        var labelAreaTop = cursor
        if (labels && visuals.labelPosition != RulerLabelPosition.BELOW) {
            labelAreaTop = cursor
            cursor += if (visuals.labelPosition == RulerLabelPosition.ALTERNATE) {
                labelHeight * 2f + alternateLabelGap + labelGap
            } else {
                labelHeight + labelGap
            }
        }

        val tickTop = cursor
        val tickBottom = tickTop + tickAreaHeight()
        val labelTopBelow = tickBottom + labelGap

        if (visuals.showBaseline) {
            paints.stroke.color = visuals.colors.baselineColor(enabled).toArgb()
            paints.stroke.strokeWidth = baselineWidth
            canvas.drawLine(0f, tickBottom, width, tickBottom, paints.stroke)
        }

        val labelsBelow = labels && visuals.labelPosition == RulerLabelPosition.BELOW
        val centerIndex = offsetSteps
        val halfSpan = width / 2f / tickSpacingPx + 2f
        val start = floor(centerIndex - halfSpan).toLong()
        val end = ceil(centerIndex + halfSpan).toLong()

        for (index in start..end) {
            if (index < 0L || index > scale.totalSteps) continue
            val x = centerX + (index - centerIndex) * tickSpacingPx * directionSign
            if (x < -tickSpacingPx || x > width + tickSpacingPx) continue

            val isMajor = scale.isMajorStep(index)
            val isSelected = index == selectedIndex && visuals.showSelectedTick
            if (!drawTickHorizontal(x, tickBottom, isMajor, isSelected)) continue

            if (labels && scale.showsLabel(index, labelSkip) && x >= 0f && x <= width) {
                paints.label.textSize = labelTextSizePx
                paints.label.textAlign = Paint.Align.CENTER
                val selected = visuals.showSelectedLabel && index == selectedIndex
                paints.label.color = visuals.colors.labelColor(selected, enabled).toArgb()
                val baseline = when {
                    labelsBelow -> labelTopBelow - paints.label.fontMetrics.top
                    visuals.labelPosition == RulerLabelPosition.ALTERNATE -> {
                        val row = if ((index / scale.majorEvery) % 2L == 0L) 0 else 1
                        val rowTop = labelAreaTop + row * (labelHeight + alternateLabelGap)
                        rowTop - paints.label.fontMetrics.top
                    }

                    else -> labelAreaTop - paints.label.fontMetrics.top
                }
                canvas.drawText(scale.tickLabel(index), x, baseline, paints.label)
            }
        }

        drawIndicatorHorizontal(centerX, tickBottom)

        if (bubbleRect != null) {
            drawBubble(centerX, bubbleRect)
        }
    }

    private fun drawVertical() {
        var cursor = 0f
        if (mode == RulerValueDisplayMode.HEADER) {
            drawHeader(width / 2f, cursor)
            cursor += headerTextHeight() + headerGap
        }

        val tickLength = tickAreaHeight()
        val contentWidth = verticalContentWidth()
        val startX = ((width - contentWidth) / 2f).coerceAtLeast(0f)
        val labelsOnRight = visuals.labelPosition == RulerLabelPosition.BELOW
        val sign = if (labelsOnRight) 1f else -1f
        val baselineX = if (labelsOnRight) startX else startX + contentWidth
        val labelX = baselineX + sign * (tickLength + labelGap)
        val centerY = (cursor + height) / 2f

        if (visuals.showBaseline) {
            paints.stroke.color = visuals.colors.baselineColor(enabled).toArgb()
            paints.stroke.strokeWidth = baselineWidth
            canvas.drawLine(baselineX, cursor, baselineX, height, paints.stroke)
        }

        val centerIndex = offsetSteps
        val halfSpan = (height - cursor) / 2f / tickSpacingPx + 2f
        val start = floor(centerIndex - halfSpan).toLong()
        val end = ceil(centerIndex + halfSpan).toLong()

        for (index in start..end) {
            if (index < 0L || index > scale.totalSteps) continue
            val y = centerY + (index - centerIndex) * tickSpacingPx * directionSign
            if (y < cursor - tickSpacingPx || y > height + tickSpacingPx) continue

            val isMajor = scale.isMajorStep(index)
            val isSelected = index == selectedIndex && visuals.showSelectedTick
            if (!drawTickVertical(baselineX, y, sign, isMajor, isSelected)) continue

            if (labels && scale.showsLabel(index, labelSkip) && y >= cursor && y <= height) {
                paints.label.textSize = labelTextSizePx
                paints.label.textAlign = if (labelsOnRight) Paint.Align.LEFT else Paint.Align.RIGHT
                val selected = visuals.showSelectedLabel && index == selectedIndex
                paints.label.color = visuals.colors.labelColor(selected, enabled).toArgb()
                var x = labelX
                if (visuals.labelPosition == RulerLabelPosition.ALTERNATE) {
                    val row = if ((index / scale.majorEvery) % 2L == 0L) 0 else 1
                    x += sign * row * (alternateLabelGap + labelTextHeight())
                }
                val baseline = y -
                    (paints.label.fontMetrics.ascent + paints.label.fontMetrics.descent) / 2f
                canvas.drawText(scale.tickLabel(index), x, baseline, paints.label)
            }
        }

        drawIndicatorVertical(centerY, baselineX, sign)
    }

    private fun drawTickHorizontal(
        x: Float,
        baseline: Float,
        isMajor: Boolean,
        isSelected: Boolean,
    ): Boolean {
        if (isMajor && !visuals.showMajorTicks) return false
        if (!isMajor && !visuals.showMinorTicks) return false

        val color = visuals.colors.tickColor(isMajor, isSelected, enabled).toArgb()
        val strokeWidth = when {
            isSelected -> selectedTickWidth
            isMajor -> majorTickWidth
            else -> minorTickWidth
        }
        val tickHeight = when {
            isSelected -> selectedTickHeight
            isMajor -> majorTickHeight
            else -> minorTickHeight
        }

        when (visuals.tickShape) {
            RulerTickShape.LINE -> {
                paints.stroke.color = color
                paints.stroke.strokeWidth = strokeWidth
                canvas.drawLine(x, baseline - tickHeight, x, baseline, paints.stroke)
            }

            RulerTickShape.ROUNDED_LINE -> {
                paints.fill.color = color
                canvas.drawRoundRect(
                    x - strokeWidth / 2f,
                    baseline - tickHeight,
                    x + strokeWidth / 2f,
                    baseline,
                    tickCornerRadius,
                    tickCornerRadius,
                    paints.fill,
                )
            }

            RulerTickShape.DOT -> {
                paints.fill.color = color
                val radius = if (isMajor) max(dotRadius, strokeWidth) else dotRadius
                canvas.drawCircle(x, baseline - radius, radius, paints.fill)
            }
        }
        return true
    }

    private fun drawTickVertical(
        baselineX: Float,
        y: Float,
        sign: Float,
        isMajor: Boolean,
        isSelected: Boolean,
    ): Boolean {
        if (isMajor && !visuals.showMajorTicks) return false
        if (!isMajor && !visuals.showMinorTicks) return false

        val color = visuals.colors.tickColor(isMajor, isSelected, enabled).toArgb()
        val strokeWidth = when {
            isSelected -> selectedTickWidth
            isMajor -> majorTickWidth
            else -> minorTickWidth
        }
        val tickLength = when {
            isSelected -> selectedTickHeight
            isMajor -> majorTickHeight
            else -> minorTickHeight
        }

        when (visuals.tickShape) {
            RulerTickShape.LINE -> {
                paints.stroke.color = color
                paints.stroke.strokeWidth = strokeWidth
                canvas.drawLine(baselineX, y, baselineX + sign * tickLength, y, paints.stroke)
            }

            RulerTickShape.ROUNDED_LINE -> {
                paints.fill.color = color
                val leftX = minOf(baselineX, baselineX + sign * tickLength)
                val rightX = maxOf(baselineX, baselineX + sign * tickLength)
                canvas.drawRoundRect(
                    leftX,
                    y - strokeWidth / 2f,
                    rightX,
                    y + strokeWidth / 2f,
                    tickCornerRadius,
                    tickCornerRadius,
                    paints.fill,
                )
            }

            RulerTickShape.DOT -> {
                paints.fill.color = color
                val radius = if (isMajor) max(dotRadius, strokeWidth) else dotRadius
                canvas.drawCircle(baselineX + sign * radius, y, radius, paints.fill)
            }
        }
        return true
    }

    private fun drawIndicatorHorizontal(centerX: Float, baseline: Float) {
        if (!visuals.showIndicator || visuals.indicatorStyle == RulerIndicatorStyle.NONE) return
        paints.stroke.color = visuals.colors.indicatorColor(enabled).toArgb()
        paints.stroke.strokeWidth = indicatorWidth
        canvas.drawLine(
            centerX,
            baseline - indicatorHeight,
            centerX,
            baseline + indicatorWidth,
            paints.stroke,
        )
        if (visuals.indicatorStyle == RulerIndicatorStyle.TRIANGLE) {
            val path = Path().apply {
                moveTo(centerX - triangleWidth / 2f, baseline - triangleHeight)
                lineTo(centerX + triangleWidth / 2f, baseline - triangleHeight)
                lineTo(centerX, baseline)
                close()
            }
            drawScope.drawPath(path, visuals.colors.indicatorColor(enabled))
        }
    }

    private fun drawIndicatorVertical(centerY: Float, baselineX: Float, sign: Float) {
        if (!visuals.showIndicator || visuals.indicatorStyle == RulerIndicatorStyle.NONE) return
        paints.stroke.color = visuals.colors.indicatorColor(enabled).toArgb()
        paints.stroke.strokeWidth = indicatorWidth
        canvas.drawLine(
            baselineX,
            centerY,
            baselineX + sign * indicatorHeight,
            centerY,
            paints.stroke,
        )
        if (visuals.indicatorStyle == RulerIndicatorStyle.TRIANGLE) {
            val path = Path().apply {
                moveTo(baselineX + sign * triangleHeight, centerY - triangleWidth / 2f)
                lineTo(baselineX + sign * triangleHeight, centerY + triangleWidth / 2f)
                lineTo(baselineX, centerY)
                close()
            }
            drawScope.drawPath(path, visuals.colors.indicatorColor(enabled))
        }
    }

    private fun drawHeader(centerX: Float, top: Float) {
        paints.header.textSize = headerTextSizePx
        paints.header.textAlign = Paint.Align.CENTER
        paints.header.color = visuals.colors.headerTextColor(enabled).toArgb()
        val baseline = top - paints.header.fontMetrics.top
        canvas.drawText(valueText, centerX, baseline, paints.header)
    }

    private fun computeBubbleRect(centerX: Float, top: Float): Rect {
        paints.value.textSize = valueTextSizePx
        val textWidth = paints.value.measureText(valueText)
        val textHeight = valueTextHeight()
        val bubbleWidth = (textWidth + bubbleHorizontalPadding * 2f).coerceAtMost(width)
        val bubbleHeight = textHeight + bubbleVerticalPadding * 2f
        return Rect(
            left = centerX - bubbleWidth / 2f,
            top = top,
            right = centerX + bubbleWidth / 2f,
            bottom = top + bubbleHeight,
        )
    }

    private fun drawBubble(centerX: Float, rect: Rect) {
        paints.fill.color = visuals.colors.bubbleColor(enabled).toArgb()
        canvas.drawRoundRect(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            bubbleCornerRadius,
            bubbleCornerRadius,
            paints.fill,
        )
        val path = Path().apply {
            moveTo(centerX - triangleWidth / 2f, rect.bottom - 1f)
            lineTo(centerX + triangleWidth / 2f, rect.bottom - 1f)
            lineTo(centerX, rect.bottom + triangleHeight)
            close()
        }
        drawScope.drawPath(path, visuals.colors.bubbleColor(enabled))

        paints.value.textSize = valueTextSizePx
        paints.value.textAlign = Paint.Align.CENTER
        paints.value.color = visuals.colors.bubbleTextColor(enabled).toArgb()
        val baseline = rect.center.y -
            (paints.value.fontMetrics.ascent + paints.value.fontMetrics.descent) / 2f
        canvas.drawText(valueText, centerX, baseline, paints.value)
    }

    private fun drawFadeMask() {
        val horizontal = visuals.orientation == RulerOrientation.HORIZONTAL
        val length = if (horizontal) width else height
        if (length <= 0f || fadeWidthPx <= 0f) return
        val stop = (fadeWidthPx / length).coerceIn(0f, 0.5f)
        val brush = if (horizontal) {
            Brush.horizontalGradient(
                0f to Color.Transparent,
                stop to Color.Black,
                1f - stop to Color.Black,
                1f to Color.Transparent,
            )
        } else {
            Brush.verticalGradient(
                0f to Color.Transparent,
                stop to Color.Black,
                1f - stop to Color.Black,
                1f to Color.Transparent,
            )
        }
        drawScope.drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

    private fun horizontalContentHeight(): Float {
        var result = 0f
        if (mode == RulerValueDisplayMode.HEADER) {
            result += headerTextHeight() + headerGap
        }
        if (mode == RulerValueDisplayMode.BUBBLE) {
            result += valueTextHeight() + bubbleVerticalPadding * 2f + bubbleGap
        }
        if (labels && visuals.labelPosition != RulerLabelPosition.BELOW) {
            result += labelTextHeight() + labelGap
            if (visuals.labelPosition == RulerLabelPosition.ALTERNATE) {
                result += alternateLabelGap + labelTextHeight()
            }
        }
        result += tickAreaHeight()
        if (labels && visuals.labelPosition == RulerLabelPosition.BELOW) {
            result += labelGap + labelTextHeight()
        }
        return result
    }

    private fun verticalContentWidth(): Float {
        var result = tickAreaHeight()
        if (labels) {
            result += labelGap + labelTextHeight() * 3f
            if (visuals.labelPosition == RulerLabelPosition.ALTERNATE) {
                result += alternateLabelGap + labelTextHeight() * 1.5f
            }
        }
        return result
    }

    private fun activeIndicatorHeight(): Float =
        if (visuals.showIndicator && visuals.indicatorStyle != RulerIndicatorStyle.NONE) {
            indicatorHeight
        } else {
            0f
        }

    private fun tickAreaHeight(): Float = max(
        max(majorTickHeight, selectedTickHeight),
        activeIndicatorHeight(),
    )

    private fun labelTextHeight(): Float {
        paints.label.textSize = labelTextSizePx
        return paints.label.fontMetrics.descent - paints.label.fontMetrics.ascent
    }

    private fun valueTextHeight(): Float {
        paints.value.textSize = valueTextSizePx
        return paints.value.fontMetrics.descent - paints.value.fontMetrics.ascent
    }

    private fun headerTextHeight(): Float {
        paints.header.textSize = headerTextSizePx
        return paints.header.fontMetrics.descent - paints.header.fontMetrics.ascent
    }
}

private const val MIN_FLING_VELOCITY_PX = 60f
private const val VALUE_EPSILON = 1e-9

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RulerValuePickerPreview() {
    val state = rememberRulerValuePickerState(
        minValue = 150.0,
        maxValue = 210.0,
        step = 1.0,
        majorStep = 10.0,
        initialValue = 160.0,
    )
    RulerValuePicker(
        state = state,
        modifier = Modifier
            .padding(16.dp)
            .size(width = 320.dp, height = 120.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RulerValuePickerReversedPreview() {
    val state = rememberRulerValuePickerState(
        minValue = 150.0,
        maxValue = 210.0,
        step = 1.0,
        majorStep = 10.0,
        initialValue = 180.0,
    )
    RulerValuePicker(
        state = state,
        modifier = Modifier
            .padding(16.dp)
            .size(width = 320.dp, height = 120.dp),
        reversed = true,
        labelPosition = RulerLabelPosition.BELOW,
        indicatorStyle = RulerIndicatorStyle.TRIANGLE,
        tickShape = RulerTickShape.ROUNDED_LINE,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RulerValuePickerVerticalPreview() {
    val state = rememberRulerValuePickerState(
        minValue = 0.0,
        maxValue = 100.0,
        step = 1.0,
        majorStep = 10.0,
        initialValue = 40.0,
        unit = "kg",
    )
    RulerValuePicker(
        state = state,
        modifier = Modifier
            .padding(16.dp)
            .size(width = 140.dp, height = 280.dp),
        orientation = RulerOrientation.VERTICAL,
        tickShape = RulerTickShape.DOT,
        labelPosition = RulerLabelPosition.BELOW,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RulerValuePickerAlternatePreview() {
    val state = rememberRulerValuePickerState(
        minValue = 0.0,
        maxValue = 100.0,
        step = 0.5,
        majorStep = 5.0,
        initialValue = 42.5,
        decimals = 1,
    )
    RulerValuePicker(
        state = state,
        modifier = Modifier
            .padding(16.dp)
            .size(width = 360.dp, height = 140.dp),
        labelPosition = RulerLabelPosition.ALTERNATE,
        indicatorStyle = RulerIndicatorStyle.BUBBLE,
        valueDisplayMode = RulerValueDisplayMode.HEADER,
        colors = RulerValuePickerDefaults.colors(
            bubbleColor = Color(0xFF00695C),
            bubbleTextColor = Color.White,
        ),
        dimensions = RulerValuePickerDefaults.dimensions(
            tickSpacing = 12.dp,
            fadeWidth = 32.dp,
        ),
    )
}
