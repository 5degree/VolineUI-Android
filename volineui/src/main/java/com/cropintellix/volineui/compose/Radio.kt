@file:Suppress("unused")

package com.cropintellix.volineui.compose

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.cropintellix.volineui.radio.RadioColors
import com.cropintellix.volineui.radio.RadioDefaults
import com.cropintellix.volineui.radio.RadioOptionsException
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Modern radio/segmented control component with sliding pill indicator for Jetpack Compose.
 * 
 * Features:
 * - Horizontal layout with multiple selectable options
 * - Animated sliding pill indicator with 3D effect
 * - Dynamic color customization for selected/unselected states
 * - Touch and swipe gesture support
 * - Haptic feedback
 * - Disabled states (global and per-option)
 * - Label support
 * - Programmatic configuration
 * 
 * Requires minimum 2 options, throws RadioOptionsException otherwise.
 *
 * @param options List of options to display (minimum 2 required)
 * @param selectedIndex Currently selected index
 * @param onSelectedIndexChange Callback when selection changes, provides the new index
 * @param modifier Modifier for the component
 * @param label Optional label text displayed above the component
 * @param enabled Whether the entire component is enabled
 * @param disabledOptions Set of indices that should be disabled
 * @param colors Color configuration for the component
 * @param height Height of the radio container
 * @param gap Gap between container edge and pill
 * @param optionSpacing Spacing between options
 * @param cornerRadius Corner radius of container and pill
 * @param pillElevation Elevation of the sliding pill
 * @param enableHapticFeedback Whether to provide haptic feedback on selection
 * @param enableSwipeGesture Whether to enable swipe gesture for selection
 * @param animationDuration Duration of animations in milliseconds
 * @param labelGap Gap between label and container
 * @param labelTextSize Text size for the label
 * @param textSize Text size for options
 */
