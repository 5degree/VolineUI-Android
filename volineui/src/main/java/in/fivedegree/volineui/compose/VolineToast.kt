@file:Suppress("unused")

package `in`.fivedegree.volineui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import `in`.fivedegree.volineui.R
import `in`.fivedegree.volineui.toast.ToastColors
import `in`.fivedegree.volineui.toast.ToastDefaults
import `in`.fivedegree.volineui.toast.ToastDuration
import `in`.fivedegree.volineui.toast.ToastPosition
import `in`.fivedegree.volineui.toast.ToastType
import kotlinx.coroutines.*

/**
 * Global toast controller for simple toast display in Compose.
 *
 * ## Simple Usage
 *
 * 1. Add the container once at your app/screen level:
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     MaterialTheme {
 *         VolineToast.Container() // Add this once
 *         // Your app content
 *     }
 * }
 * ```
 *
 * 2. Show toasts from anywhere:
 * ```kotlin
 * VolineToast.show("Hello!")
 * VolineToast.success("Saved successfully!")
 * VolineToast.error("Something went wrong")
 * VolineToast.warning("Low battery")
 * VolineToast.info("New update available")
 * ```
 */
object VolineToast {

    // Internal state
    private var _isVisible = mutableStateOf(false)
    private var _message = mutableStateOf("")
    private var _title = mutableStateOf<String?>(null)
    private var _type = mutableStateOf(ToastType.DEFAULT)
    private var _duration = mutableStateOf(ToastDuration.MEDIUM)
    private var _position = mutableStateOf(ToastPosition.CENTER)
    private var _icon = mutableStateOf<Any?>(null)

    private var dismissJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Show a default toast.
     */
    @JvmStatic
    @JvmOverloads
    fun show(
        message: String,
        title: String? = null,
        duration: ToastDuration = ToastDuration.MEDIUM,
        position: ToastPosition = ToastPosition.CENTER,
        icon: Any? = null,
    ) {
        showInternal(message, title, ToastType.DEFAULT, duration, position, icon)
    }

    /**
     * Show a success toast.
     */
    @JvmStatic
    @JvmOverloads
    fun success(
        message: String,
        title: String? = null,
        duration: ToastDuration = ToastDuration.MEDIUM,
        position: ToastPosition = ToastPosition.CENTER,
    ) {
        showInternal(message, title, ToastType.SUCCESS, duration, position, null)
    }

    /**
     * Show an error toast.
     */
    @JvmStatic
    @JvmOverloads
    fun error(
        message: String,
        title: String? = null,
        duration: ToastDuration = ToastDuration.MEDIUM,
        position: ToastPosition = ToastPosition.CENTER,
    ) {
        showInternal(message, title, ToastType.ERROR, duration, position, null)
    }

    /**
     * Show a warning toast.
     */
    @JvmStatic
    @JvmOverloads
    fun warning(
        message: String,
        title: String? = null,
        duration: ToastDuration = ToastDuration.MEDIUM,
        position: ToastPosition = ToastPosition.CENTER,
    ) {
        showInternal(message, title, ToastType.WARNING, duration, position, null)
    }

    /**
     * Show an info toast.
     */
    @JvmStatic
    @JvmOverloads
    fun info(
        message: String,
        title: String? = null,
        duration: ToastDuration = ToastDuration.MEDIUM,
        position: ToastPosition = ToastPosition.CENTER,
    ) {
        showInternal(message, title, ToastType.INFO, duration, position, null)
    }

    /**
     * Dismiss the current toast.
     */
    @JvmStatic
    fun dismiss() {
        dismissJob?.cancel()
        _isVisible.value = false
    }

    private fun showInternal(
        message: String,
        title: String?,
        type: ToastType,
        duration: ToastDuration,
        position: ToastPosition,
        icon: Any?,
    ) {
        // Cancel any existing dismiss job
        dismissJob?.cancel()

        // Update state
        _message.value = message
        _title.value = title
        _type.value = type
        _duration.value = duration
        _position.value = position
        _icon.value = icon
        _isVisible.value = true

        // Schedule auto-dismiss
        dismissJob = scope.launch {
            delay(duration.millis)
            _isVisible.value = false
        }
    }

