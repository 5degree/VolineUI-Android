@file:Suppress("unused")

package com.cropintellix.volineui.button

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default values and colors for AdvancedButton composable.
 */
object ButtonDefaults {
    
    // ===== DEFAULT DIMENSIONS =====
    val CornerRadius: Dp = 8.dp
    val BorderWidth: Dp = 1.5.dp
    val ElevationNormal: Dp = 2.dp
    val ElevationPressed: Dp = 4.dp
    val IconSpacing: Dp = 8.dp
    val MinWidth: Dp = 64.dp
    
    // ===== SIZE PRESETS =====
    
    data class SizePreset(
        val minHeight: Dp,
        val horizontalPadding: Dp,
        val verticalPadding: Dp,
        val textSize: TextUnit,
        val iconSize: Dp
    )
    
    val SizeXS = SizePreset(
        minHeight = 28.dp,
        horizontalPadding = 8.dp,
        verticalPadding = 4.dp,
        textSize = 12.sp,
        iconSize = 14.dp
    )
    
    val SizeS = SizePreset(
        minHeight = 36.dp,
        horizontalPadding = 12.dp,
        verticalPadding = 8.dp,
        textSize = 13.sp,
        iconSize = 16.dp
    )
    
    val SizeM = SizePreset(
        minHeight = 48.dp,
        horizontalPadding = 20.dp,
        verticalPadding = 14.dp,
        textSize = 15.sp,
        iconSize = 20.dp
    )
    
    val SizeL = SizePreset(
        minHeight = 52.dp,
        horizontalPadding = 20.dp,
        verticalPadding = 14.dp,
        textSize = 16.sp,
        iconSize = 24.dp
    )
    
    val SizeXL = SizePreset(
        minHeight = 60.dp,
        horizontalPadding = 24.dp,
        verticalPadding = 16.dp,
        textSize = 18.sp,
        iconSize = 28.dp
    )
    
    fun getSizePreset(size: ButtonSize): SizePreset = when (size) {
        ButtonSize.XS -> SizeXS
        ButtonSize.S -> SizeS
        ButtonSize.M -> SizeM
        ButtonSize.L -> SizeL
        ButtonSize.XL -> SizeXL
    }
    
    // ===== ANIMATION =====
    const val AnimationDuration: Int = 200
    const val DebounceTime: Long = 300L
    const val DoubleClickTime: Long = 300L
    const val ScaleAmount: Float = 0.96f
    
    // ===== DEFAULT COLORS =====
    private val DefaultBackgroundColor = Color(0xFF6200EE)
    private val DefaultTextColor = Color.White
    private val DefaultBorderColor = Color(0xFF6200EE)
    private val DisabledBackgroundColor = Color(0xFFE0E0E0)
    private val DisabledTextColor = Color(0xFF9E9E9E)
    private val DisabledBorderColor = Color(0xFFBDBDBD)
    private val SuccessColor = Color(0xFF4CAF50)
    private val ErrorColor = Color(0xFFF44336)
    private val RippleColor = Color(0x40FFFFFF)
    private val LoadingColor = Color.White
    
