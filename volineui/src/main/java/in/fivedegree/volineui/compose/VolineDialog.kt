@file:Suppress("unused")

package `in`.fivedegree.volineui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.fivedegree.volineui.R
import `in`.fivedegree.volineui.button.ButtonStyle
import `in`.fivedegree.volineui.button.CornerType
import `in`.fivedegree.volineui.dialog.DialogColors
import `in`.fivedegree.volineui.dialog.DialogDefaults
import `in`.fivedegree.volineui.dialog.DialogType

/**
 * Global dialog controller for simple dialog display in Compose.
 *
 * ## Simple Usage
 *
 * 1. Add the container once at your app/screen level:
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     MaterialTheme {
 *         VolineDialog.Container() // Add this once
 *         // Your app content
 *     }
 * }
 * ```
 *
 * 2. Show dialogs from anywhere:
 * ```kotlin
 * VolineDialog.show(
 *     title = "Hello",
 *     message = "This is a dialog"
 * )
 *
 * VolineDialog.success("Success!", "Operation completed")
 * VolineDialog.success("Success!", "Operation completed", isCancelable = false) // back/outside disabled
 * VolineDialog.error("Error", "Something went wrong")
 *
 * VolineDialog.confirm(
 *     title = "Delete?",
 *     message = "Are you sure?",
 *     onConfirm = { /* handle */ }
 * )
 * ```
 */
object VolineDialog {

    // Internal state
    private var _isVisible = mutableStateOf(false)
    private var _title = mutableStateOf("")
    private var _message = mutableStateOf("")
    private var _type = mutableStateOf(DialogType.DEFAULT)
    private var _icon = mutableStateOf<Any?>(null)
    private var _showDefaultIcon = mutableStateOf(true)
    private var _primaryButtonText = mutableStateOf("OK")
    private var _secondaryButtonText = mutableStateOf<String?>(null)
    private var _onPrimaryClick = mutableStateOf<(() -> Unit)?>(null)
    private var _onSecondaryClick = mutableStateOf<(() -> Unit)?>(null)
    private var _isCancelable = mutableStateOf(true)

    /**
     * Show a dialog.
     */
    @JvmStatic
    fun show(
        title: String,
        message: String,
        type: DialogType = DialogType.DEFAULT,
        icon: Any? = null,
        showDefaultIcon: Boolean = true,
        primaryButtonText: String = "OK",
        onPrimaryClick: (() -> Unit)? = null,
        secondaryButtonText: String? = null,
        onSecondaryClick: (() -> Unit)? = null,
        isCancelable: Boolean = true,
    ) {
        _title.value = title
        _message.value = message
        _type.value = type
        _icon.value = icon
        _showDefaultIcon.value = showDefaultIcon
        _primaryButtonText.value = primaryButtonText
        _secondaryButtonText.value = secondaryButtonText
        _onPrimaryClick.value = onPrimaryClick
        _onSecondaryClick.value = onSecondaryClick
        _isCancelable.value = isCancelable
        _isVisible.value = true
    }

    /**
     * Show a success dialog.
     */
    @JvmStatic
    @JvmOverloads
    fun success(
        title: String,
        message: String,
        buttonText: String = "OK",
        onButtonClick: (() -> Unit)? = null,
        isCancelable: Boolean = true,
    ) {
        show(
            title = title,
            message = message,
            type = DialogType.SUCCESS,
            primaryButtonText = buttonText,
            onPrimaryClick = onButtonClick,
            isCancelable = isCancelable,
        )
    }

    /**
     * Show an error dialog.
     */
    @JvmStatic
    @JvmOverloads
    fun error(
        title: String,
        message: String,
        buttonText: String = "OK",
        onButtonClick: (() -> Unit)? = null,
        isCancelable: Boolean = true,
    ) {
        show(
            title = title,
            message = message,
            type = DialogType.ERROR,
            primaryButtonText = buttonText,
            onPrimaryClick = onButtonClick,
            isCancelable = isCancelable,
        )
    }

    /**
     * Show a warning dialog.
     */
    @JvmStatic
    @JvmOverloads
    fun warning(
        title: String,
        message: String,
        buttonText: String = "OK",
        onButtonClick: (() -> Unit)? = null,
        isCancelable: Boolean = true,
    ) {
        show(
            title = title,
            message = message,
            type = DialogType.WARNING,
            primaryButtonText = buttonText,
            onPrimaryClick = onButtonClick,
            isCancelable = isCancelable,
        )
    }

    /**
     * Show an info dialog.
     */
    @JvmStatic
    @JvmOverloads
    fun info(
        title: String,
        message: String,
        buttonText: String = "OK",
        onButtonClick: (() -> Unit)? = null,
        isCancelable: Boolean = true,
    ) {
        show(
            title = title,
            message = message,
            type = DialogType.INFO,
            primaryButtonText = buttonText,
            onPrimaryClick = onButtonClick,
            isCancelable = isCancelable,
        )
    }

    /**
     * Show a confirmation dialog.
     */
    @JvmStatic
    @JvmOverloads
    fun confirm(
        title: String,
        message: String,
        confirmText: String = "Confirm",
        cancelText: String = "Cancel",
        onConfirm: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        isCancelable: Boolean = true,
    ) {
        show(
            title = title,
            message = message,
            type = DialogType.CONFIRMATION,
            primaryButtonText = confirmText,
            secondaryButtonText = cancelText,
            onPrimaryClick = onConfirm,
            onSecondaryClick = onCancel,
            isCancelable = isCancelable,
        )
    }

    /**
     * Show a destructive action dialog.
     */
    @JvmStatic
    @JvmOverloads
    fun destructive(
        title: String,
        message: String,
        destructiveText: String = "Delete",
        cancelText: String = "Cancel",
        onDestructive: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        isCancelable: Boolean = true,
    ) {
        show(
            title = title,
            message = message,
            type = DialogType.DESTRUCTIVE,
            primaryButtonText = destructiveText,
            secondaryButtonText = cancelText,
            onPrimaryClick = onDestructive,
            onSecondaryClick = onCancel,
            isCancelable = isCancelable,
        )
    }

    /**
     * Dismiss the current dialog.
     */
    @JvmStatic
    fun dismiss() {
        _isVisible.value = false
    }

    /**
     * Dialog container composable. Add this once at your app/screen level.
     *
     * Example:
     * ```kotlin
     * @Composable
     * fun MyScreen() {
     *     VolineDialog.Container()
     *     // Your screen content...
     * }
     * ```
     */
    @Composable
    fun Container(
        colors: DialogColors = DialogDefaults.colors(),
    ) {
        val isVisible by _isVisible
        val title by _title
        val message by _message
        val type by _type
        val icon by _icon
        val showDefaultIcon by _showDefaultIcon
        val primaryButtonText by _primaryButtonText
        val secondaryButtonText by _secondaryButtonText
        val onPrimaryClick by _onPrimaryClick
        val onSecondaryClick by _onSecondaryClick
        val isCancelable by _isCancelable

        if (isVisible) {
            Dialog(
                onDismissRequest = { if (isCancelable) dismiss() },
                properties = DialogProperties(
                    dismissOnBackPress = isCancelable,
                    dismissOnClickOutside = isCancelable,
                ),
            ) {
                DialogContent(
                    title = title,
                    message = message,
                    type = type,
                    icon = icon,
                    showDefaultIcon = showDefaultIcon,
                    primaryButtonText = primaryButtonText,
                    // Dismiss before callback so a chained VolineDialog.show/confirm in the listener is not cleared by this dismiss().
                    onPrimaryClick = {
                        dismiss()
                        onPrimaryClick?.invoke()
                    },
                    secondaryButtonText = secondaryButtonText,
                    onSecondaryClick = {
                        dismiss()
                        onSecondaryClick?.invoke()
                    },
                    colors = colors,
                )
            }
        }
    }
}

/**
 * Dialog content composable.
 */
@Composable
private fun DialogContent(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    type: DialogType = DialogType.DEFAULT,
    icon: Any? = null,
    showDefaultIcon: Boolean = true,
    primaryButtonText: String = "OK",
    onPrimaryClick: () -> Unit = {},
    secondaryButtonText: String? = null,
    onSecondaryClick: () -> Unit = {},
    colors: DialogColors = DialogDefaults.colors(),
) {
    val shape = RoundedCornerShape(DialogDefaults.CornerRadius)
    colors.accentColor(type)
    val iconTint = colors.iconTint(type)

    // Determine icon to display
    val displayIcon: Any? = icon ?: if (showDefaultIcon) {
        when (type) {
            DialogType.SUCCESS -> painterResource(R.drawable.ic_success_filled)
            DialogType.ERROR -> painterResource(R.drawable.ic_error_filled)
            DialogType.WARNING, DialogType.DESTRUCTIVE -> painterResource(R.drawable.ic_warning_filled)
            DialogType.INFO -> painterResource(R.drawable.ic_info_filled)
            DialogType.DEFAULT, DialogType.CONFIRMATION -> null
        }
    } else null

    Surface(
        modifier = modifier
            .widthIn(min = DialogDefaults.MinWidth, max = DialogDefaults.MaxWidth)
            .clip(shape),
        shape = shape,
        color = colors.backgroundColor,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(DialogDefaults.HorizontalPadding, DialogDefaults.VerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon
            displayIcon?.let { iconContent ->
                when (iconContent) {
                    is ImageVector -> {
                        Icon(
                            imageVector = iconContent,
                            contentDescription = null,
                            modifier = Modifier.size(DialogDefaults.IconSize),
                            tint = iconTint,
                        )
                    }
                    is Painter -> {
                        Icon(
                            painter = iconContent,
                            contentDescription = null,
                            modifier = Modifier.size(DialogDefaults.IconSize),
                            tint = iconTint,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DialogDefaults.IconTextSpacing))
            }

            // Title
            Text(
                text = title,
                fontSize = DialogDefaults.TitleTextSize,
                fontWeight = FontWeight.SemiBold,
                color = colors.titleTextColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(DialogDefaults.TitleMessageSpacing))

            // Message
            Text(
                text = message,
                fontSize = DialogDefaults.MessageTextSize,
                fontWeight = FontWeight.Normal,
                color = colors.messageTextColor,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(DialogDefaults.ContentButtonSpacing))

            // Buttons using AdvancedButton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialogDefaults.ButtonSpacing),
            ) {
                // Secondary button
                secondaryButtonText?.let { text ->
                    AdvancedButton(
                        text = text,
                        onClick = onSecondaryClick,
                        style = ButtonStyle.OUTLINED,
                        cornerType = CornerType.PILL,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Primary button
                AdvancedButton(
                    text = primaryButtonText,
                    onClick = onPrimaryClick,
                    cornerType = CornerType.PILL,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