    /**
     * Toast container composable. Add this once at your app/screen level.
     *
     * Example:
     * ```kotlin
     * @Composable
     * fun MyScreen() {
     *     VolineToast.Container()
     *     // Your screen content...
     * }
     * ```
     */
    @Composable
    fun Container(
        modifier: Modifier = Modifier,
        colors: ToastColors = ToastDefaults.colors(),
    ) {
        val isVisible by _isVisible
        val message by _message
        val title by _title
        val type by _type
        val position by _position
        val icon by _icon

        if (isVisible) {
            Popup(
                alignment = when (position) {
                    ToastPosition.TOP -> Alignment.TopCenter
                    ToastPosition.CENTER -> Alignment.Center
                    ToastPosition.BOTTOM -> Alignment.BottomCenter
                },
                properties = PopupProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
                onDismissRequest = { dismiss() },
            ) {
                // Use a Box to center content and apply animation
                Box(
                    modifier = modifier.padding(
                        vertical = when (position) {
                            ToastPosition.TOP -> 48.dp
                            ToastPosition.BOTTOM -> 48.dp
                            ToastPosition.CENTER -> 0.dp
                        },
                        horizontal = 24.dp
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Animate the content appearance
                    var appeared by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { appeared = true }

                    val scale by animateFloatAsState(
                        targetValue = if (appeared) 1f else 0.8f,
                        animationSpec = tween(ToastDefaults.FadeInDuration),
                        label = "toast_scale"
                    )
                    val alpha by animateFloatAsState(
                        targetValue = if (appeared) 1f else 0f,
                        animationSpec = tween(ToastDefaults.FadeInDuration),
                        label = "toast_alpha"
                    )

                    ToastContent(
                        message = message,
                        title = title,
                        type = type,
                        icon = icon,
                        colors = colors,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            },
                    )
                }
            }
        }
    }
}

/**
 * Toast content composable.
 */
@Composable
private fun ToastContent(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    type: ToastType = ToastType.DEFAULT,
    icon: Any? = null,
    showDefaultIcon: Boolean = true,
    colors: ToastColors = ToastDefaults.colors(),
    cornerRadius: Dp = ToastDefaults.CornerRadius,
    elevation: Dp = ToastDefaults.Elevation,
    fontFamily: FontFamily? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = colors.borderColor(type)
    val iconTint = colors.iconTint(type)

    // Determine icon to display
    val displayIcon: Any? = icon ?: if (showDefaultIcon) {
        when (type) {
            ToastType.SUCCESS -> painterResource(R.drawable.ic_success_filled)
            ToastType.ERROR -> painterResource(R.drawable.ic_error_filled)
            ToastType.WARNING -> painterResource(R.drawable.ic_warning_filled)
            ToastType.INFO -> painterResource(R.drawable.ic_info_filled)
            ToastType.DEFAULT -> null
        }
    } else null

    Surface(
        modifier = modifier
            .widthIn(min = ToastDefaults.MinWidth, max = ToastDefaults.MaxWidth)
            .shadow(elevation, shape)
            .clip(shape)
            .border(ToastDefaults.BorderWidth, borderColor, shape),
        shape = shape,
        color = colors.backgroundColor,
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = ToastDefaults.HorizontalPadding,
                    vertical = ToastDefaults.VerticalPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Icon
            displayIcon?.let { iconContent ->
                when (iconContent) {
                    is ImageVector -> {
                        Icon(
                            imageVector = iconContent,
                            contentDescription = null,
                            modifier = Modifier.size(ToastDefaults.IconSize),
                            tint = iconTint,
                        )
                    }
                    is Painter -> {
                        Icon(
                            painter = iconContent,
                            contentDescription = null,
                            modifier = Modifier.size(ToastDefaults.IconSize),
                            tint = iconTint,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ToastDefaults.IconTextSpacing))
            }

            // Title
            title?.let {
                Text(
                    text = it,
                    fontSize = ToastDefaults.TitleTextSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = colors.titleTextColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Message
            Text(
                text = message,
                fontSize = ToastDefaults.MessageTextSize,
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                color = colors.messageTextColor,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