    /**
     * Creates a [ButtonColors] instance with the default colors.
     * Uses the primary color from MaterialTheme if available.
     */
    @Composable
    fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.primary,
        backgroundColorPressed: Color = darkenColor(backgroundColor, 0.15f),
        backgroundColorDisabled: Color = DisabledBackgroundColor,
        textColor: Color = DefaultTextColor,
        textColorPressed: Color = DefaultTextColor,
        textColorDisabled: Color = DisabledTextColor,
        borderColor: Color = MaterialTheme.colorScheme.primary,
        borderColorPressed: Color = darkenColor(borderColor, 0.15f),
        borderColorDisabled: Color = DisabledBorderColor,
        iconColor: Color = textColor,
        iconColorPressed: Color = textColorPressed,
        iconColorDisabled: Color = DisabledTextColor,
        rippleColor: Color = RippleColor,
        loadingColor: Color = LoadingColor,
        successColor: Color = SuccessColor,
        errorColor: Color = ErrorColor,
    ): ButtonColors = ButtonColors(
        backgroundColor = backgroundColor,
        backgroundColorPressed = backgroundColorPressed,
        backgroundColorDisabled = backgroundColorDisabled,
        textColor = textColor,
        textColorPressed = textColorPressed,
        textColorDisabled = textColorDisabled,
        borderColor = borderColor,
        borderColorPressed = borderColorPressed,
        borderColorDisabled = borderColorDisabled,
        iconColor = iconColor,
        iconColorPressed = iconColorPressed,
        iconColorDisabled = iconColorDisabled,
        rippleColor = rippleColor,
        loadingColor = loadingColor,
        successColor = successColor,
        errorColor = errorColor,
    )
    
    /**
     * Creates colors for outlined button style.
     */
    @Composable
    fun outlinedColors(
        backgroundColor: Color = Color.Transparent,
        textColor: Color = MaterialTheme.colorScheme.primary,
        borderColor: Color = MaterialTheme.colorScheme.primary,
    ): ButtonColors = colors(
        backgroundColor = backgroundColor,
        backgroundColorPressed = Color.Transparent,
        textColor = textColor,
        textColorPressed = darkenColor(textColor, 0.15f),
        borderColor = borderColor,
        borderColorPressed = darkenColor(borderColor, 0.15f),
        iconColor = textColor,
        iconColorPressed = darkenColor(textColor, 0.15f),
        rippleColor = Color(0x40000000),
        loadingColor = textColor,
    )
    
    /**
     * Creates colors for text button style.
     */
    @Composable
    fun textColors(
        textColor: Color = MaterialTheme.colorScheme.primary,
    ): ButtonColors = colors(
        backgroundColor = Color.Transparent,
        backgroundColorPressed = Color.Transparent,
        textColor = textColor,
        textColorPressed = darkenColor(textColor, 0.15f),
        borderColor = Color.Transparent,
        iconColor = textColor,
        iconColorPressed = darkenColor(textColor, 0.15f),
        rippleColor = Color(0x40000000),
        loadingColor = textColor,
    )
    
    /**
     * Creates colors for tonal button style.
     */
    @Composable
    fun tonalColors(
        primaryColor: Color = MaterialTheme.colorScheme.primary,
    ): ButtonColors = colors(
        backgroundColor = primaryColor.copy(alpha = 0.15f),
        backgroundColorPressed = primaryColor.copy(alpha = 0.25f),
        textColor = primaryColor,
        textColorPressed = darkenColor(primaryColor, 0.15f),
        borderColor = Color.Transparent,
        iconColor = primaryColor,
        iconColorPressed = darkenColor(primaryColor, 0.15f),
        rippleColor = primaryColor.copy(alpha = 0.25f),
        loadingColor = primaryColor,
    )
    
    /**
     * Creates colors for chip button style.
     */
    @Composable
    fun chipColors(
        primaryColor: Color = MaterialTheme.colorScheme.primary,
    ): ButtonColors = colors(
        backgroundColor = primaryColor.copy(alpha = 0.1f),
        backgroundColorPressed = primaryColor.copy(alpha = 0.2f),
        textColor = primaryColor,
        textColorPressed = darkenColor(primaryColor, 0.15f),
        borderColor = primaryColor,
        iconColor = primaryColor,
        iconColorPressed = darkenColor(primaryColor, 0.15f),
        rippleColor = primaryColor.copy(alpha = 0.25f),
        loadingColor = primaryColor,
    )
}

/**
 * Represents the colors used by an AdvancedButton in different states.
 */
@Immutable
data class ButtonColors(
    val backgroundColor: Color,
    val backgroundColorPressed: Color,
    val backgroundColorDisabled: Color,
    val textColor: Color,
    val textColorPressed: Color,
    val textColorDisabled: Color,
    val borderColor: Color,
    val borderColorPressed: Color,
    val borderColorDisabled: Color,
    val iconColor: Color,
    val iconColorPressed: Color,
    val iconColorDisabled: Color,
    val rippleColor: Color,
    val loadingColor: Color,
    val successColor: Color,
    val errorColor: Color,
) {
    /**
     * Get background color based on current state.
     */
    fun backgroundColor(state: ButtonState): Color = when (state) {
        ButtonState.NORMAL -> backgroundColor
        ButtonState.PRESSED -> backgroundColorPressed
        ButtonState.DISABLED -> backgroundColorDisabled
        ButtonState.LOADING -> backgroundColor
        ButtonState.SUCCESS -> successColor
        ButtonState.ERROR -> errorColor
    }
    
    /**
     * Get text color based on current state.
     */
    fun textColor(state: ButtonState): Color = when (state) {
        ButtonState.NORMAL -> textColor
        ButtonState.PRESSED -> textColorPressed
        ButtonState.DISABLED -> textColorDisabled
        ButtonState.LOADING -> textColor
        ButtonState.SUCCESS -> Color.White
        ButtonState.ERROR -> Color.White
    }
    
    /**
     * Get border color based on current state.
     */
    fun borderColor(state: ButtonState): Color = when (state) {
        ButtonState.NORMAL -> borderColor
        ButtonState.PRESSED -> borderColorPressed
        ButtonState.DISABLED -> borderColorDisabled
        ButtonState.LOADING -> borderColor
        ButtonState.SUCCESS -> successColor
        ButtonState.ERROR -> errorColor
    }
    
    /**
     * Get icon color based on current state.
     */
    fun iconColor(state: ButtonState): Color = when (state) {
        ButtonState.NORMAL -> iconColor
        ButtonState.PRESSED -> iconColorPressed
        ButtonState.DISABLED -> iconColorDisabled
        ButtonState.LOADING -> iconColor
        ButtonState.SUCCESS -> Color.White
        ButtonState.ERROR -> Color.White
    }
}

// ===== HELPER FUNCTIONS =====

/**
 * Darkens a color by a given factor.
 */
fun darkenColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * (1 - factor)).coerceIn(0f, 1f),
        green = (color.green * (1 - factor)).coerceIn(0f, 1f),
        blue = (color.blue * (1 - factor)).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

/**
 * Lightens a color by a given factor.
 */
fun lightenColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red + (1f - color.red) * factor).coerceIn(0f, 1f),
        green = (color.green + (1f - color.green) * factor).coerceIn(0f, 1f),
        blue = (color.blue + (1f - color.blue) * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}
