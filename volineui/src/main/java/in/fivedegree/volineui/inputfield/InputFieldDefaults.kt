package `in`.fivedegree.volineui.inputfield

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.fivedegree.volineui.inputfield.InputFieldDefaults.MinHeight

/**
 * Default values and colors for InputField composable.
 */
object InputFieldDefaults {

    // Default dimensions
    val CornerRadius: Dp = 8.dp
    val BorderWidth: Dp = 1.dp
    val FocusedBorderWidth: Dp = 2.dp
    val HorizontalPadding: Dp = 16.dp
    val VerticalPadding: Dp = 12.dp
    val IconPadding: Dp = 12.dp
    val IconSize: Dp = 24.dp
    val LabelGap: Dp = 5.dp
    val MinHeight: Dp = 48.dp
    /** Vertical inset so the trailing divider does not span the full field height (Figma-style). */
    val TrailingDividerVerticalInset: Dp = 8.dp
    val TrailingDividerWidth: Dp = 1.dp
    /**
     * Trailing vertical divider height — fits within the inner padded content area so the divider
     * does not inflate the row height beyond [MinHeight].
     */
    val TrailingDividerHeight: Dp = MinHeight - VerticalPadding * 2
    /** Space between the editable text and the trailing divider (View + Compose). */
    val TrailingInputToDividerGap: Dp = 8.dp
    /** Space between divider and trailing text, and before optional trailing icon. */
    val TrailingTextStartPadding: Dp = 12.dp
    val TrailingTextEndPadding: Dp = 8.dp
    /** Max width for trailing unit text; ellipsis applies beyond this (fraction of field width). */
    const val TrailingTextMaxWidthFraction: Float = 0.42f

    // Default text sizes
    val TextSize: TextUnit = 16.sp
    val LabelTextSize: TextUnit = 14.sp
    val ErrorTextSize: TextUnit = 12.sp
    val CounterTextSize: TextUnit = 12.sp

    // Default colors
    private val BorderColor = Color(0xFFCCCCCC)
    private val ErrorColor = Color(0xFFE53935)
    private val SuccessColor = Color(0xFF43A047)
    private val DisabledColor = Color(0xFF9E9E9E)
    private val BackgroundColor = Color.White
    private val TextColor = Color(0xFF212121)
    private val LabelTextColor = Color(0xFF252525)
    private val HintTextColor = Color(0xFF757575)
    private val TrailingTextColorDefault = Color(0xFF9E9E9E)

    /**
     * Creates an [InputFieldColors] instance with the default colors.
     */
    @Composable
    fun colors(
        textColor: Color = TextColor,
        labelTextColor: Color = LabelTextColor,
        hintTextColor: Color = HintTextColor,
        backgroundColor: Color = BackgroundColor,
        borderColor: Color = BorderColor,
        focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
        errorColor: Color = ErrorColor,
        successColor: Color = SuccessColor,
        disabledColor: Color = DisabledColor,
        loadingColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        disabledTextColor: Color = DisabledColor,
        disabledBackgroundColor: Color = Color(0xFFF5F5F5),
        cursorColor: Color = MaterialTheme.colorScheme.primary,
        iconColor: Color = Color(0xFF757575),
        trailingTextColor: Color = TrailingTextColorDefault,
        trailingDividerColor: Color = BorderColor,
    ): InputFieldColors = InputFieldColors(
        textColor = textColor,
        labelTextColor = labelTextColor,
        hintTextColor = hintTextColor,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        focusedBorderColor = focusedBorderColor,
        errorColor = errorColor,
        successColor = successColor,
        disabledColor = disabledColor,
        loadingColor = loadingColor,
        disabledTextColor = disabledTextColor,
        disabledBackgroundColor = disabledBackgroundColor,
        cursorColor = cursorColor,
        iconColor = iconColor,
        trailingTextColor = trailingTextColor,
        trailingDividerColor = trailingDividerColor,
    )
}

/**
 * Represents the colors used by an InputField in different states.
 */
@Immutable
data class InputFieldColors(
    val textColor: Color,
    val labelTextColor: Color,
    val hintTextColor: Color,
    val backgroundColor: Color,
    val borderColor: Color,
    val focusedBorderColor: Color,
    val errorColor: Color,
    val successColor: Color,
    val disabledColor: Color,
    val loadingColor: Color,
    val disabledTextColor: Color,
    val disabledBackgroundColor: Color,
    val cursorColor: Color,
    val iconColor: Color,
    val trailingTextColor: Color,
    val trailingDividerColor: Color,
) {
    /**
     * Get border color based on current state.
     */
    fun borderColor(
        enabled: Boolean,
        isError: Boolean,
        isSuccess: Boolean,
        isFocused: Boolean,
        isLoading: Boolean,
    ): Color {
        return when {
            !enabled -> disabledBackgroundColor
            isError -> errorColor
            isSuccess -> successColor
            isLoading -> loadingColor
            isFocused -> focusedBorderColor
            else -> borderColor
        }
    }

    /**
     * Get text color based on enabled state.
     */
    fun textColor(enabled: Boolean): Color {
        return if (enabled) textColor else disabledTextColor
    }

    /**
     * Get background color based on enabled state.
     */
    fun backgroundColor(enabled: Boolean): Color {
        return if (enabled) backgroundColor else disabledBackgroundColor
    }

    fun trailingTextColor(enabled: Boolean): Color {
        return if (enabled) trailingTextColor else trailingTextColor.copy(alpha = 0.5f)
    }
}
