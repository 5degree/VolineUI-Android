@file:Suppress("unused")

package com.cropintellix.volineui.dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default values and colors for VolineDialog composable.
 */
object DialogDefaults {

    // ===== DEFAULT DIMENSIONS =====
    val CornerRadius: Dp = 16.dp
    val HorizontalPadding: Dp = 24.dp
    val VerticalPadding: Dp = 24.dp
    val IconSize: Dp = 64.dp
    val IconTextSpacing: Dp = 20.dp
    val TitleMessageSpacing: Dp = 10.dp
    val ContentButtonSpacing: Dp = 24.dp
    val ButtonSpacing: Dp = 12.dp
    val ButtonCornerRadius: Dp = 40.dp
    val ButtonHeight: Dp = 44.dp
    val MinWidth: Dp = 280.dp
    val MaxWidth: Dp = 400.dp

    // ===== DEFAULT TEXT SIZES =====
    val TitleTextSize: TextUnit = 18.sp
    val MessageTextSize: TextUnit = 15.sp
    val ButtonTextSize: TextUnit = 15.sp

    // ===== ANIMATION =====
    const val AnimationDuration: Int = 300

    // ===== DEFAULT COLORS =====
    private val BackgroundColor = Color.White
    private val TitleTextColor = Color(0xFF1C1B1F)
    private val MessageTextColor = Color(0xFF424242)
    private val PrimaryColor = Color(0xFF2196F3)
    private val SuccessColor = Color(0xFF43A047)
    private val ErrorColor = Color(0xFFE53935)
    private val WarningColor = Color(0xFFFF9800)
    private val InfoColor = Color(0xFF2196F3)
    private val DestructiveColor = Color(0xFFD32F2F)
    private val SecondaryButtonColor = Color(0xFF757575)
    private val DisabledColor = Color(0xFFBDBDBD)
    private val DimColor = Color(0x80000000)

    /**
     * Creates a [DialogColors] instance with the default colors.
     * Uses the primary color from MaterialTheme for accent colors.
     */
    @Composable
    fun colors(
        backgroundColor: Color = BackgroundColor,
        titleTextColor: Color = TitleTextColor,
        messageTextColor: Color = MessageTextColor,
        primaryColor: Color = MaterialTheme.colorScheme.primary,
        primaryButtonTextColor: Color = Color.White,
        secondaryButtonColor: Color = SecondaryButtonColor,
        secondaryButtonTextColor: Color = SecondaryButtonColor,
        successColor: Color = SuccessColor,
        errorColor: Color = ErrorColor,
        warningColor: Color = WarningColor,
        infoColor: Color = InfoColor,
        destructiveColor: Color = DestructiveColor,
        disabledColor: Color = DisabledColor,
        dimColor: Color = DimColor,
        iconTint: Color = Color.Unspecified, // Will use type-specific color if unspecified
    ): DialogColors = DialogColors(
        backgroundColor = backgroundColor,
        titleTextColor = titleTextColor,
        messageTextColor = messageTextColor,
        primaryColor = primaryColor,
        primaryButtonTextColor = primaryButtonTextColor,
        secondaryButtonColor = secondaryButtonColor,
        secondaryButtonTextColor = secondaryButtonTextColor,
        successColor = successColor,
        errorColor = errorColor,
        warningColor = warningColor,
        infoColor = infoColor,
        destructiveColor = destructiveColor,
        disabledColor = disabledColor,
        dimColor = dimColor,
        iconTint = iconTint,
    )

    /**
     * Creates colors for success dialog.
     */
    @Composable
    fun successColors(): DialogColors = colors(
        primaryColor = SuccessColor,
    )

    /**
     * Creates colors for error dialog.
     */
    @Composable
    fun errorColors(): DialogColors = colors(
        primaryColor = ErrorColor,
    )

    /**
     * Creates colors for warning dialog.
     */
    @Composable
    fun warningColors(): DialogColors = colors(
        primaryColor = WarningColor,
    )

    /**
     * Creates colors for destructive action dialog.
     */
    @Composable
    fun destructiveColors(): DialogColors = colors(
        primaryColor = DestructiveColor,
    )
}

/**
 * Represents the colors used by a VolineDialog.
 */
@Immutable
data class DialogColors(
    val backgroundColor: Color,
    val titleTextColor: Color,
    val messageTextColor: Color,
    val primaryColor: Color,
    val primaryButtonTextColor: Color,
    val secondaryButtonColor: Color,
    val secondaryButtonTextColor: Color,
    val successColor: Color,
    val errorColor: Color,
    val warningColor: Color,
    val infoColor: Color,
    val destructiveColor: Color,
    val disabledColor: Color,
    val dimColor: Color,
    val iconTint: Color,
) {
    /**
     * Get accent color based on dialog type.
     */
    fun accentColor(type: DialogType): Color = when (type) {
        DialogType.DEFAULT, DialogType.CONFIRMATION -> primaryColor
        DialogType.SUCCESS -> successColor
        DialogType.ERROR -> errorColor
        DialogType.WARNING -> warningColor
        DialogType.INFO -> infoColor
        DialogType.DESTRUCTIVE -> destructiveColor
    }

    /**
     * Get primary button color based on dialog type.
     */
    fun primaryButtonColor(type: DialogType): Color = accentColor(type)

    /**
     * Get icon tint color based on dialog type.
     * Uses type-specific color if iconTint is unspecified.
     */
    fun iconTint(type: DialogType): Color {
        if (iconTint != Color.Unspecified) return iconTint
        return accentColor(type)
    }
}
