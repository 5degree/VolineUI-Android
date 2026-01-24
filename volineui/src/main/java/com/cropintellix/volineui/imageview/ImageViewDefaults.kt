@file:Suppress("unused")

package com.cropintellix.volineui.imageview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default values and configurations for AdvancedImageView composable.
 */
object ImageViewDefaults {

    // ===== DIMENSIONS =====
    val CornerRadius: Dp = 8.dp
    val BorderWidth: Dp = 1.dp
    val DefaultHeight: Dp = 200.dp
    val MinHeight: Dp = 120.dp
    val MinWidth: Dp = 120.dp
    
    // ===== LABEL DEFAULTS =====
    val LabelGap: Dp = 5.dp
    val LabelTextSize: TextUnit = 14.sp
    val LabelFontWeight: FontWeight = FontWeight.Normal
    
    // ===== PLACEHOLDER DEFAULTS =====
    val PlaceholderIconSize: Dp = 36.dp
    val PlaceholderTextSize: TextUnit = 12.sp
    val PlaceholderGap: Dp = 8.dp
    val PlaceholderText: String = "Tap to capture"
    
    // ===== DELETE BUTTON DEFAULTS =====
    val DeleteButtonSize: Dp = 26.dp
    val DeleteButtonPadding: Dp = 5.dp
    val DeleteButtonMargin: Dp = 6.dp
    val DeleteButtonCornerRadius: Dp = 4.dp
    
    // ===== LOADING INDICATOR =====
    val LoadingIndicatorSize: Dp = 36.dp
    val LoadingIndicatorStrokeWidth: Dp = 3.dp
    
    // ===== ANIMATION =====
    const val AnimationDuration: Int = 200
    const val FadeAnimationDuration: Int = 300
    
    // ===== DEFAULT COLORS =====
    private val DefaultLabelColor = Color(0xFF252525)
    private val DefaultBorderColor = Color(0xFFCCCCCC)
    private val DefaultBackgroundColor = Color.Transparent
    private val DefaultPlaceholderIconColor = Color(0xFF666666)
    private val DefaultPlaceholderTextColor = Color(0xFF666666)
    private val DefaultDeleteIconTint = Color(0xFFE53935)
    private val DefaultDeleteButtonBackground = Color(0x33E53935)
    private val DefaultLoadingColor = Color(0xFF666666)
    private val DefaultErrorColor = Color(0xFFF44336)
    
    /**
     * Creates a [ImageViewColors] instance with the default colors.
     */
    @Composable
    fun colors(
        labelColor: Color = DefaultLabelColor,
        borderColor: Color = DefaultBorderColor,
        backgroundColor: Color = DefaultBackgroundColor,
        placeholderIconColor: Color = DefaultPlaceholderIconColor,
        placeholderTextColor: Color = DefaultPlaceholderTextColor,
        deleteIconTint: Color = DefaultDeleteIconTint,
        deleteButtonBackground: Color = DefaultDeleteButtonBackground,
        loadingColor: Color = DefaultLoadingColor,
        errorColor: Color = DefaultErrorColor,
    ): ImageViewColors = ImageViewColors(
        labelColor = labelColor,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        placeholderIconColor = placeholderIconColor,
        placeholderTextColor = placeholderTextColor,
        deleteIconTint = deleteIconTint,
        deleteButtonBackground = deleteButtonBackground,
        loadingColor = loadingColor,
        errorColor = errorColor,
    )
    
    /**
     * Creates colors optimized for a bordered placeholder style.
     */
    @Composable
    fun borderedPlaceholderColors(
        borderColor: Color = DefaultBorderColor,
        backgroundColor: Color = Color(0xFFF5F5F5),
    ): ImageViewColors = colors(
        borderColor = borderColor,
        backgroundColor = backgroundColor,
    )
    
    /**
     * Creates colors optimized for display-only mode (no placeholder).
     */
    @Composable
    fun displayOnlyColors(
        backgroundColor: Color = Color.Transparent,
    ): ImageViewColors = colors(
        borderColor = Color.Transparent,
        backgroundColor = backgroundColor,
    )
}

/**
 * Represents the colors used by an AdvancedImageView in different states.
 */
@Immutable
data class ImageViewColors(
    val labelColor: Color,
    val borderColor: Color,
    val backgroundColor: Color,
    val placeholderIconColor: Color,
    val placeholderTextColor: Color,
    val deleteIconTint: Color,
    val deleteButtonBackground: Color,
    val loadingColor: Color,
    val errorColor: Color,
) {
    /**
     * Get placeholder icon color based on state.
     */
    fun placeholderIconColor(state: ImageState): Color = when (state) {
        ImageState.ERROR -> errorColor
        else -> placeholderIconColor
    }
    
    /**
     * Get placeholder text color based on state.
     */
    fun placeholderTextColor(state: ImageState): Color = when (state) {
        ImageState.ERROR -> errorColor
        else -> placeholderTextColor
    }
    
    /**
     * Get border color based on state.
     */
    fun borderColor(state: ImageState): Color = when (state) {
        ImageState.ERROR -> errorColor
        else -> borderColor
    }
}

/**
 * Configuration for the label in AdvancedImageView.
 */
@Immutable
data class ImageViewLabelConfig(
    val text: String = "",
    val gap: Dp = ImageViewDefaults.LabelGap,
    val textSize: TextUnit = ImageViewDefaults.LabelTextSize,
    val fontWeight: FontWeight = ImageViewDefaults.LabelFontWeight,
)

/**
 * Configuration for the placeholder in AdvancedImageView.
 */
@Immutable
data class ImageViewPlaceholderConfig(
    val text: String = ImageViewDefaults.PlaceholderText,
    val iconResId: Int = 0,
    val iconSize: Dp = ImageViewDefaults.PlaceholderIconSize,
    val textSize: TextUnit = ImageViewDefaults.PlaceholderTextSize,
    val gap: Dp = ImageViewDefaults.PlaceholderGap,
)

/**
 * Configuration for features in AdvancedImageView.
 */
@Immutable
data class ImageViewFeatures(
    val showDeleteButton: Boolean = true,
    val showLoadingIndicator: Boolean = true,
    val enableFullScreenPreview: Boolean = true,
    val enableCameraCapture: Boolean = true,
)
