@file:Suppress("unused")

package `in`.fivedegree.volineui.ruler

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default dimensions, animation timings, and colors for [RulerValuePicker].
 */
object RulerValuePickerDefaults {

    // Layout
    val PreferredHeight: Dp = 108.dp
    val PreferredWidth: Dp = 108.dp
    val MinimumLength: Dp = 240.dp
    val TickSpacing: Dp = 14.dp
    val MinLabelSpacing: Dp = 52.dp
    val FadeWidth: Dp = 28.dp

    // Ticks
    val MinorTickWidth: Dp = 1.dp
    val MinorTickHeight: Dp = 10.dp
    val MajorTickWidth: Dp = 1.5.dp
    val MajorTickHeight: Dp = 20.dp
    val SelectedTickWidth: Dp = 2.dp
    val SelectedTickHeight: Dp = 28.dp
    val TickCornerRadius: Dp = 1.dp
    val DotRadius: Dp = 1.5.dp

    // Text
    val LabelTextSize: TextUnit = 11.sp
    val LabelGap: Dp = 4.dp
    val AlternateLabelGap: Dp = 2.dp
    val ValueTextSize: TextUnit = 20.sp
    val HeaderTextSize: TextUnit = 18.sp
    val HeaderGap: Dp = 6.dp

    // Indicator
    val IndicatorWidth: Dp = 2.5.dp
    val IndicatorHeight: Dp = 36.dp
    val TriangleWidth: Dp = 8.dp
    val TriangleHeight: Dp = 6.dp

    // Bubble
    val BubbleCornerRadius: Dp = 10.dp
    val BubbleHorizontalPadding: Dp = 12.dp
    val BubbleVerticalPadding: Dp = 6.dp
    val BubbleGap: Dp = 6.dp

    // Misc
    val BaselineWidth: Dp = 1.dp
    const val AnimationDuration: Int = 220
    const val HapticThrottleMs: Long = 24L
    const val DisabledAlpha: Float = 0.38f
    const val MaxDecimals: Int = 6
    const val AutoDecimals: Int = -1

    private val DefaultTickColor = Color(0xFFBDBDBD)
    private val DefaultMajorTickColor = Color(0xFF9E9E9E)
    private val DefaultLabelColor = Color(0xFF757575)
    private val DefaultBaselineColor = Color(0xFFE0E0E0)
    private val DefaultHeaderTextColor = Color(0xFF212121)

    /**
     * Creates a [RulerValuePickerColors] instance using theme-aware defaults.
     */
    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        tickColor: Color = DefaultTickColor,
        majorTickColor: Color = DefaultMajorTickColor,
        selectedTickColor: Color = MaterialTheme.colorScheme.primary,
        labelColor: Color = DefaultLabelColor,
        selectedLabelColor: Color = MaterialTheme.colorScheme.primary,
        indicatorColor: Color = MaterialTheme.colorScheme.primary,
        baselineColor: Color = DefaultBaselineColor,
        bubbleColor: Color = MaterialTheme.colorScheme.primary,
        bubbleTextColor: Color = MaterialTheme.colorScheme.onPrimary,
        headerTextColor: Color = DefaultHeaderTextColor,
        disabledAlpha: Float = DisabledAlpha,
    ): RulerValuePickerColors = RulerValuePickerColors(
        containerColor = containerColor,
        tickColor = tickColor,
        majorTickColor = majorTickColor,
        selectedTickColor = selectedTickColor,
        labelColor = labelColor,
        selectedLabelColor = selectedLabelColor,
        indicatorColor = indicatorColor,
        baselineColor = baselineColor,
        bubbleColor = bubbleColor,
        bubbleTextColor = bubbleTextColor,
        headerTextColor = headerTextColor,
        disabledAlpha = disabledAlpha,
    )

    /**
     * Creates a [RulerValuePickerDimensions] instance with optional overrides.
     */
    fun dimensions(
        preferredHeight: Dp = PreferredHeight,
        preferredWidth: Dp = PreferredWidth,
        minimumLength: Dp = MinimumLength,
        tickSpacing: Dp = TickSpacing,
        minLabelSpacing: Dp = MinLabelSpacing,
        fadeWidth: Dp = FadeWidth,
        minorTickWidth: Dp = MinorTickWidth,
        minorTickHeight: Dp = MinorTickHeight,
        majorTickWidth: Dp = MajorTickWidth,
        majorTickHeight: Dp = MajorTickHeight,
        selectedTickWidth: Dp = SelectedTickWidth,
        selectedTickHeight: Dp = SelectedTickHeight,
        tickCornerRadius: Dp = TickCornerRadius,
        dotRadius: Dp = DotRadius,
        labelTextSize: TextUnit = LabelTextSize,
        labelGap: Dp = LabelGap,
        alternateLabelGap: Dp = AlternateLabelGap,
        valueTextSize: TextUnit = ValueTextSize,
        headerTextSize: TextUnit = HeaderTextSize,
        headerGap: Dp = HeaderGap,
        indicatorWidth: Dp = IndicatorWidth,
        indicatorHeight: Dp = IndicatorHeight,
        triangleWidth: Dp = TriangleWidth,
        triangleHeight: Dp = TriangleHeight,
        bubbleCornerRadius: Dp = BubbleCornerRadius,
        bubbleHorizontalPadding: Dp = BubbleHorizontalPadding,
        bubbleVerticalPadding: Dp = BubbleVerticalPadding,
        bubbleGap: Dp = BubbleGap,
        baselineWidth: Dp = BaselineWidth,
        animationDuration: Int = AnimationDuration,
        hapticThrottleMs: Long = HapticThrottleMs,
    ): RulerValuePickerDimensions = RulerValuePickerDimensions(
        preferredHeight = preferredHeight,
        preferredWidth = preferredWidth,
        minimumLength = minimumLength,
        tickSpacing = tickSpacing,
        minLabelSpacing = minLabelSpacing,
        fadeWidth = fadeWidth,
        minorTickWidth = minorTickWidth,
        minorTickHeight = minorTickHeight,
        majorTickWidth = majorTickWidth,
        majorTickHeight = majorTickHeight,
        selectedTickWidth = selectedTickWidth,
        selectedTickHeight = selectedTickHeight,
        tickCornerRadius = tickCornerRadius,
        dotRadius = dotRadius,
        labelTextSize = labelTextSize,
        labelGap = labelGap,
        alternateLabelGap = alternateLabelGap,
        valueTextSize = valueTextSize,
        headerTextSize = headerTextSize,
        headerGap = headerGap,
        indicatorWidth = indicatorWidth,
        indicatorHeight = indicatorHeight,
        triangleWidth = triangleWidth,
        triangleHeight = triangleHeight,
        bubbleCornerRadius = bubbleCornerRadius,
        bubbleHorizontalPadding = bubbleHorizontalPadding,
        bubbleVerticalPadding = bubbleVerticalPadding,
        bubbleGap = bubbleGap,
        baselineWidth = baselineWidth,
        animationDuration = animationDuration,
        hapticThrottleMs = hapticThrottleMs,
    )
}

