@file:Suppress("unused")

package `in`.fivedegree.volineui.ruler

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Immutable scale engine shared by the View and Compose [RulerValuePicker]
 * implementations.
 *
 * Values are tracked with a discrete [Long] step index internally, which avoids
 * floating point drift (for example `0.1 + 0.2` style artifacts) and keeps tick
 * lookup, snapping, and accessibility deterministic.
 *
 * @throws RulerValuePickerException if the supplied configuration is invalid
 */
internal class RulerScale(
    minValue: Double,
    maxValue: Double,
    step: Double,
    majorStep: Double,
    decimals: Int = RulerValuePickerDefaults.AutoDecimals,
) {

    val minValue: Double
    val maxValue: Double
    val step: Double
    val majorStep: Double

    /** Number of decimal places used for display and snap rounding. */
    val decimals: Int

    /** Index of the last reachable step (inclusive). */
    val totalSteps: Long

    /** Highest reachable value (`minValue + totalSteps * step`). */
    val effectiveMaxValue: Double

    /** Number of minor steps between two major ticks. */
    val majorEvery: Long

    private val minDecimal: BigDecimal
    private val stepDecimal: BigDecimal

    init {
        if (!minValue.isFinite() || !maxValue.isFinite() ||
            !step.isFinite() || !majorStep.isFinite()
        ) {
            throw RulerValuePickerException.invalidRange(minValue, maxValue)
        }
        if (minValue >= maxValue) {
            throw RulerValuePickerException.invalidRange(minValue, maxValue)
        }
        if (step <= 0.0) {
            throw RulerValuePickerException.invalidStep(step)
        }
        if (majorStep <= 0.0) {
            throw RulerValuePickerException.invalidMajorStep(majorStep)
        }

        val ratio = majorStep / step
        val roundedRatio = ratio.roundToLong()
        if (roundedRatio < 1L || abs(ratio - roundedRatio) > 1e-6 * max(1.0, ratio)) {
            throw RulerValuePickerException.majorStepNotMultiple(majorStep, step)
        }

        val stepCount = (maxValue - minValue) / step
        if (stepCount < 1.0) {
            throw RulerValuePickerException.invalidRange(minValue, maxValue)
        }
        if (stepCount > MAX_TOTAL_STEPS) {
            throw RulerValuePickerException.tooManySteps(stepCount)
        }

        this.minValue = minValue
        this.maxValue = maxValue
        this.step = step
        this.majorStep = majorStep
        this.majorEvery = roundedRatio

        val resolvedDecimals = if (decimals == RulerValuePickerDefaults.AutoDecimals) {
            maxOf(
                decimalPlacesOf(step),
                decimalPlacesOf(minValue),
                decimalPlacesOf(maxValue),
            ).coerceIn(0, RulerValuePickerDefaults.MaxDecimals)
        } else {
            if (decimals !in 0..RulerValuePickerDefaults.MaxDecimals) {
                throw RulerValuePickerException.invalidDecimals(decimals)
            }
            decimals
        }
        this.decimals = resolvedDecimals

        val steps = floor(stepCount + STEP_EPSILON).toLong().coerceAtLeast(0L)
        totalSteps = steps
        minDecimal = BigDecimal.valueOf(minValue)
        stepDecimal = BigDecimal.valueOf(step)
        effectiveMaxValue = valueForStep(steps)
    }

    /**
     * Clamps [value] into the valid range, mapping `NaN` to [minValue].
     */
    fun coerce(value: Double): Double = when {
        value.isNaN() -> minValue
        value <= minValue -> minValue
        value >= effectiveMaxValue -> effectiveMaxValue
        else -> value
    }

    /**
     * Returns the nearest discrete step index for [value].
     */
    fun stepIndexFor(value: Double): Long {
        val clamped = coerce(value)
        if (clamped <= minValue) return 0L
        if (clamped >= effectiveMaxValue) return totalSteps
        val raw = (clamped - minValue) / step
        return (raw + STEP_EPSILON).roundToLong().coerceIn(0L, totalSteps)
    }

    /**
     * Returns the fractional step index for [value]. Used to position ticks
     * smoothly while dragging, before snap is applied.
     */
    fun fractionalStepIndex(value: Double): Double {
        val clamped = coerce(value)
        return ((clamped - minValue) / step).coerceIn(0.0, totalSteps.toDouble())
    }

    /**
     * Rounds [value] to the nearest step.
     */
    fun snap(value: Double): Double = valueForStep(stepIndexFor(value))

    /**
     * Returns the value at the given discrete [index].
     */
    fun valueForStep(index: Long): Double {
        val safeIndex = index.coerceIn(0L, totalSteps)
        return minDecimal
            .add(stepDecimal.multiply(BigDecimal.valueOf(safeIndex)))
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Returns the value at a fractional [index]. The result is continuous and
     * is not rounded to [decimals].
     */
    fun valueForFractionalStep(index: Double): Double {
        val safeIndex = index.coerceIn(0.0, totalSteps.toDouble())
        return coerce(minValue + safeIndex * step)
    }

    /**
     * Returns `true` when [index] lands on a major tick.
     */
    fun isMajorStep(index: Long): Boolean = index % majorEvery == 0L

    /**
     * Number of major ticks to skip between labels so labels do not overlap.
     *
     * @param tickSpacingPx rendered distance between two minor ticks, in pixels
     * @param minLabelSpacingPx minimum space between two label centers, in pixels
     */
    fun labelSkipFactor(tickSpacingPx: Float, minLabelSpacingPx: Float): Long {
        if (tickSpacingPx <= 0f || minLabelSpacingPx <= 0f) return 1L
        val majorSpacing = majorEvery * tickSpacingPx
        if (majorSpacing <= 0f) return 1L
        return ceil(minLabelSpacingPx / majorSpacing).toLong().coerceAtLeast(1L)
    }

    /**
     * Returns `true` when the major tick at [index] should render a label.
     */
    fun showsLabel(index: Long, labelSkipFactor: Long): Boolean {
        if (!isMajorStep(index)) return false
        val factor = labelSkipFactor.coerceAtLeast(1L)
        return (index / majorEvery) % factor == 0L
    }

    /**
     * Formats [value] using the configured number of decimal places.
     */
    fun format(value: Double): String =
        BigDecimal.valueOf(coerce(value))
            .setScale(decimals, RoundingMode.HALF_UP)
            .toPlainString()

    /**
     * Formats [value] and appends [unit] when it is not blank.
     */
    fun formatWithUnit(value: Double, unit: String?): String {
        val formatted = format(value)
        return if (unit.isNullOrBlank()) formatted else "$formatted $unit"
    }

    /**
     * Converts a value to a text label for the tick at [index].
     */
    fun tickLabel(index: Long): String = format(valueForStep(index))

    private fun decimalPlacesOf(value: Double): Int {
        if (!value.isFinite() || value == 0.0) return 0
        return BigDecimal.valueOf(value)
            .stripTrailingZeros()
            .scale()
            .coerceIn(0, RulerValuePickerDefaults.MaxDecimals)
    }

    private companion object {
        const val STEP_EPSILON = 1e-9
        const val MAX_TOTAL_STEPS = 1_000_000.0
    }
}
