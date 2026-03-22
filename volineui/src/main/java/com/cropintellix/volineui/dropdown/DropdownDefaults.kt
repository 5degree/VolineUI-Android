@file:Suppress("unused")

package com.cropintellix.volineui.dropdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default values and colors for Dropdown composable.
 * These defaults are shared between View and Compose components where applicable.
 */
object DropdownDefaults {
    
    // ==================== DIMENSIONS ====================
    
    // Container
    val CornerRadius: Dp = 8.dp
    val BorderWidth: Dp = 1.dp
    val FocusedBorderWidth: Dp = 2.dp
    val HorizontalPadding: Dp = 16.dp
    val VerticalPadding: Dp = 12.dp
    val MinHeight: Dp = 48.dp
    val LabelGap: Dp = 5.dp
    
    // Icons
    val IconSize: Dp = 24.dp
    val IconPadding: Dp = 12.dp
    
    // Dropdown menu
    val DropdownMaxHeight: Dp = 300.dp
    val DropdownElevation: Dp = 8.dp
    
    // Options
    val OptionHeight: Dp = 48.dp
    val OptionPadding: Dp = 16.dp
    
    // Chips (for multi-select)
    val ChipHeight: Dp = 32.dp
    val ChipCornerRadius: Dp = 16.dp
    val ChipSpacing: Dp = 8.dp
    val ChipIconSize: Dp = 16.dp
    
    // Search
    val SearchDebounceMs: Int = 300
    
    // ==================== TEXT SIZES ====================
    
    val TextSize: TextUnit = 16.sp
    val LabelTextSize: TextUnit = 14.sp
    val HintTextSize: TextUnit = 16.sp
    val ErrorTextSize: TextUnit = 12.sp
    val OptionTextSize: TextUnit = 16.sp
    val OptionDescriptionTextSize: TextUnit = 14.sp
    val ChipTextSize: TextUnit = 14.sp
    val BadgeTextSize: TextUnit = 12.sp
    val HeaderTextSize: TextUnit = 12.sp
    
    // ==================== ANIMATION ====================
    
    const val AnimationDuration: Int = 250
    const val IconRotationDuration: Int = 200
    
    // ==================== COLORS ====================
    
    // Default color values
    private val BorderColor = Color(0xFFCCCCCC)
    private val FocusedBorderColor = Color(0xFF2196F3)
    private val ErrorColor = Color(0xFFE53935)
    private val SuccessColor = Color(0xFF43A047)
    private val DisabledColor = Color(0xFF9E9E9E)
    private val LoadingColor = Color(0xFF2196F3)
    private val BackgroundColor = Color.White
    private val TextColor = Color(0xFF252525)
    private val LabelTextColor = Color(0xFF252525)
    private val HintTextColor = Color(0xFF757575)
    private val OptionTextColor = Color(0xFF212121)
    private val OptionSelectedTextColor = Color(0xFF2196F3)
    private val OptionSelectedBackgroundColor = Color(0x202196F3)
    private val OptionHoverColor = Color(0x10000000)
    private val ChipBackgroundColor = Color(0xFFE0E0E0)
    private val ChipTextColor = Color(0xFF212121)
    private val HeaderTextColor = Color(0xFF757575)
    private val DividerColor = Color(0xFFE0E0E0)
    private val DropdownBackgroundColor = Color.White
    
    /**
     * Creates a [DropdownColors] instance with the default or custom colors.
     * Uses MaterialTheme primary color if available for focused/selected states.
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
        loadingColor: Color = MaterialTheme.colorScheme.primary,
        disabledTextColor: Color = DisabledColor,
        disabledBackgroundColor: Color = Color(0xFFF5F5F5),
        iconColor: Color = Color(0xFF757575),
        optionTextColor: Color = OptionTextColor,
        optionSelectedTextColor: Color = MaterialTheme.colorScheme.primary,
        optionSelectedBackgroundColor: Color = OptionSelectedBackgroundColor,
        optionHoverColor: Color = OptionHoverColor,
        chipBackgroundColor: Color = ChipBackgroundColor,
        chipTextColor: Color = ChipTextColor,
        headerTextColor: Color = HeaderTextColor,
        dividerColor: Color = DividerColor,
        dropdownBackgroundColor: Color = DropdownBackgroundColor,
    ): DropdownColors = DropdownColors(
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
        iconColor = iconColor,
        optionTextColor = optionTextColor,
        optionSelectedTextColor = optionSelectedTextColor,
        optionSelectedBackgroundColor = optionSelectedBackgroundColor,
        optionHoverColor = optionHoverColor,
        chipBackgroundColor = chipBackgroundColor,
        chipTextColor = chipTextColor,
        headerTextColor = headerTextColor,
        dividerColor = dividerColor,
        dropdownBackgroundColor = dropdownBackgroundColor,
    )
}

/**
 * Represents the colors used by a Dropdown component in different states.
 */
@Immutable
data class DropdownColors(
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
    val iconColor: Color,
    val optionTextColor: Color,
    val optionSelectedTextColor: Color,
    val optionSelectedBackgroundColor: Color,
    val optionHoverColor: Color,
    val chipBackgroundColor: Color,
    val chipTextColor: Color,
    val headerTextColor: Color,
    val dividerColor: Color,
    val dropdownBackgroundColor: Color,
) {
    /**
     * Get border color based on current state.
     */
    fun borderColor(
        enabled: Boolean,
        isError: Boolean,
        isSuccess: Boolean,
        isExpanded: Boolean,
        isLoading: Boolean,
    ): Color {
        return when {
            !enabled -> disabledBackgroundColor
            isError -> errorColor
            isSuccess -> successColor
            isLoading -> loadingColor
            isExpanded -> focusedBorderColor
            else -> borderColor
        }
    }
    
    /**
     * Get text color based on enabled state and whether there's a selection.
     */
    fun triggerTextColor(enabled: Boolean, hasSelection: Boolean): Color {
        return when {
            !enabled -> disabledTextColor
            hasSelection -> textColor
            else -> hintTextColor
        }
    }
    
    /**
     * Get background color based on enabled state.
     */
    fun backgroundColor(enabled: Boolean): Color {
        return if (enabled) backgroundColor else disabledBackgroundColor
    }
    
    /**
     * Get option text color based on selection state.
     */
    fun optionTextColor(isSelected: Boolean, isEnabled: Boolean): Color {
        return when {
            !isEnabled -> disabledColor
            isSelected -> optionSelectedTextColor
            else -> optionTextColor
        }
    }
}