@Composable
fun Radio(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    disabledOptions: Set<Int> = emptySet(),
    colors: RadioColors = RadioDefaults.colors(),
    height: Dp = RadioDefaults.Height,
    gap: Dp = RadioDefaults.Gap,
    optionSpacing: Dp = RadioDefaults.OptionSpacing,
    cornerRadius: Dp = RadioDefaults.CornerRadius,
    pillElevation: Dp = RadioDefaults.PillElevation,
    enableHapticFeedback: Boolean = true,
    enableSwipeGesture: Boolean = false,
    animationDuration: Int = RadioDefaults.AnimationDuration,
    labelGap: Dp = RadioDefaults.LabelGap,
    labelTextSize: TextUnit = RadioDefaults.LabelTextSize,
    textSize: TextUnit = RadioDefaults.TextSize,
) {
    // Validate options
    require(options.isNotEmpty()) { throw RadioOptionsException.emptyOptions() }
    require(options.size >= 2) { throw RadioOptionsException.insufficientOptions(options.size) }
    
    val view = LocalView.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    // Animation state for pill position
    val animatedPillPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = { t ->
                // Overshoot interpolator effect
                val tension = 1.2f
                val t2 = t - 1.0f
                t2 * t2 * ((tension + 1) * t2 + tension) + 1.0f
            }
        ),
        label = "pillPosition"
    )
    
    // Drag state for swipe gesture
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    
    // Current pill position (either animated or dragged)
    val currentPillPosition = if (isDragging) dragPosition else animatedPillPosition
    
    // Animated text colors
    val textColors = options.indices.map { index ->
        animateColorAsState(
            targetValue = if (index == selectedIndex) colors.selectedTextColor else colors.unselectedTextColor,
            animationSpec = tween(durationMillis = animationDuration / 2),
            label = "textColor$index"
        )
    }
    
    // Convert dimensions to pixels
    with(density) { height.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val optionSpacingPx = with(density) { optionSpacing.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    with(density) { textSize.toPx() }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!enabled) Modifier else Modifier)
    ) {
        // Label
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = labelTextSize,
                    color = colors.labelTextColor
                )
            )
            Spacer(modifier = Modifier.height(labelGap))
        }
        
        // Radio container with canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .then(
                    if (enabled) {
                        Modifier
                            .pointerInput(options.size, selectedIndex, disabledOptions) {
                                detectTapGestures { offset ->
                                    val contentWidth = size.width - gapPx * 2
                                    val availableWidth = contentWidth - (options.size - 1) * optionSpacingPx
                                    val optionWidth = availableWidth / options.size
                                    
                                    val relativeX = offset.x - gapPx
                                    val tappedIndex = (relativeX / (optionWidth + optionSpacingPx)).toInt()
                                        .coerceIn(0, options.size - 1)
                                    
                                    if (tappedIndex != selectedIndex && !disabledOptions.contains(tappedIndex)) {
                                        if (enableHapticFeedback) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                        onSelectedIndexChange(tappedIndex)
                                    }
                                }
                            }
                            .then(
                                if (enableSwipeGesture) {
                                    Modifier.pointerInput(options.size, selectedIndex, disabledOptions) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { offset ->
                                                isDragging = true
                                                dragPosition = selectedIndex.toFloat()
                                            },
                                            onDragEnd = {
                                                val nearestIndex = dragPosition.roundToInt()
                                                    .coerceIn(0, options.size - 1)
                                                if (nearestIndex != selectedIndex && !disabledOptions.contains(nearestIndex)) {
                                                    if (enableHapticFeedback) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                    }
                                                    onSelectedIndexChange(nearestIndex)
                                                }
                                                isDragging = false
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                val contentWidth = size.width - gapPx * 2
                                                val availableWidth = contentWidth - (options.size - 1) * optionSpacingPx
                                                val optionWidth = availableWidth / options.size
                                                
                                                val positionDelta = dragAmount / (optionWidth + optionSpacingPx)
                                                dragPosition = (dragPosition + positionDelta)
                                                    .coerceIn(0f, (options.size - 1).toFloat())
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                    } else Modifier
                )
                .then(if (!enabled) Modifier else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .then(if (!enabled) Modifier else Modifier)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                // Draw container background
                drawRoundRect(
                    color = colors.containerBackgroundColor,
                    topLeft = Offset.Zero,
                    size = Size(canvasWidth, canvasHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
                
                // Calculate option dimensions
                val contentWidth = canvasWidth - gapPx * 2
                val availableWidth = contentWidth - (options.size - 1) * optionSpacingPx
                val optionWidth = availableWidth / options.size
                
                // Draw sliding pill with 3D gradient effect
                val pillLeft = gapPx + currentPillPosition * (optionWidth + optionSpacingPx)
                val pillTop = gapPx
                pillLeft + optionWidth
                val pillBottom = canvasHeight - gapPx
                val pillHeight = pillBottom - pillTop
                val pillCornerRadius = cornerRadiusPx - gapPx
                
                // Create 3D gradient effect
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        lightenColor(colors.selectedBackgroundColor, 0.2f),
                        colors.selectedBackgroundColor,
                        darkenColor(colors.selectedBackgroundColor, 0.1f)
                    ),
                    startY = pillTop,
                    endY = pillBottom
                )
                
                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(optionWidth, pillHeight),
                    cornerRadius = CornerRadius(pillCornerRadius, pillCornerRadius)
                )
                
                // Draw option texts
                options.forEachIndexed { index, option ->
                    val optionLeft = gapPx + index * (optionWidth + optionSpacingPx)
                    val optionCenterX = optionLeft + optionWidth / 2
                    val optionCenterY = canvasHeight / 2
                    
                    val isDisabled = disabledOptions.contains(index)
                    val textColor = if (isDisabled) {
                        textColors[index].value.copy(alpha = 0.4f)
                    } else {
                        textColors[index].value
                    }
                    
                    val textLayoutResult = textMeasurer.measure(
                        text = option,
                        style = TextStyle(
                            fontSize = textSize,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = optionCenterX - textLayoutResult.size.width / 2,
                            y = optionCenterY - textLayoutResult.size.height / 2
                        )
                    )
                }
            }
            
            // Apply disabled alpha overlay
            if (!enabled) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                ) {
                    drawRect(
                        color = Color.White.copy(alpha = 1f - colors.disabledAlpha)
                    )
                }
            }
        }
    }
}

/**
 * Lightens a color by a given factor.
 */
private fun lightenColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * (1 + factor)).coerceAtMost(1f),
        green = (color.green * (1 + factor)).coerceAtMost(1f),
        blue = (color.blue * (1 + factor)).coerceAtMost(1f),
        alpha = color.alpha
    )
}

/**
 * Darkens a color by a given factor.
 */
private fun darkenColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * (1 - factor)).coerceAtLeast(0f),
        green = (color.green * (1 - factor)).coerceAtLeast(0f),
        blue = (color.blue * (1 - factor)).coerceAtLeast(0f),
        alpha = color.alpha
    )
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
private fun RadioPreview() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Radio(
            options = listOf("Admin", "Editor", "Viewer"),
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { selectedIndex = it },
            label = "Select Your Role"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadioCustomColorsPreview() {
    var selectedIndex by remember { mutableIntStateOf(1) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Radio(
            options = listOf("Daily", "Weekly", "Monthly"),
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { selectedIndex = it },
            label = "Preferred Time",
            colors = RadioDefaults.colors(
                selectedBackgroundColor = Color(0xFFFF6B6B),
                selectedTextColor = Color.White,
                unselectedTextColor = Color(0xFF999999),
                containerBackgroundColor = Color(0xFFF0F0F0)
            ),
            height = 52.dp,
            cornerRadius = 26.dp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadioDisabledPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        Radio(
            options = listOf("S", "M", "L"),
            selectedIndex = 1,
            onSelectedIndexChange = { },
            label = "Disabled Radio",
            enabled = false
        )
    }
}
