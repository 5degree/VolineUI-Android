@file:Suppress("unused")

package `in`.fivedegree.volineui.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.fivedegree.volineui.inputfield.InputFieldColors
import `in`.fivedegree.volineui.inputfield.InputFieldDefaults
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Picker mode for the DateTimePicker component.
 */
enum class DateTimePickerMode {
    DATE_ONLY,
    TIME_ONLY,
    DATE_TIME
}

/**
 * A date/time picker composable that visually matches [InputField].
 *
 * The component renders as a read-only input field. Tapping it opens the
 * appropriate Material 3 picker dialog(s). On confirmation the selected
 * value is delivered as epoch milliseconds via [onDateTimeSelected].
 *
 * For [DateTimePickerMode.DATE_TIME] the date picker is shown first,
 * followed by the time picker after a date is confirmed.
 *
 * @param selectedMillis Currently selected date/time in epoch milliseconds, or null
 * @param onDateTimeSelected Called with the selected epoch milliseconds when the user confirms
 * @param modifier Modifier for the component
 * @param mode Picker mode: date only, time only, or date + time
 * @param label Optional label text displayed above the field
 * @param hint Optional hint/placeholder text
 * @param enabled Whether the picker is interactive
 * @param isError Whether the field is in error state
 * @param isSuccess Whether the field is in success state
 * @param errorMessage Error message to display below the field
 * @param colors Color configuration, reuses [InputFieldColors]
 * @param cornerRadius Corner radius of the field
 * @param borderWidth Border width in normal state
 * @param focusedBorderWidth Border width when the dialog is open
 * @param leadingIcon Optional drawable resource ID for the icon at the start of the field
 * @param trailingIcon Optional drawable resource ID for the icon at the end of the field
 * @param dateFormat [SimpleDateFormat] pattern for date display
 * @param timeFormat [SimpleDateFormat] pattern for time display
 * @param dateTimeFormat [SimpleDateFormat] pattern for combined date+time display
 * @param minDateMillis Optional minimum selectable date in epoch milliseconds
 * @param maxDateMillis Optional maximum selectable date in epoch milliseconds
 * @param is24Hour Whether to use 24-hour format in the time picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    selectedMillis: Long?,
    onDateTimeSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    mode: DateTimePickerMode = DateTimePickerMode.DATE_ONLY,
    label: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    errorMessage: String? = null,
    colors: InputFieldColors = InputFieldDefaults.colors(),
    cornerRadius: Dp = InputFieldDefaults.CornerRadius,
    borderWidth: Dp = InputFieldDefaults.BorderWidth,
    focusedBorderWidth: Dp = InputFieldDefaults.FocusedBorderWidth,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    dateFormat: String = "dd MMM yyyy",
    timeFormat: String = "hh:mm a",
    dateTimeFormat: String = "dd MMM yyyy, hh:mm a",
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    is24Hour: Boolean = false,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    val dialogOpen = showDatePicker || showTimePicker

    val effectiveHint = hint ?: when (mode) {
        DateTimePickerMode.DATE_ONLY -> label?.takeIf { it.isNotBlank() }?.let { "Select $it" } ?: "Select date"
        DateTimePickerMode.TIME_ONLY -> label?.takeIf { it.isNotBlank() }?.let { "Select $it" } ?: "Select time"
        DateTimePickerMode.DATE_TIME -> label?.takeIf { it.isNotBlank() }?.let { "Select $it" } ?: "Select date & time"
    }

    val displayText = selectedMillis?.let {
        val pattern = when (mode) {
            DateTimePickerMode.DATE_ONLY -> dateFormat
            DateTimePickerMode.TIME_ONLY -> timeFormat
            DateTimePickerMode.DATE_TIME -> dateTimeFormat
        }
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(it))
    } ?: ""

    val animatedBorderColor by animateColorAsState(
        targetValue = colors.borderColor(
            enabled = enabled,
            isError = isError,
            isSuccess = isSuccess,
            isFocused = dialogOpen,
            isLoading = false
        ),
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )

    val currentBorderWidth = when {
        !enabled -> borderWidth
        dialogOpen -> focusedBorderWidth
        else -> borderWidth
    }

    Column(modifier = modifier) {
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = InputFieldDefaults.LabelTextSize,
                    color = colors.labelTextColor
                )
            )
            Spacer(modifier = Modifier.height(InputFieldDefaults.LabelGap))
        }

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
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    when (mode) {
                        DateTimePickerMode.DATE_ONLY -> showDatePicker = true
                        DateTimePickerMode.TIME_ONLY -> showTimePicker = true
                        DateTimePickerMode.DATE_TIME -> showDatePicker = true
                    }
                }
        ) {
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
                if (leadingIcon != null) {
                    Icon(
                        painter = painterResource(id = leadingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(InputFieldDefaults.IconSize),
                        tint = colors.iconColor
                    )
                    Spacer(modifier = Modifier.width(InputFieldDefaults.IconPadding))
                }

                Text(
                    text = displayText.ifEmpty { effectiveHint },
                    style = TextStyle(
                        fontSize = InputFieldDefaults.TextSize,
                        color = if (displayText.isNotEmpty()) {
                            colors.textColor(enabled)
                        } else {
                            colors.hintTextColor
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (trailingIcon != null) {
                    Icon(
                        painter = painterResource(id = trailingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(InputFieldDefaults.IconSize),
                        tint = colors.iconColor
                    )
                }
            }
        }

        if (!errorMessage.isNullOrEmpty() && isError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = TextStyle(
                    fontSize = InputFieldDefaults.ErrorTextSize,
                    color = colors.errorColor
                )
            )
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val initialMillis = when (mode) {
            DateTimePickerMode.DATE_TIME -> pendingDateMillis ?: selectedMillis
            else -> selectedMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val afterMin = minDateMillis?.let { utcTimeMillis >= it } ?: true
                    val beforeMax = maxDateMillis?.let { utcTimeMillis <= it } ?: true
                    return afterMin && beforeMax
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                pendingDateMillis = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            showDatePicker = false
                            if (mode == DateTimePickerMode.DATE_TIME) {
                                pendingDateMillis = picked
                                showTimePicker = true
                            } else {
                                onDateTimeSelected(picked)
                            }
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        pendingDateMillis = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            selectedMillis?.let { timeInMillis = it }
        }

        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = is24Hour
        )

        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
                pendingDateMillis = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false

                        val baseMillis = when (mode) {
                            DateTimePickerMode.DATE_TIME -> pendingDateMillis ?: System.currentTimeMillis()
                            else -> selectedMillis ?: System.currentTimeMillis()
                        }

                        val result = Calendar.getInstance().apply {
                            timeInMillis = baseMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        pendingDateMillis = null
                        onDateTimeSelected(result.timeInMillis)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        pendingDateMillis = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    text = when (mode) {
                        DateTimePickerMode.DATE_TIME -> "Select Time"
                        else -> "Select Time"
                    }
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
private fun DateTimePickerDateOnlyPreview() {
    var selectedMillis by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DateTimePicker(
            selectedMillis = selectedMillis,
            onDateTimeSelected = { selectedMillis = it },
            label = "Date of Birth",
            hint = "Select your birth date"
        )

        DateTimePicker(
            selectedMillis = System.currentTimeMillis(),
            onDateTimeSelected = {},
            label = "Pre-filled Date"
        )

        DateTimePicker(
            selectedMillis = null,
            onDateTimeSelected = {},
            label = "Required Date",
            isError = true,
            errorMessage = "This field is required"
        )

        DateTimePicker(
            selectedMillis = null,
            onDateTimeSelected = {},
            label = "Disabled Date",
            enabled = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DateTimePickerTimeModePreview() {
    var selectedMillis by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DateTimePicker(
            selectedMillis = selectedMillis,
            onDateTimeSelected = { selectedMillis = it },
            label = "Alarm Time",
            mode = DateTimePickerMode.TIME_ONLY
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DateTimePickerDateTimeModePreview() {
    var selectedMillis by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DateTimePicker(
            selectedMillis = selectedMillis,
            onDateTimeSelected = { selectedMillis = it },
            label = "Appointment",
            mode = DateTimePickerMode.DATE_TIME
        )
    }
}
