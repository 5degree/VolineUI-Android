package com.cropintellix.volineuiandroid.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropintellix.volineui.compose.InputField
import com.cropintellix.volineui.inputfield.InputFieldDefaults
import com.cropintellix.volineui.inputfield.InputMaskTransformation
import com.cropintellix.volineui.inputfield.ValidationType
import com.cropintellix.volineuiandroid.ui.theme.AppTheme
import com.cropintellix.volineui.R as VolineR

/**
 * Example activity demonstrating the Compose InputField component.
 * Shows various configurations and features of the InputField.
 */
class ComposeInputFieldExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    InputFieldExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputFieldExamplesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Compose InputField Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Basic InputField
        SectionTitle("Basic InputField")
        BasicInputFieldExample()

        HorizontalDivider()

        // Password Field
        SectionTitle("Password Field")
        PasswordFieldExample()

        HorizontalDivider()

        // With Validation
        SectionTitle("Email Validation")
        EmailValidationExample()

        HorizontalDivider()

        // Error State
        SectionTitle("Error State")
        ErrorStateExample()

        HorizontalDivider()

        // Success State
        SectionTitle("Success State")
        SuccessStateExample()

        HorizontalDivider()

        // With Icons
        SectionTitle("With Icons")
        IconsExample()

        HorizontalDivider()

        // Trailing unit text (Figma-style)
        SectionTitle("Trailing text (units)")
        TrailingTextExamples()

        HorizontalDivider()

        // Character Counter
        SectionTitle("Character Counter")
        CharacterCounterExample()

        HorizontalDivider()

        // Phone Mask
        SectionTitle("Phone Number Mask")
        PhoneMaskExample()

        HorizontalDivider()

        // Disabled State
        SectionTitle("Disabled State")
        DisabledExample()

        HorizontalDivider()

        // Loading State
        SectionTitle("Loading State")
        LoadingExample()

        HorizontalDivider()

        // Custom Colors
        SectionTitle("Custom Colors")
        CustomColorsExample()

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
private fun BasicInputFieldExample() {
    var text by remember { mutableStateOf("") }

    InputField(
        value = text,
        onValueChange = { text = it },
        label = "Username",
        hint = "Enter your username",
        showClearIcon = true
    )
}

@Composable
private fun PasswordFieldExample() {
    var password by remember { mutableStateOf("") }

    InputField(
        value = password,
        onValueChange = { password = it },
        label = "Password",
        hint = "Enter your password",
        isPassword = true
    )
}

@Composable
private fun EmailValidationExample() {
    var email by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf(true) }

    InputField(
        value = email,
        onValueChange = { email = it },
        label = "Email Address",
        hint = "example@email.com",
        validationType = ValidationType.EMAIL,
        isError = !isValid && email.isNotEmpty(),
        errorMessage = if (!isValid && email.isNotEmpty()) "Please enter a valid email" else null,
        onValidationResult = { isValid = it },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        )
    )
}

@Composable
private fun ErrorStateExample() {
    var text by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column {
        InputField(
            value = text,
            onValueChange = {
                text = it
                if (showError) showError = false
            },
            label = "Required Field",
            hint = "This field is required",
            isError = showError,
            errorMessage = if (showError) "This field cannot be empty" else null
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showError = text.isEmpty() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Validate")
        }
    }
}

@Composable
private fun SuccessStateExample() {
    var text by remember { mutableStateOf("Valid input") }

    InputField(
        value = text,
        onValueChange = { text = it },
        label = "Verified Field",
        hint = "Enter text",
        isSuccess = text.isNotEmpty()
    )
}

@Composable
private fun IconsExample() {
    var searchText by remember { mutableStateOf("") }

    InputField(
        value = searchText,
        onValueChange = { searchText = it },
        label = "Search",
        hint = "Search for something...",
        showClearIcon = true
    )
}

@Composable
private fun TrailingTextExamples() {
    var urea by remember { mutableStateOf("") }
    var ammonium by remember { mutableStateOf("12") }
    var longUnit by remember { mutableStateOf("100") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InputField(
            value = urea,
            onValueChange = { urea = it },
            label = "Total quantity of Urea applied",
            hint = "0",
            trailingText = "Kg/acre",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        InputField(
            value = ammonium,
            onValueChange = { ammonium = it },
            label = "Total quantity of Ammonium Sulphate applied",
            hint = "0",
            trailingText = "Kg/acre",
            showClearIcon = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        InputField(
            value = longUnit,
            onValueChange = { longUnit = it },
            label = "Trailing text + icon (text first, then icon)",
            hint = "0",
            trailingText = "Very long unit label that should ellipsize",
            trailingIcon = painterResource(VolineR.drawable.ic_warning_filled),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun CharacterCounterExample() {
    var bio by remember { mutableStateOf("") }

    InputField(
        value = bio,
        onValueChange = { bio = it },
        label = "Bio",
        hint = "Tell us about yourself",
        maxLength = 150,
        showCharacterCounter = true,
        singleLine = false,
        maxLines = 4
    )
}

@Composable
private fun PhoneMaskExample() {
    var phone by remember { mutableStateOf("") }

    InputField(
        value = phone,
        onValueChange = { phone = it },
        label = "Phone Number",
        hint = "(123) 456-7890",
        inputMask = InputMaskTransformation.PHONE_MASK,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        )
    )
}

@Composable
private fun DisabledExample() {
    InputField(
        value = "Cannot edit this",
        onValueChange = { },
        label = "Disabled Field",
        enabled = false
    )
}

@Composable
private fun LoadingExample() {
    var text by remember { mutableStateOf("Verifying...") }
    var isLoading by remember { mutableStateOf(true) }

    Column {
        InputField(
            value = text,
            onValueChange = { text = it },
            label = "Async Validation",
            hint = "Enter value",
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { isLoading = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Loading")
            }
            Button(
                onClick = { isLoading = false },
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Loading")
            }
        }
    }
}

@Composable
private fun CustomColorsExample() {
    var text by remember { mutableStateOf("") }

    InputField(
        value = text,
        onValueChange = { text = it },
        label = "Custom Styled",
        hint = "Purple theme",
        colors = InputFieldDefaults.colors(
            focusedBorderColor = Color(0xFF9C27B0),
            cursorColor = Color(0xFF9C27B0),
            labelTextColor = Color(0xFF7B1FA2)
        ),
        cornerRadius = 16.dp,
        focusedBorderWidth = 3.dp
    )
}
