package `in`.fivedegree.volineuiandroid.compose

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.fivedegree.volineui.compose.VolineDialog
import `in`.fivedegree.volineuiandroid.ui.theme.AppTheme

/**
 * Example activity demonstrating the Compose VolineDialog component.
 * Shows the simplified API for dialog display.
 */
class ComposeDialogExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                // Add dialog container once at the top level
                VolineDialog.Container()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DialogExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogExamplesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

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
            text = "Compose Dialog Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Simple API: Just call VolineDialog.show(...) from anywhere!",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Basic Dialogs
        SectionTitle("Basic Dialogs")

        Button(
            onClick = {
                VolineDialog.show(
                    title = "Simple Dialog",
                    message = "This is a basic dialog with just a title, message, and OK button."
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simple Dialog")
        }

        Button(
            onClick = {
                VolineDialog.show(
                    title = "Dialog with Callback",
                    message = "Click OK to see the callback in action.",
                    onPrimaryClick = {
                        Toast.makeText(context, "OK button clicked!", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dialog with Callback")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Dialog Types
        SectionTitle("Dialog Types")

        Button(
            onClick = {
                VolineDialog.success(
                    title = "Success!",
                    message = "Your operation completed successfully."
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Success Dialog")
        }

        Button(
            onClick = {
                VolineDialog.error(
                    title = "Error",
                    message = "Something went wrong. Please try again later."
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Error Dialog")
        }

        Button(
            onClick = {
                VolineDialog.warning(
                    title = "Warning",
                    message = "This action may have consequences. Please review before proceeding."
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Warning Dialog")
        }

        Button(
            onClick = {
                VolineDialog.info(
                    title = "Information",
                    message = "Here's some useful information for you to know."
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Info Dialog")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Action Dialogs
        SectionTitle("Action Dialogs")

        Button(
            onClick = {
                VolineDialog.confirm(
                    title = "Confirm Action",
                    message = "Are you sure you want to proceed with this action?",
                    confirmText = "Yes, Proceed",
                    cancelText = "Cancel",
                    onConfirm = {
                        Toast.makeText(context, "Confirmed!", Toast.LENGTH_SHORT).show()
                    },
                    onCancel = {
                        Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirmation Dialog")
        }

        Button(
            onClick = {
                VolineDialog.destructive(
                    title = "Delete Item",
                    message = "This action cannot be undone. Are you sure you want to delete this item?",
                    destructiveText = "Delete",
                    cancelText = "Keep",
                    onDestructive = {
                        Toast.makeText(context, "Item deleted!", Toast.LENGTH_SHORT).show()
                    },
                    onCancel = {
                        Toast.makeText(context, "Kept the item", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Destructive Dialog")
        }

        Button(
            onClick = {
                VolineDialog.show(
                    title = "Save Changes?",
                    message = "You have unsaved changes. Would you like to save them before leaving?",
                    primaryButtonText = "Save",
                    secondaryButtonText = "Discard",
                    onPrimaryClick = {
                        Toast.makeText(context, "Changes saved!", Toast.LENGTH_SHORT).show()
                    },
                    onSecondaryClick = {
                        Toast.makeText(context, "Changes discarded", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Two Button Dialog")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Custom Dialogs
        SectionTitle("Custom Dialogs")

        Button(
            onClick = {
                VolineDialog.show(
                    title = "Non-Cancelable",
                    message = "This dialog cannot be dismissed by tapping outside or pressing back. You must click the button.",
                    isCancelable = false
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Non-Cancelable Dialog")
        }

        Button(
            onClick = {
                VolineDialog.show(
                    title = "No Icon Dialog",
                    message = "This dialog has no icon at all.",
                    showDefaultIcon = false
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("No Icon Dialog")
        }

        Button(
            onClick = {
                VolineDialog.show(
                    title = "Terms and Conditions",
                    message = "By using this application, you agree to be bound by these Terms and Conditions. " +
                            "We reserve the right to modify these terms at any time. Your continued use of the " +
                            "application constitutes acceptance of any modifications. Please read these terms carefully " +
                            "before using our services.",
                    primaryButtonText = "I Agree",
                    secondaryButtonText = "Decline"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Long Message Dialog")
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
