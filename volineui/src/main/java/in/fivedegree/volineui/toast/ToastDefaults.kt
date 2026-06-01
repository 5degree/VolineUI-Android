@file:Suppress("unused")

package `in`.fivedegree.volineui.toast

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default values and colors for VolineToast composable.
 */
object ToastDefaults {

    // ===== DEFAULT DIMENSIONS =====
    val CornerRadius: Dp = 12.dp
    val BorderWidth: Dp = 2.dp
    val HorizontalPadding: Dp = 36.dp
    val VerticalPadding: Dp = 28.dp
    val IconSize: Dp = 32.dp
    val IconTextSpacing: Dp = 12.dp
    val MaxWidth: Dp = 320.dp
    val MinWidth: Dp = 200.dp
    val Elevation: Dp = 8.dp

    // ===== DEFAULT TEXT SIZES =====
    val TitleTextSize: TextUnit = 16.sp
    val MessageTextSize: TextUnit = 14.sp

    // ===== ANIMATION =====
    const val AnimationDuration: Int = 300
    const val FadeInDuration: Int = 200
    const val FadeOutDuration: Int = 150

    // ===== DEFAULT COLORS =====
    private val BackgroundColor = Color.White
    private val TitleTextColor = Color(0xFF252525)
    private val MessageTextColor = Color(0xFF424242)
    private val DefaultBorderColor = Color(0xFF2196F3) // Will be overridden by app's primary
    private val SuccessColor = Color(0xFF43A047)
    private val ErrorColor = Color(0xFFE53935)
    private val WarningColor = Color(0xFFFF9800)
    private val InfoColor = Color(0xFF2196F3)

    /**
     * Creates a [ToastColors] instance with the default colors.
     * Uses the primary color from MaterialTheme for the default border.
     */
    @Composable
    fun colors(
        backgroundColor: Color = BackgroundColor,
        titleTextColor: Color = TitleTextColor,
        messageTextColor: Color = MessageTextColor,
        borderColor: Color = MaterialTheme.colorScheme.primary,
        successColor: Color = SuccessColor,
        errorColor: Color = ErrorColor,
        warningColor: Color = WarningColor,
        infoColor: Color = InfoColor,
        iconTint: Color = Color.Unspecified, // Will use type-specific color if unspecified
    ): ToastColors = ToastColors(
        backgroundColor = backgroundColor,
        titleTextColor = titleTextColor,
        messageTextColor = messageTextColor,
        borderColor = borderColor,
        successColor = successColor,
        errorColor = errorColor,
        warningColor = warningColor,
        infoColor = infoColor,
        iconTint = iconTint,
    )

    /**
     * Creates colors for success toast style.
     */
    @Composable
    fun successColors(
        backgroundColor: Color = BackgroundColor,
    ): ToastColors = colors(
        backgroundColor = backgroundColor,
        borderColor = SuccessColor,
    )

    /**
     * Creates colors for error toast style.
     */
    @Composable
    fun errorColors(
        backgroundColor: Color = BackgroundColor,
    ): ToastColors = colors(
        backgroundColor = backgroundColor,
        borderColor = ErrorColor,
    )

    /**
     * Creates colors for warning toast style.
     */
    @Composable
    fun warningColors(
        backgroundColor: Color = BackgroundColor,
    ): ToastColors = colors(
        backgroundColor = backgroundColor,
        borderColor = WarningColor,
    )

    /**
     * Creates colors for info toast style.
     */
    @Composable
    fun infoColors(
        backgroundColor: Color = BackgroundColor,
    ): ToastColors = colors(
        backgroundColor = backgroundColor,
        borderColor = InfoColor,
    )
}

/**
 * Represents the colors used by a VolineToast in different states.
 */
@Immutable
data class ToastColors(
    val backgroundColor: Color,
    val titleTextColor: Color,
    val messageTextColor: Color,
    val borderColor: Color,
    val successColor: Color,
    val errorColor: Color,
    val warningColor: Color,
    val infoColor: Color,
    val iconTint: Color,
) {
    /**
     * Get border color based on toast type.
     */
    fun borderColor(type: ToastType): Color = when (type) {
        ToastType.DEFAULT -> borderColor
        ToastType.SUCCESS -> successColor
        ToastType.ERROR -> errorColor
        ToastType.WARNING -> warningColor
        ToastType.INFO -> infoColor
    }

    /**
     * Get icon tint color based on toast type.
     * Uses type-specific color if iconTint is unspecified.
     */
    fun iconTint(type: ToastType): Color {
        if (iconTint != Color.Unspecified) return iconTint
        return borderColor(type)
    }
}
