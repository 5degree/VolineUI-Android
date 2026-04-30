@file:Suppress("unused")

package com.cropintellix.volineui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cropintellix.volineui.R
import com.cropintellix.volineui.inputfield.ImmediatePasswordTransformation
import com.cropintellix.volineui.inputfield.InputFieldColors
import com.cropintellix.volineui.inputfield.InputFieldDefaults
import com.cropintellix.volineui.inputfield.InputMaskTransformation
import com.cropintellix.volineui.inputfield.InputValidator
import com.cropintellix.volineui.inputfield.ValidationType
import kotlin.math.roundToInt

/** Regex that allows valid in-progress decimal input: "", "3", ".3", "3.", "3.14" etc. */
private val decimalInputRegex = Regex("^[0-9]*\\.?[0-9]*$")

/**
 * A comprehensive input field composable with extensive features matching the View-based InputField.
 *
 * Features:
 * - Real-time validation
 * - Multiple input types
 * - Visual states (error, success, disabled, loading)
 * - Animations (focus border, shake on error)
 * - Optional features (clear icon, character counter, prefix/suffix icons)
 * - Input masking
 * - Password field with visibility toggle
 *
 * @param value The current text value
 * @param onValueChange Called when the text value changes
 * @param modifier Modifier for the component
 * @param label Optional label text displayed above the field
 * @param hint Optional hint/placeholder text
 * @param enabled Whether the field is enabled
 * @param readOnly Whether the field is read-only
 * @param isError Whether the field is in error state
 * @param isSuccess Whether the field is in success state
 * @param isLoading Whether the field is in loading state
 * @param errorMessage Error message to display below the field
 * @param colors Color configuration for the field
 * @param cornerRadius Corner radius of the field
 * @param borderWidth Border width in normal state
 * @param focusedBorderWidth Border width when focused
 * @param leadingIcon Optional icon at the start of the field
 * @param trailingText Optional suffix text (e.g. "Kg/acre") shown in a trailing slot
 * @param trailingIcon Optional icon at the end of the field
 * @param showClearIcon Whether to show a clear button when text is present
 * @param keyboardOptions Keyboard configuration
 * @param keyboardActions Keyboard action handlers
 * @param isPassword Whether this is a password field
 * @param maxLength Maximum character length
 * @param showCharacterCounter Whether to show character counter
 * @param singleLine Whether the field is single line
 * @param maxLines Maximum number of lines for multi-line input
 * @param validationType Type of validation to apply
 * @param customValidationPattern Custom regex pattern for validation
 * @param inputMask Input mask pattern (e.g., "(###) ###-####")
 * @param maskCharacter Character used as placeholder in mask
 * @param allowedChars Optional whitelist of allowed characters. When provided, any other character is ignored.
 * @param onValidationResult Callback for validation result
 * @param textOverflow How visual overflow is handled for label, hint, and error text (default end ellipsis).
 */