/**
 * Represents the colors used by [RulerValuePicker] in different states.
 */
@Immutable
data class RulerValuePickerColors(
    val containerColor: Color,
    val tickColor: Color,
    val majorTickColor: Color,
    val selectedTickColor: Color,
    val labelColor: Color,
    val selectedLabelColor: Color,
    val indicatorColor: Color,
    val baselineColor: Color,
    val bubbleColor: Color,
    val bubbleTextColor: Color,
    val headerTextColor: Color,
    val disabledAlpha: Float,
) {
    /**
     * Returns the tick color for the requested tick state.
     */
    fun tickColor(isMajor: Boolean, isSelected: Boolean, enabled: Boolean): Color {
        val color = when {
            isSelected -> selectedTickColor
            isMajor -> majorTickColor
            else -> tickColor
        }
        return color.forEnabled(enabled, disabledAlpha)
    }

    /**
     * Returns the label color for the requested label state.
     */
    fun labelColor(isSelected: Boolean, enabled: Boolean): Color {
        val color = if (isSelected) selectedLabelColor else labelColor
        return color.forEnabled(enabled, disabledAlpha)
    }

    fun indicatorColor(enabled: Boolean): Color =
        indicatorColor.forEnabled(enabled, disabledAlpha)

    fun baselineColor(enabled: Boolean): Color =
        baselineColor.forEnabled(enabled, disabledAlpha)

    fun bubbleColor(enabled: Boolean): Color =
        bubbleColor.forEnabled(enabled, disabledAlpha)

    fun bubbleTextColor(enabled: Boolean): Color =
        bubbleTextColor.forEnabled(enabled, disabledAlpha)

    fun headerTextColor(enabled: Boolean): Color =
        headerTextColor.forEnabled(enabled, disabledAlpha)

    fun containerColor(enabled: Boolean): Color =
        containerColor.forEnabled(enabled, disabledAlpha)
}

/**
 * Represents the dimensions used by [RulerValuePicker].
 */
@Immutable
data class RulerValuePickerDimensions(
    val preferredHeight: Dp,
    val preferredWidth: Dp,
    val minimumLength: Dp,
    val tickSpacing: Dp,
    val minLabelSpacing: Dp,
    val fadeWidth: Dp,
    val minorTickWidth: Dp,
    val minorTickHeight: Dp,
    val majorTickWidth: Dp,
    val majorTickHeight: Dp,
    val selectedTickWidth: Dp,
    val selectedTickHeight: Dp,
    val tickCornerRadius: Dp,
    val dotRadius: Dp,
    val labelTextSize: TextUnit,
    val labelGap: Dp,
    val alternateLabelGap: Dp,
    val valueTextSize: TextUnit,
    val headerTextSize: TextUnit,
    val headerGap: Dp,
    val indicatorWidth: Dp,
    val indicatorHeight: Dp,
    val triangleWidth: Dp,
    val triangleHeight: Dp,
    val bubbleCornerRadius: Dp,
    val bubbleHorizontalPadding: Dp,
    val bubbleVerticalPadding: Dp,
    val bubbleGap: Dp,
    val baselineWidth: Dp,
    val animationDuration: Int,
    val hapticThrottleMs: Long,
)

internal fun Color.forEnabled(enabled: Boolean, disabledAlpha: Float): Color =
    if (enabled) this else copy(alpha = alpha * disabledAlpha)
