@file:Suppress("unused")

package com.cropintellix.volineui.compose

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.cropintellix.volineui.button.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Advanced button component for Jetpack Compose with comprehensive features.
 *
 * Features:
 * - Multiple button styles (filled, outlined, text, elevated, tonal, icon, FAB, chip)
 * - Size variants (XS, S, M, L, XL)
 * - Corner types (sharp, rounded, pill)
 * - Leading/trailing icons with independent click handlers
 * - Gradient backgrounds
 * - Loading states (spinner, dots, shimmer, progress)
 * - Success/error states with animations
 * - Disabled state
 * - Scale animations on press
 * - Click, double-click, long-press handling
 * - Haptic feedback
 *
 * @param text Button text
 * @param onClick Click callback
 * @param modifier Modifier for the button
 * @param style Button style (filled, outlined, text, etc.)
 * @param size Size variant (XS, S, M, L, XL)
 * @param enabled Whether the button is enabled
 * @param cornerType Corner shape type (sharp, rounded, pill)
 * @param colors Color configuration
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param onLeadingIconClick Optional callback for leading icon click
 * @param onTrailingIconClick Optional callback for trailing icon click
 * @param fullWidth Whether button should fill max width
 * @param useGradient Whether to use gradient background
 * @param gradientColors Gradient colors (start, end)
 * @param gradientAngle Gradient angle in degrees
 * @param isLoading Whether button is in loading state
 * @param loadingType Type of loading animation
 * @param loadingText Optional text to show during loading
 * @param progress Progress value (0-100) for progress loading type
 * @param showProgressText Whether to show progress percentage
 * @param isSuccess Whether button is in success state
 * @param isError Whether button is in error state
 * @param onDoubleClick Optional double-click callback
 * @param onLongClick Optional long-press callback
 * @param onDisabledClick Optional callback when disabled button is clicked
 * @param enableHapticFeedback Whether to provide haptic feedback
 * @param hapticIntensity Haptic feedback intensity
 * @param enableScaleAnimation Whether to scale on press
 * @param scaleAmount Scale factor when pressed
 * @param textTransform Text transformation (none, uppercase, lowercase, capitalize)
 * @param maxLines Maximum number of text lines
 * @param fontFamily Optional font family
 * @param fontWeight Font weight
 * @param letterSpacing Letter spacing
 * @param customCornerRadius Custom corner radius (overrides cornerType)
 * @param customBorderWidth Custom border width
 * @param customElevation Custom elevation
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvancedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.FILLED,
    size: ButtonSize = ButtonSize.M,
    enabled: Boolean = true,
    cornerType: CornerType = CornerType.ROUNDED,
    colors: ButtonColors = ButtonDefaults.colors(),
    leadingIcon: Any? = null, // Can be ImageVector or Painter
    trailingIcon: Any? = null, // Can be ImageVector or Painter
    onLeadingIconClick: (() -> Unit)? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    fullWidth: Boolean = true,
    useGradient: Boolean = false,
    gradientColors: Pair<Color, Color>? = null,
    gradientAngle: Float = 0f,
    isLoading: Boolean = false,
    loadingType: LoadingType = LoadingType.SPINNER,
    loadingText: String? = null,
    progress: Int = 0,
    showProgressText: Boolean = false,
    isSuccess: Boolean = false,
    isError: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onDisabledClick: (() -> Unit)? = null,
    enableHapticFeedback: Boolean = true,
    hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    enableScaleAnimation: Boolean = true,
    scaleAmount: Float = ButtonDefaults.ScaleAmount,
    textTransform: TextTransform = TextTransform.NONE,
    maxLines: Int = 1,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    customCornerRadius: Dp? = null,
    customBorderWidth: Dp? = null,
    customElevation: Dp? = null,
    customHorizontalPadding: Dp = 12.dp,
    customVerticalPadding: Dp = 16.dp,
    customMinHeight: Dp = 24.dp,
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // Size preset with customizable overrides
    val sizePreset = ButtonDefaults.getSizePreset(size)
    val actualHorizontalPadding =
        customHorizontalPadding.takeIf { it != 12.dp } ?: sizePreset.horizontalPadding
    val actualVerticalPadding =
        customVerticalPadding.takeIf { it != 16.dp } ?: sizePreset.verticalPadding
    val actualMinHeight = customMinHeight.takeIf { it != 32.dp } ?: sizePreset.minHeight

    // Interaction state
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Click handling
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var clickCount by remember { mutableIntStateOf(0) }

    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enableScaleAnimation && enabled && !isLoading) scaleAmount else 1f,
        animationSpec = tween(durationMillis = ButtonDefaults.AnimationDuration / 2),
        label = "scale"
    )

    // Success/Error animation
    var animatedBackgroundColor by remember { mutableStateOf(colors.backgroundColor) }
    LaunchedEffect(isSuccess, isError) {
        when {
            isSuccess -> {
                animatedBackgroundColor = colors.successColor
                delay(1500)
                animatedBackgroundColor = colors.backgroundColor
            }

            isError -> {
                animatedBackgroundColor = colors.errorColor
                delay(1500)
                animatedBackgroundColor = colors.backgroundColor
            }
        }
    }

    // Determine current state
    val currentState = when {
        isLoading -> ButtonState.LOADING
        isSuccess -> ButtonState.SUCCESS
        isError -> ButtonState.ERROR
        !enabled -> ButtonState.DISABLED
        isPressed -> ButtonState.PRESSED
        else -> ButtonState.NORMAL
    }

    // Get colors for current state
    val backgroundColor = when {
        isSuccess || isError -> animatedBackgroundColor
        else -> colors.backgroundColor(currentState)
    }
    val textColor = colors.textColor(currentState)
    val borderColor = colors.borderColor(currentState)
    val iconColor = colors.iconColor(currentState)

    // Corner radius
    val cornerRadius = customCornerRadius ?: when (cornerType) {
        CornerType.SHARP -> 0.dp
        CornerType.ROUNDED -> ButtonDefaults.CornerRadius
        CornerType.PILL -> sizePreset.minHeight / 2
    }

    // Border width
    val borderWidth = customBorderWidth ?: ButtonDefaults.BorderWidth

    // Elevation - flat when disabled (matches tonal/disabled behavior from View)
    val elevation = customElevation ?: when {
        currentState == ButtonState.DISABLED -> 0.dp
        style == ButtonStyle.ELEVATED -> if (isPressed) ButtonDefaults.ElevationPressed else ButtonDefaults.ElevationNormal
        style == ButtonStyle.FAB || style == ButtonStyle.EXTENDED_FAB -> 6.dp
        else -> 0.dp
    }

    // Background brush or color - apply 3D effect for depth when enabled
    val backgroundBrush = when {
        // Disabled buttons should be flat (no 3D gradient), similar to tonal style
        currentState == ButtonState.DISABLED -> null
        // Custom gradient
        useGradient && gradientColors != null && style in listOf(
            ButtonStyle.FILLED,
            ButtonStyle.ELEVATED,
            ButtonStyle.FAB,
            ButtonStyle.EXTENDED_FAB
        ) -> {
            // Match View's GradientDrawable.Orientation behavior
            // Uses discrete orientations based on angle ranges
            when {
                gradientAngle !in 45.0..<315.0 -> Brush.horizontalGradient(
                    colors = listOf(gradientColors.first, gradientColors.second)
                )

                gradientAngle in 45.0..<135.0 -> Brush.verticalGradient(
                    colors = listOf(gradientColors.second, gradientColors.first) // Bottom to Top
                )

                gradientAngle in 135.0..<225.0 -> Brush.horizontalGradient(
                    colors = listOf(gradientColors.second, gradientColors.first) // Right to Left
                )

                else -> Brush.verticalGradient(
                    colors = listOf(gradientColors.first, gradientColors.second) // Top to Bottom
                )
            }
        }
        // 3D gradient effect for filled/elevated/FAB buttons (matching View component)
        style in listOf(
            ButtonStyle.FILLED,
            ButtonStyle.ELEVATED,
            ButtonStyle.FAB,
            ButtonStyle.EXTENDED_FAB
        ) -> {
            val lighterColor = lightenColor(backgroundColor, 0.18f)
            val darkerColor = darkenColor(backgroundColor, 0.15f)
            Brush.verticalGradient(
                colors = listOf(lighterColor, backgroundColor, darkerColor)
            )
        }

        else -> null
    }

    // Adjust styling based on button style
    val actualBackgroundColor = when (style) {
        ButtonStyle.OUTLINED, ButtonStyle.TEXT, ButtonStyle.ICON -> Color.Transparent
        ButtonStyle.TONAL -> backgroundColor.copy(alpha = 0.15f)
        ButtonStyle.CHIP -> backgroundColor.copy(alpha = 0.098f)  // Matches View: 25/255
        else -> backgroundColor
    }

    // For OUTLINED style: use black as default if textColor is white (the default from colors())
    // For TEXT/TONAL/CHIP: use the textColor from colors (user can set via textColors())
    // If user used default colors(), textColor will be white - use theme primary instead
    val actualTextColor = when (style) {
        ButtonStyle.OUTLINED -> if (textColor == Color.White) Color.Black else textColor
        ButtonStyle.TEXT -> if (textColor == Color.White) Color.Black else textColor
        ButtonStyle.TONAL, ButtonStyle.CHIP ->
            if (textColor == Color.White) colors.backgroundColor else textColor
        else -> textColor
    }

    val actualIconColor = when (style) {
        ButtonStyle.OUTLINED -> if (iconColor == Color.White) Color.Black else iconColor
        ButtonStyle.TEXT -> if (iconColor == Color.White) Color.Black else iconColor
        ButtonStyle.TONAL, ButtonStyle.CHIP ->
            if (iconColor == Color.White) colors.backgroundColor else iconColor
        else -> iconColor
    }

    // For OUTLINED style: use black as default if borderColor uses default primary/pressed colors
    // This checks the NORMAL state borderColor to detect if default was used, then applies black consistently
    val normalStateBorderColor = colors.borderColor(ButtonState.NORMAL)
    val actualBorderColor = when (style) {
        ButtonStyle.OUTLINED -> if (normalStateBorderColor == colors.backgroundColor || 
                                    normalStateBorderColor == Color.Black) Color.Black else borderColor
        else -> borderColor
    }

    val border = when (style) {
        ButtonStyle.OUTLINED -> BorderStroke(borderWidth, actualBorderColor)
        ButtonStyle.CHIP -> BorderStroke(1.dp, borderColor)
        else -> null
    }

    // Text transformation
    val transformedText = when (textTransform) {
        TextTransform.UPPERCASE -> text.toUpperCase(Locale.current)
        TextTransform.LOWERCASE -> text.lowercase()
        TextTransform.CAPITALIZE -> text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        TextTransform.NONE -> text
    }

    // Loading content
    val showLoadingIndicator = isLoading && loadingType == LoadingType.SPINNER
    val showDotsAnimation = isLoading && loadingType == LoadingType.DOTS
    val showProgressBar = isLoading && loadingType == LoadingType.PROGRESS

    // Dots animation
    var dotsPhase by remember { mutableIntStateOf(0) }
    LaunchedEffect(showDotsAnimation) {
        if (showDotsAnimation) {
            while (true) {
                delay(400)
                dotsPhase = (dotsPhase + 1) % 4
            }
        }
    }

    // Success/Error icons
    val displayText = when {
        isSuccess -> "✓"
        isError -> "✗"
        showDotsAnimation -> {
            val dots = ".".repeat(dotsPhase)
            (loadingText ?: transformedText) + dots
        }

        showProgressText && showProgressBar -> "$progress%"
        isLoading && loadingText != null -> loadingText
        else -> transformedText
    }

    // Haptic feedback helper
    fun performHaptic() {
        if (!enableHapticFeedback) return
        val hapticConstant = when (hapticIntensity) {
            HapticIntensity.NONE -> return
            HapticIntensity.LIGHT -> HapticFeedbackConstants.KEYBOARD_TAP
            HapticIntensity.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
            HapticIntensity.HEAVY -> HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(hapticConstant)
    }

    // Click handling with debounce and double-click
    fun handleClick() {
        if (!enabled) {
            onDisabledClick?.invoke()
            return
        }
        if (isLoading) return

        val currentTime = System.currentTimeMillis()
        val timeSinceLastClick = currentTime - lastClickTime

        if (onDoubleClick != null && timeSinceLastClick < ButtonDefaults.DoubleClickTime) {
            clickCount++
            if (clickCount == 1) {
                performHaptic()
                onDoubleClick.invoke()
                clickCount = 0
            }
        } else {
            clickCount = 0
            coroutineScope.launch {
                delay(ButtonDefaults.DoubleClickTime)
                if (clickCount == 0 && timeSinceLastClick >= ButtonDefaults.DebounceTime) {
                    performHaptic()
                    onClick()
                }
            }
        }
        lastClickTime = currentTime
    }

    // Modifier based on style and fullWidth
    val widthModifier = when {
        style == ButtonStyle.CHIP -> Modifier.wrapContentWidth() // Chips always wrap content
        style == ButtonStyle.ICON -> Modifier.size(sizePreset.minHeight)
        style == ButtonStyle.FAB -> Modifier.size(56.dp)
        fullWidth -> Modifier.fillMaxWidth()
        else -> Modifier.widthIn(min = ButtonDefaults.MinWidth)
    }

    Surface(
        modifier = modifier
            .then(widthModifier)
            .scale(scale),
        shape = RoundedCornerShape(cornerRadius),
        color = if (backgroundBrush == null) actualBackgroundColor else Color.Transparent,
        contentColor = actualTextColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = border,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (backgroundBrush != null) {
                        Modifier.background(backgroundBrush)
                    } else Modifier
                )
                // Use dark ripple for outlined/text buttons when default white ripple is detected
                .let { mod ->
                    val actualRippleColor = when {
                        style in listOf(ButtonStyle.OUTLINED, ButtonStyle.TEXT, ButtonStyle.TONAL, ButtonStyle.CHIP)
                            && colors.rippleColor == Color(0x40FFFFFF) -> Color(0x40000000)
                        else -> colors.rippleColor
                    }
                    mod.combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = actualRippleColor),
                        // Enable clicks if: (1) button is enabled and not loading, or (2) onDisabledClick is provided
                        enabled = (enabled && !isLoading) || onDisabledClick != null,
                        onClick = { handleClick() },
                        onLongClick = if (onLongClick != null) {
                            {
                                if (enabled && !isLoading) {
                                    performHaptic()
                                    onLongClick.invoke()
                                }
                            }
                        } else null
                    )
                }
                .padding(
                    horizontal = actualHorizontalPadding,
                    vertical = actualVerticalPadding
                )
                .heightIn(min = actualMinHeight),
            contentAlignment = Alignment.Center
        ) {
            when {
                showLoadingIndicator -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(sizePreset.iconSize),
                        color = colors.loadingColor,
                        strokeWidth = 2.dp
                    )
                }

                showProgressBar -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.size(sizePreset.iconSize),
                            color = colors.loadingColor,
                            strokeWidth = 2.dp
                        )
                        if (showProgressText) {
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text(
                                text = displayText,
                                style = TextStyle(
                                    fontSize = sizePreset.textSize,
                                    color = actualTextColor,
                                    fontFamily = fontFamily,
                                    fontWeight = fontWeight,
                                    letterSpacing = letterSpacing
                                ),
                                maxLines = maxLines
                            )
                        }
                    }
                }

                style == ButtonStyle.ICON || style == ButtonStyle.FAB -> {
                    // Icon only button
                    leadingIcon?.let {
                        IconContent(
                            icon = it,
                            size = sizePreset.iconSize,
                            tint = actualIconColor,
                            onClick = if (enabled && !isLoading) onLeadingIconClick else null
                        )
                    }
                }

                else -> {
                    // Button with text and optional icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        leadingIcon?.let {
                            IconContent(
                                icon = it,
                                size = sizePreset.iconSize,
                                tint = actualIconColor,
                                onClick = if (enabled && !isLoading) onLeadingIconClick else null
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        }

                        Text(
                            text = displayText,
                            style = TextStyle(
                                fontSize = sizePreset.textSize,
                                color = actualTextColor,
                                fontFamily = fontFamily,
                                fontWeight = fontWeight,
                                letterSpacing = letterSpacing
                            ),
                            maxLines = maxLines,
                            textAlign = TextAlign.Center
                        )

                        trailingIcon?.let {
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            IconContent(
                                icon = it,
                                size = sizePreset.iconSize,
                                tint = actualIconColor,
                                onClick = if (enabled && !isLoading) onTrailingIconClick else null
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper composable for rendering icons from various sources
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconContent(
    icon: Any,
    size: Dp,
    tint: Color,
    onClick: (() -> Unit)?,
) {
    val modifier = Modifier
        .size(size)
        .then(
            if (onClick != null) {
                Modifier.combinedClickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { onClick() }
                )
            } else Modifier
        )

    when (icon) {
        is ImageVector -> {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = modifier,
                tint = tint
            )
        }

        is Painter -> {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = modifier,
                tint = tint
            )
        }
    }
}

/**
 * Lightens a color by a given factor.
 */
private fun lightenColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red + (1f - color.red) * factor).coerceIn(0f, 1f),
        green = (color.green + (1f - color.green) * factor).coerceIn(0f, 1f),
        blue = (color.blue + (1f - color.blue) * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

/**
 * Darkens a color by a given factor.
 */
private fun darkenColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * (1 - factor)).coerceIn(0f, 1f),
        green = (color.green * (1 - factor)).coerceIn(0f, 1f),
        blue = (color.blue * (1 - factor)).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}
