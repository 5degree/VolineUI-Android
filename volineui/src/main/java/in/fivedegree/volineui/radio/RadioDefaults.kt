@file:Suppress("unused")

package `in`.fivedegree.volineui.radio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default values and colors for Radio composable.
 */
object RadioDefaults {
    
    // Dimensions
    val Height: Dp = 48.dp
    val Gap: Dp = 4.dp
    val OptionSpacing: Dp = 4.dp
    val CornerRadius: Dp = 24.dp
    val PillElevation: Dp = 4.dp
    val LabelGap: Dp = 5.dp
    
    // Text sizes
    val LabelTextSize: TextUnit = 14.sp
    val TextSize: TextUnit = 14.sp
    
    // Animation
    const val AnimationDuration: Int = 300
    
    // Default colors
    private val SelectedBackgroundColor = Color(0xFF2196F3)
    private val UnselectedBackgroundColor = Color(0xFFF5F5F5)
    private val SelectedTextColor = Color.White
    private val UnselectedTextColor = Color(0xFF757575)
    private val ContainerBackgroundColor = Color(0xFFF5F5F5)
    private val LabelTextColor = Color(0xFF252525)
    
    /**
     * Creates a [RadioColors] instance with the default colors.
     * Uses the primary color from MaterialTheme if available, otherwise falls back to blue.
     */
    @Composable
    fun colors(
        selectedBackgroundColor: Color = MaterialTheme.colorScheme.primary,
        unselectedBackgroundColor: Color = UnselectedBackgroundColor,
        selectedTextColor: Color = SelectedTextColor,
        unselectedTextColor: Color = UnselectedTextColor,
        containerBackgroundColor: Color = ContainerBackgroundColor,
        labelTextColor: Color = LabelTextColor,
        disabledAlpha: Float = 0.6f,
    ): RadioColors = RadioColors(
        selectedBackgroundColor = selectedBackgroundColor,
        unselectedBackgroundColor = unselectedBackgroundColor,
        selectedTextColor = selectedTextColor,
        unselectedTextColor = unselectedTextColor,
        containerBackgroundColor = containerBackgroundColor,
        labelTextColor = labelTextColor,
        disabledAlpha = disabledAlpha,
    )
}

/**
 * Represents the colors used by a Radio component in different states.
 */
@Immutable
data class RadioColors(
    val selectedBackgroundColor: Color,
    val unselectedBackgroundColor: Color,
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
    val containerBackgroundColor: Color,
    val labelTextColor: Color,
    val disabledAlpha: Float,
) {
    /**
     * Get text color based on selection state.
     */
    fun textColor(isSelected: Boolean): Color {
        return if (isSelected) selectedTextColor else unselectedTextColor
    }
}