@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    colors: InputFieldColors = InputFieldDefaults.colors(),
    cornerRadius: Dp = InputFieldDefaults.CornerRadius,
    borderWidth: Dp = InputFieldDefaults.BorderWidth,
    focusedBorderWidth: Dp = InputFieldDefaults.FocusedBorderWidth,
    leadingIcon: Painter? = null,
    trailingText: String? = null,
    trailingIcon: Painter? = null,
    showClearIcon: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false,
    maxLength: Int = Int.MAX_VALUE,
    showCharacterCounter: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    validationType: ValidationType = ValidationType.NONE,
    customValidationPattern: String = "",
    inputMask: String = "",
    maskCharacter: Char = '#',
    allowedChars: List<String> = emptyList(),
    onValidationResult: ((Boolean) -> Unit)? = null,
    textOverflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val effectiveHint = hint ?: label?.takeIf { it.isNotBlank() }?.let { "Enter $it" }
    val showTrailingSlot = !trailingText.isNullOrBlank()
    val allowedCharacterSet = remember(allowedChars) { allowedChars.flatMap { it.asIterable() }.toSet() }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    rememberCoroutineScope()

    // Password visibility state
    var passwordVisible by remember { mutableStateOf(false) }

    // Shake animation for error
    val shakeOffset = remember { Animatable(0f) }

    // Focus manager to toggle keyboard visibility
    val focusManager = LocalFocusManager.current

    // Trigger shake animation when error state becomes true
    LaunchedEffect(isError, errorMessage) {
        if (isError && !errorMessage.isNullOrEmpty()) {
            // Shake animation
            repeat(3) {
                shakeOffset.animateTo(
                    targetValue = 10f,
                    animationSpec = tween(durationMillis = 50)
                )
                shakeOffset.animateTo(
                    targetValue = -10f,
                    animationSpec = tween(durationMillis = 50)
                )
            }
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessHigh)
            )
        }
    }

    // Validation
    LaunchedEffect(value, validationType) {
        if (validationType != ValidationType.NONE && value.isNotEmpty()) {
            val isValid = when (validationType) {
                ValidationType.EMAIL -> InputValidator.isValidEmail(value)
                ValidationType.PHONE -> InputValidator.isValidPhone(value)
                ValidationType.URL -> InputValidator.isValidUrl(value)
                ValidationType.CUSTOM -> {
                    if (customValidationPattern.isNotEmpty()) {
                        InputValidator.isValidCustomPattern(value, customValidationPattern)
                    } else true
                }
                else -> true
            }
            onValidationResult?.invoke(isValid)
        }
    }

    // Determine current border color with animation
    val animatedBorderColor by animateColorAsState(
        targetValue = colors.borderColor(
            enabled = enabled,
            isError = isError,
            isSuccess = isSuccess,
            isFocused = isFocused,
            isLoading = isLoading
        ),
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )

    // Determine current border width
    val currentBorderWidth = when {
        !enabled -> borderWidth
        readOnly -> borderWidth
        isFocused -> focusedBorderWidth
        else -> borderWidth
    }

    // Visual transformation
    val visualTransformation = when {
        isPassword && !passwordVisible -> ImmediatePasswordTransformation()
        inputMask.isNotEmpty() -> InputMaskTransformation(inputMask, maskCharacter)
        else -> VisualTransformation.None
    }

    // Handle text change with max length
    val handleValueChange: (String) -> Unit = { newValue ->
        // For masked input, extract only the input characters
        val unmaskedValue = if (inputMask.isNotEmpty()) {
            newValue.filter { it.isLetterOrDigit() }
        } else {
            newValue
        }
        val actualValue = if (allowedCharacterSet.isEmpty()) {
            unmaskedValue
        } else {
            unmaskedValue.filter { it in allowedCharacterSet }
        }

        // Decimal validation guard: silently reject input that would produce an
        // invalid decimal token (e.g. "3...", ".3.", "4.4.", "...").
        // In-progress values like ".3" or "3." are still allowed.
        val isValidDecimal = keyboardOptions.keyboardType != KeyboardType.Decimal ||
            actualValue.isEmpty() ||
            decimalInputRegex.matches(actualValue)

        if (isValidDecimal && actualValue.length <= maxLength) {
            onValueChange(actualValue)
            if (actualValue.length == maxLength) {
                focusManager.clearFocus()
            }
        }
    }

    Column(
        modifier = modifier
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
    ) {
        // Label
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = InputFieldDefaults.LabelTextSize,
                    color = colors.labelTextColor
                ),
                maxLines = 2,
                overflow = textOverflow,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(InputFieldDefaults.LabelGap))
        }

        // Input field container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(colors.backgroundColor(enabled))
                .border(
                    width = currentBorderWidth,
                    color = animatedBorderColor,
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val trailingTextMaxWidth =
                    (maxWidth * InputFieldDefaults.TrailingTextMaxWidthFraction).coerceAtLeast(48.dp)
                val trailingDividerHeight = InputFieldDefaults.TrailingDividerHeight
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = InputFieldDefaults.MinHeight)
                        .padding(
                            horizontal = InputFieldDefaults.HorizontalPadding,
                            vertical = InputFieldDefaults.VerticalPadding
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leading icon
                    if (leadingIcon != null) {
                        Icon(
                            painter = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(InputFieldDefaults.IconSize),
                            tint = colors.iconColor
                        )
                        Spacer(modifier = Modifier.width(InputFieldDefaults.IconPadding))
                    }

                    // Text field
                    BasicTextField(
                        value = value,
                        onValueChange = handleValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        enabled = enabled && !readOnly,
                        readOnly = readOnly,
                        textStyle = TextStyle(
                            fontSize = InputFieldDefaults.TextSize,
                            color = colors.textColor(enabled)
                        ),
                        keyboardOptions = if (isPassword) {
                            keyboardOptions.copy(keyboardType = KeyboardType.Password)
                        } else {
                            keyboardOptions
                        },
                        keyboardActions = keyboardActions,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                        cursorBrush = SolidColor(colors.cursorColor),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (value.isEmpty() && !effectiveHint.isNullOrEmpty()) {
                                    Text(
                                        text = effectiveHint,
                                        style = TextStyle(
                                            fontSize = InputFieldDefaults.TextSize,
                                            color = colors.hintTextColor
                                        ),
                                        maxLines = if (singleLine) 1 else maxLines,
                                        overflow = textOverflow,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // Trailing icons + optional divider / unit text / trailing icon
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Loading indicator
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(InputFieldDefaults.IconSize),
                                color = colors.loadingColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Password toggle
                        if (isPassword) {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible) R.drawable.ic_visibility
                                    else R.drawable.ic_visibility_off
                                ),
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                modifier = Modifier
                                    .size(InputFieldDefaults.IconSize)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        passwordVisible = !passwordVisible
                                    },
                                tint = colors.iconColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Clear icon
                        if (showClearIcon && value.isNotEmpty() && enabled && !readOnly) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clear),
                                contentDescription = "Clear",
                                modifier = Modifier
                                    .size(InputFieldDefaults.IconSize)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onValueChange("")
                                    },
                                tint = colors.iconColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (showTrailingSlot) {
                            Spacer(modifier = Modifier.width(InputFieldDefaults.TrailingInputToDividerGap))
                            VerticalDivider(
                                modifier = Modifier.height(trailingDividerHeight.coerceAtLeast(1.dp)),
                                thickness = InputFieldDefaults.TrailingDividerWidth,
                                color = colors.trailingDividerColor,
                            )
                            Text(
                                text = trailingText.trim(),
                                style = TextStyle(
                                    fontSize = InputFieldDefaults.TextSize,
                                    color = colors.trailingTextColor(enabled)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(
                                        start = InputFieldDefaults.TrailingTextStartPadding,
                                        end = if (trailingIcon != null) {
                                            InputFieldDefaults.TrailingTextEndPadding
                                        } else {
                                            0.dp
                                        },
                                    )
                                    .widthIn(max = trailingTextMaxWidth)
                            )
                        }

                        if (trailingIcon != null) {
                            Icon(
                                painter = trailingIcon,
                                contentDescription = null,
                                modifier = Modifier.size(InputFieldDefaults.IconSize),
                                tint = colors.iconColor
                            )
                        }
                    }
                }
            }
        }

        // Error message and counter row
        if (!errorMessage.isNullOrEmpty() || showCharacterCounter) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Error message
                if (!errorMessage.isNullOrEmpty() && isError) {
                    Text(
                        text = errorMessage,
                        style = TextStyle(
                            fontSize = InputFieldDefaults.ErrorTextSize,
                            color = colors.errorColor
                        ),
                        maxLines = 3,
                        overflow = textOverflow,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Character counter
                if (showCharacterCounter) {
                    val counterText = if (maxLength < Int.MAX_VALUE) {
                        "${value.length} / $maxLength"
                    } else {
                        "${value.length}"
                    }
                    Text(
                        text = counterText,
                        style = TextStyle(
                            fontSize = InputFieldDefaults.CounterTextSize,
                            color = colors.hintTextColor
                        ),
                        maxLines = 1,
                        overflow = textOverflow
                    )
                }
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
private fun InputFieldPreview() {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InputField(
            value = text,
            onValueChange = { text = it },
            label = "Username",
            hint = "Enter your username"
        )

        InputField(
            value = text,
            onValueChange = { text = it },
            label = "Password",
            hint = "Enter your password",
            isPassword = true
        )

        InputField(
            value = text,
            onValueChange = { text = it },
            label = "Email",
            hint = "Enter your email",
            isError = true,
            errorMessage = "Invalid email address"
        )

        InputField(
            value = text,
            onValueChange = { text = it },
            label = "With Counter",
            hint = "Max 50 characters",
            maxLength = 50,
            showCharacterCounter = true,
            showClearIcon = true
        )

        InputField(
            value = text,
            onValueChange = { text = it },
            label = "Disabled",
            hint = "Cannot edit",
            enabled = false
        )

        InputField(
            value = text,
            onValueChange = { text = it },
            label = "Quantity (trailing unit)",
            hint = "0",
            trailingText = "Kg/acre",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        InputField(
            value = text,
            onValueChange = { text = it },
            label = "Unit + trailing icon",
            hint = "0",
            trailingText = "Very long unit that ellipsize…",
            trailingIcon = painterResource(id = R.drawable.ic_clear)
        )
    }
}
