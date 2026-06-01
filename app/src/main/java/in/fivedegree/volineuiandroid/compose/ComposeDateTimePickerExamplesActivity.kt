package `in`.fivedegree.volineuiandroid.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.fivedegree.volineui.compose.DateTimePicker
import `in`.fivedegree.volineui.compose.DateTimePickerMode
import `in`.fivedegree.volineui.compose.Dropdown
import `in`.fivedegree.volineui.compose.InputField
import `in`.fivedegree.volineui.dropdown.DropdownOption
import `in`.fivedegree.volineuiandroid.R
import `in`.fivedegree.volineuiandroid.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComposeDateTimePickerExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DateTimePickerExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTimePickerExamplesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Compose DateTimePicker Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        SectionTitle("Date Only")
        DateOnlyExample()

        HorizontalDivider()

        SectionTitle("Time Only")
        TimeOnlyExample()

        HorizontalDivider()

        SectionTitle("Date & Time")
        DateTimeExample()

        HorizontalDivider()

        SectionTitle("With Min/Max Date")
        MinMaxDateExample()

        HorizontalDivider()

        SectionTitle("Error State")
        ErrorStateExample()

        HorizontalDivider()

        SectionTitle("Disabled State")
        DisabledStateExample()

        HorizontalDivider()

        SectionTitle("Form — Mixed with InputField & Dropdown")
        FormExample()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF666666)
    )
}

@Composable
private fun DateOnlyExample() {
    var selectedMillis by remember { mutableStateOf<Long?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DateTimePicker(
            selectedMillis = selectedMillis,
            onDateTimeSelected = { selectedMillis = it },
            label = "Date of Birth",
            hint = "Select your birth date",
            trailingIcon = R.drawable.ic_calendar
        )

        SelectionDisplay(selectedMillis)
    }
}

@Composable
private fun TimeOnlyExample() {
    var selectedMillis by remember { mutableStateOf<Long?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DateTimePicker(
            selectedMillis = selectedMillis,
            onDateTimeSelected = { selectedMillis = it },
            label = "Alarm Time",
            mode = DateTimePickerMode.TIME_ONLY,
            trailingIcon = R.drawable.ic_clock
        )

        SelectionDisplay(selectedMillis, "hh:mm a")
    }
}

@Composable
private fun DateTimeExample() {
    var selectedMillis by remember { mutableStateOf<Long?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DateTimePicker(
            selectedMillis = selectedMillis,
            onDateTimeSelected = { selectedMillis = it },
            label = "Appointment",
            mode = DateTimePickerMode.DATE_TIME,
            leadingIcon = R.drawable.ic_calendar,
            trailingIcon = R.drawable.ic_clock
        )

        SelectionDisplay(selectedMillis, "dd MMM yyyy, hh:mm a")
    }
}

@Composable
private fun MinMaxDateExample() {
    var selectedMillis by remember { mutableStateOf<Long?>(null) }
    val now = System.currentTimeMillis()
    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DateTimePicker(
            selectedMillis = selectedMillis,
            onDateTimeSelected = { selectedMillis = it },
            label = "Event Date (next 30 days)",
            hint = "Choose a date within next 30 days",
            minDateMillis = now,
            maxDateMillis = now + thirtyDaysMs
        )

        SelectionDisplay(selectedMillis)
    }
}

@Composable
private fun ErrorStateExample() {
    var selectedMillis by remember { mutableStateOf<Long?>(null) }

    DateTimePicker(
        selectedMillis = selectedMillis,
        onDateTimeSelected = { selectedMillis = it },
        label = "Required Date",
        isError = selectedMillis == null,
        errorMessage = if (selectedMillis == null) "This field is required" else null
    )
}

@Composable
private fun DisabledStateExample() {
    DateTimePicker(
        selectedMillis = System.currentTimeMillis(),
        onDateTimeSelected = {},
        label = "Locked Date",
        enabled = false
    )
}

@Composable
private fun FormExample() {
    var name by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf<DropdownOption?>(null) }
    var birthDate by remember { mutableStateOf<Long?>(null) }
    var appointmentDateTime by remember { mutableStateOf<Long?>(null) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }

    val cities = listOf(
        DropdownOption("New York"),
        DropdownOption("London"),
        DropdownOption("Tokyo"),
        DropdownOption("Mumbai"),
        DropdownOption("Sydney"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InputField(
            value = name,
            onValueChange = { name = it },
            label = "Full Name",
            hint = "Enter your full name"
        )

        Dropdown(
            options = cities,
            selectedOption = selectedCity,
            onSelectionChange = { selectedCity = it },
            label = "City"
        )

        DateTimePicker(
            selectedMillis = birthDate,
            onDateTimeSelected = { birthDate = it },
            label = "Date of Birth",
            mode = DateTimePickerMode.DATE_ONLY
        )

        DateTimePicker(
            selectedMillis = appointmentDateTime,
            onDateTimeSelected = { appointmentDateTime = it },
            label = "Appointment Date & Time",
            mode = DateTimePickerMode.DATE_TIME
        )

        DateTimePicker(
            selectedMillis = reminderTime,
            onDateTimeSelected = { reminderTime = it },
            label = "Reminder Time",
            mode = DateTimePickerMode.TIME_ONLY
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = buildString {
                appendLine("Form Values:")
                appendLine("Name: ${name.ifEmpty { "-" }}")
                appendLine("City: ${selectedCity?.text ?: "-"}")
                appendLine("DOB: ${birthDate?.toFormattedDate("dd MMM yyyy") ?: "-"}")
                appendLine("Appointment: ${appointmentDateTime?.toFormattedDate("dd MMM yyyy, hh:mm a") ?: "-"}")
                append("Reminder: ${reminderTime?.toFormattedDate("hh:mm a") ?: "-"}")
            },
            fontSize = 14.sp,
            color = Color(0xFF333333),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(12.dp)
        )
    }
}

@Composable
private fun SelectionDisplay(millis: Long?, pattern: String = "dd MMM yyyy") {
    val text = millis?.let {
        "Selected: ${it.toFormattedDate(pattern)}  (millis: $it)"
    } ?: "Selected: -"

    Text(
        text = text,
        fontSize = 13.sp,
        color = Color(0xFF888888),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9))
            .padding(8.dp)
    )
}

private fun Long.toFormattedDate(pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
