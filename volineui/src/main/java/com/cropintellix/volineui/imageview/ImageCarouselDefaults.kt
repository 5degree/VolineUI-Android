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
 * Default values and configurations for ImageCarousel composable.
 * 
 * These defaults match the View-based ImageCarousel for consistent look and behavior.
 */
object ImageCarouselDefaults {

    // ===== LABEL DEFAULTS =====
    val LabelGap: Dp = 5.dp
    val LabelTextSize: TextUnit = 14.sp
    val LabelFontWeight: FontWeight = FontWeight.Normal

    // ===== CAROUSEL DIMENSIONS =====
    val CarouselHeight: Dp = 200.dp

    // ===== ITEM DIMENSIONS =====
    val ItemWidth: Dp = 120.dp
    val ItemHeight: Dp = 120.dp
    val ItemSpacing: Dp = 8.dp
    val CornerRadius: Dp = 8.dp
    val BorderWidth: Dp = 1.dp

    // ===== INDICATOR DEFAULTS =====
    val IndicatorSize: Dp = 6.dp
    val IndicatorSpacing: Dp = 4.dp
    val IndicatorBottomMargin: Dp = 8.dp

    // ===== ADD BUTTON DEFAULTS =====
    val AddButtonIconSize: Dp = 32.dp

    // ===== DELETE BUTTON DEFAULTS =====
    val DeleteButtonSize: Dp = 24.dp
    val DeleteButtonPadding: Dp = 4.dp
    val DeleteButtonMargin: Dp = 4.dp

    // ===== LOADING INDICATOR =====
    val LoadingIndicatorSize: Dp = 24.dp
    val LoadingIndicatorStrokeWidth: Dp = 2.dp

    // ===== ANIMATION =====
    const val AnimationDuration: Int = 200
    const val FadeAnimationDuration: Int = 300

    // ===== DEFAULT MAX IMAGE COUNT =====
    const val DefaultMaxImageCount: Int = Int.MAX_VALUE

    // ===== DEFAULT COLORS =====
    private val DefaultLabelColor = Color(0xFF252525)
    private val DefaultBorderColor = Color(0xFFCCCCCC)
    private val DefaultBackgroundColor = Color.White
    private val DefaultAddButtonIconColor = Color(0xFF666666)
    private val DefaultDeleteIconTint = Color(0xFFE53935)
    private val DefaultDeleteButtonBackground = Color(0x33E53935)
    private val DefaultLoadingColor = Color(0xFF666666)
    private val DefaultIndicatorActiveColor = Color.White
    private val DefaultIndicatorInactiveColor = Color(0x80FFFFFF)

    /**
     * Creates a [ImageCarouselColors] instance with the default colors.
     */
    @Composable
    fun colors(
        labelColor: Color = DefaultLabelColor,
        borderColor: Color = DefaultBorderColor,
        backgroundColor: Color = DefaultBackgroundColor,
        addButtonIconColor: Color = DefaultAddButtonIconColor,
        deleteIconTint: Color = DefaultDeleteIconTint,
        deleteButtonBackground: Color = DefaultDeleteButtonBackground,
        loadingColor: Color = DefaultLoadingColor,
        indicatorActiveColor: Color = DefaultIndicatorActiveColor,
        indicatorInactiveColor: Color = DefaultIndicatorInactiveColor,
    ): ImageCarouselColors = ImageCarouselColors(
        labelColor = labelColor,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        addButtonIconColor = addButtonIconColor,
        deleteIconTint = deleteIconTint,
        deleteButtonBackground = deleteButtonBackground,
        loadingColor = loadingColor,
        indicatorActiveColor = indicatorActiveColor,
        indicatorInactiveColor = indicatorInactiveColor,
    )

    /**
     * Creates colors optimized for dark indicator on light images.
     */
    @Composable
    fun lightIndicatorColors(
        indicatorActiveColor: Color = Color(0xFF2196F3),
        indicatorInactiveColor: Color = Color(0x40000000),
    ): ImageCarouselColors = colors(
        indicatorActiveColor = indicatorActiveColor,
        indicatorInactiveColor = indicatorInactiveColor,
    )
}

/**
 * Represents the colors used by an ImageCarousel in different states.
 */
@Immutable
data class ImageCarouselColors(
    val labelColor: Color,
    val borderColor: Color,
    val backgroundColor: Color,
    val addButtonIconColor: Color,
    val deleteIconTint: Color,
    val deleteButtonBackground: Color,
    val loadingColor: Color,
    val indicatorActiveColor: Color,
    val indicatorInactiveColor: Color,
)

/**
 * Configuration for the label in ImageCarousel.
 */
@Immutable
data class CarouselLabelConfig(
    val text: String = "",
    val gap: Dp = ImageCarouselDefaults.LabelGap,
    val textSize: TextUnit = ImageCarouselDefaults.LabelTextSize,
    val fontWeight: FontWeight = ImageCarouselDefaults.LabelFontWeight,
)

/**
 * Configuration for carousel items.
 */
@Immutable
data class CarouselItemConfig(
    val width: Dp = ImageCarouselDefaults.ItemWidth,
    val height: Dp = ImageCarouselDefaults.ItemHeight,
    val spacing: Dp = ImageCarouselDefaults.ItemSpacing,
    val cornerRadius: Dp = ImageCarouselDefaults.CornerRadius,
    val borderWidth: Dp = ImageCarouselDefaults.BorderWidth,
)

/**
 * Configuration for carousel indicators.
 */
@Immutable
data class CarouselIndicatorConfig(
    val size: Dp = ImageCarouselDefaults.IndicatorSize,
    val spacing: Dp = ImageCarouselDefaults.IndicatorSpacing,
    val bottomMargin: Dp = ImageCarouselDefaults.IndicatorBottomMargin,
)

/**
 * Configuration for features in ImageCarousel.
 */
@Immutable
data class CarouselFeatures(
    val showIndicators: Boolean = true,
    val showDeleteIcon: Boolean = true,
    val enableFullScreen: Boolean = true,
    val maxImageCount: Int = ImageCarouselDefaults.DefaultMaxImageCount,
)
