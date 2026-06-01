package `in`.fivedegree.volineuiandroid.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.fivedegree.volineui.compose.VolineToast
import `in`.fivedegree.volineui.toast.ToastDuration
import `in`.fivedegree.volineui.toast.ToastPosition
import `in`.fivedegree.volineuiandroid.ui.theme.AppTheme

/**
 * Example activity demonstrating the Compose VolineToast component.
 * Shows the simplified API for toast display.
 */
class ComposeToastExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                // Add toast container once at the top level
                VolineToast.Container()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ToastExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToastExamplesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Text(
            text = "Compose Toast Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Simple API: Just call VolineToast.show(...) from anywhere!",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Basic Toasts
        SectionTitle("Basic Toasts")

        Button(
            onClick = { VolineToast.show("This is a default toast message") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Default Toast")
        }

        Button(
            onClick = {
                VolineToast.show(
                    message = "This is the message body of the toast",
                    title = "Toast Title"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toast with Title")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Toast Types
        SectionTitle("Toast Types")

        Button(
            onClick = { VolineToast.success("Operation completed successfully!") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Success Toast")
        }

        Button(
            onClick = {
                VolineToast.success(
                    message = "Your data has been saved.",
                    title = "Success"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Success Toast with Title")
        }

        Button(
            onClick = { VolineToast.error("Something went wrong. Please try again.") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Error Toast")
        }

        Button(
            onClick = {
                VolineToast.error(
                    message = "Unable to connect to server.",
                    title = "Connection Error"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Error Toast with Title")
        }

        Button(
            onClick = { VolineToast.warning("Your session is about to expire.") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Warning Toast")
        }

        Button(
            onClick = {
                VolineToast.warning(
                    message = "Battery level is low.",
                    title = "Warning"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Warning Toast with Title")
        }

        Button(
            onClick = { VolineToast.info("A new update is available.") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Info Toast")
        }

        Button(
            onClick = {
                VolineToast.info(
                    message = "Check out our new features!",
                    title = "What's New"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Info Toast with Title")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Toast Positions
        SectionTitle("Toast Positions")

        Button(
            onClick = {
                VolineToast.success(
                    message = "This toast appears at the top",
                    position = ToastPosition.TOP
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toast at Top")
        }

        Button(
            onClick = {
                VolineToast.info(
                    message = "This toast appears at the center",
                    position = ToastPosition.CENTER
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toast at Center (Default)")
        }

        Button(
            onClick = {
                VolineToast.warning(
                    message = "This toast appears at the bottom",
                    position = ToastPosition.BOTTOM
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toast at Bottom")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Toast Durations
        SectionTitle("Toast Durations")

        Button(
            onClick = {
                VolineToast.show(
                    message = "This toast disappears quickly (2s)",
                    duration = ToastDuration.SHORT
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Short Duration (2s)")
        }

        Button(
            onClick = {
                VolineToast.show(
                    message = "This toast has medium duration (3.5s)",
                    duration = ToastDuration.MEDIUM
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Medium Duration (3.5s)")
        }

        Button(
            onClick = {
                VolineToast.show(
                    message = "This toast stays longer (5s)",
                    duration = ToastDuration.LONG
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Long Duration (5s)")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Dismiss
        SectionTitle("Manual Dismiss")

        Button(
            onClick = { VolineToast.dismiss() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dismiss Current Toast")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF666666),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}
