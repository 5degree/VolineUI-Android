@file:Suppress("unused")

package `in`.fivedegree.volineui.filepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Defaults for [in.fivedegree.volineui.compose.AdvancedFilePicker].
 */
object FilePickerDefaults {

    private val DefaultLabelColor = Color(0xFF252525)
    private val DefaultBorderColor = Color(0xFFCCCCCC)
    private val DefaultBackgroundColor = Color(0xFFFAFAFA)
    private val DefaultPlaceholderIconColor = Color(0xFF666666)
    private val DefaultPlaceholderTextColor = Color(0xFF666666)
    private val DefaultFileNameColor = Color(0xFF252525)
    private val DefaultFileMetaColor = Color(0xFF757575)
    private val DefaultIconTint = Color(0xFF546E7A)
    private val DefaultOpenIconTint = Color(0xFF1976D2)
    private val DefaultDeleteIconTint = Color(0xFFE53935)
    private val DefaultDeleteButtonBackground = Color(0x33E53935)
    private val DefaultLoadingColor = Color(0xFF666666)
    private val DefaultErrorColor = Color(0xFFF44336)
    private val DefaultAddButtonIconColor = Color(0xFF666666)
    private val DefaultCardBackground = Color.White

    // Dimensions
    val CornerRadius: Dp = 8.dp
    val BorderWidth: Dp = 1.dp
    val DefaultHeight: Dp = 120.dp
    val CarouselHeight: Dp = 128.dp
    val ItemWidth: Dp = 148.dp
    val ItemSpacing: Dp = 8.dp

    // Label
    val LabelGap: Dp = 5.dp
    val LabelTextSize: TextUnit = 14.sp
    val LabelFontWeight: FontWeight = FontWeight.Normal
    val LabelTextStyle: TextStyle = TextStyle(
        color = DefaultLabelColor,
        fontSize = LabelTextSize,
        fontWeight = LabelFontWeight,
    )

    // Placeholder
    val PlaceholderIconSize: Dp = 32.dp
    val PlaceholderTextSize: TextUnit = 12.sp
    val PlaceholderGap: Dp = 8.dp
    const val PlaceholderText: String = "Tap to select file"

    // File card
    val FileIconSize: Dp = 28.dp
    val FileNameTextSize: TextUnit = 13.sp
    val FileMetaTextSize: TextUnit = 11.sp
    val CardPadding: Dp = 10.dp
    val CardContentGap: Dp = 6.dp
    val ActionIconSize: Dp = 20.dp
    val ActionIconPadding: Dp = 4.dp

    // Delete
    val DeleteButtonSize: Dp = 24.dp
    val DeleteButtonPadding: Dp = 4.dp
    val DeleteButtonMargin: Dp = 4.dp

    // Loading
    val LoadingIndicatorSize: Dp = 28.dp
    val LoadingIndicatorStrokeWidth: Dp = 2.5.dp

    // Add
    val AddButtonIconSize: Dp = 28.dp

    // Animation
    const val FadeAnimationDuration: Int = 250

    @Composable
    fun colors(
        borderColor: Color = DefaultBorderColor,
        backgroundColor: Color = DefaultBackgroundColor,
        cardBackgroundColor: Color = DefaultCardBackground,
        placeholderIconColor: Color = DefaultPlaceholderIconColor,
        placeholderTextColor: Color = DefaultPlaceholderTextColor,
        fileNameColor: Color = DefaultFileNameColor,
        fileMetaColor: Color = DefaultFileMetaColor,
        fileIconTint: Color = DefaultIconTint,
        openIconTint: Color = DefaultOpenIconTint,
        deleteIconTint: Color = DefaultDeleteIconTint,
        deleteButtonBackground: Color = DefaultDeleteButtonBackground,
        loadingColor: Color = DefaultLoadingColor,
        errorColor: Color = DefaultErrorColor,
        addButtonIconColor: Color = DefaultAddButtonIconColor,
    ): FilePickerColors = FilePickerColors(
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        cardBackgroundColor = cardBackgroundColor,
        placeholderIconColor = placeholderIconColor,
        placeholderTextColor = placeholderTextColor,
        fileNameColor = fileNameColor,
        fileMetaColor = fileMetaColor,
        fileIconTint = fileIconTint,
        openIconTint = openIconTint,
        deleteIconTint = deleteIconTint,
        deleteButtonBackground = deleteButtonBackground,
        loadingColor = loadingColor,
        errorColor = errorColor,
        addButtonIconColor = addButtonIconColor,
    )
}

/**
 * Color tokens for AdvancedFilePicker chrome.
 */
@Immutable
data class FilePickerColors(
    val borderColor: Color,
    val backgroundColor: Color,
    val cardBackgroundColor: Color,
    val placeholderIconColor: Color,
    val placeholderTextColor: Color,
    val fileNameColor: Color,
    val fileMetaColor: Color,
    val fileIconTint: Color,
    val openIconTint: Color,
    val deleteIconTint: Color,
    val deleteButtonBackground: Color,
    val loadingColor: Color,
    val errorColor: Color,
    val addButtonIconColor: Color,
) {
    fun placeholderIconColor(state: FilePickerUiState): Color =
        if (state == FilePickerUiState.ERROR) errorColor else placeholderIconColor

    fun placeholderTextColor(state: FilePickerUiState): Color =
        if (state == FilePickerUiState.ERROR) errorColor else placeholderTextColor

    fun borderColor(state: FilePickerUiState): Color =
        if (state == FilePickerUiState.ERROR) errorColor else borderColor
}
