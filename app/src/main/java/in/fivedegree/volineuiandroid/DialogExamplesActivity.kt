package `in`.fivedegree.volineuiandroid

import android.R.id.message
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import `in`.fivedegree.volineui.VolineDialog
import `in`.fivedegree.volineui.dialog.DialogType

/**
 * Example activity demonstrating the View-based VolineDialog component.
 * Shows various configurations and features of the VolineDialog.
 */
class DialogExamplesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog_examples)

        setupBasicDialogs()
        setupDialogTypes()
        setupActionDialogs()
        setupCustomDialogs()
    }

    private fun setupBasicDialogs() {
        // Simple dialog
        findViewById<Button>(R.id.btnSimpleDialog).setOnClickListener {
            VolineDialog.show(this) {
                title = "Simple Dialog"
                message = "This is a basic dialog with just a title, message, and OK button."
            }
        }

        // Dialog with callback
        findViewById<Button>(R.id.btnDialogWithCallback).setOnClickListener {
            VolineDialog.show(this) {
                title = "Dialog with Callback"
                message = "Click OK to see the callback in action."
                onPrimaryClick = {
                    Toast.makeText(this@DialogExamplesActivity, "OK button clicked!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupDialogTypes() {
        // Success dialog
        findViewById<Button>(R.id.btnSuccessDialog).setOnClickListener {
            VolineDialog.success(
                this,
                title = "Success!",
                message = "Your operation completed successfully."
            )
        }

        // Error dialog
        findViewById<Button>(R.id.btnErrorDialog).setOnClickListener {
            VolineDialog.error(
                this,
                title = "Error",
                message = "Something went wrong. Please try again later."
            )
        }

        // Warning dialog
        findViewById<Button>(R.id.btnWarningDialog).setOnClickListener {
            VolineDialog.warning(
                this,
                title = "Warning",
                message = "This action may have consequences. Please review before proceeding."
            )
        }

        // Info dialog
        findViewById<Button>(R.id.btnInfoDialog).setOnClickListener {
            VolineDialog.info(
                this,
                title = "Information",
                message = "Here's some useful information for you to know."
            )
        }
    }

    private fun setupActionDialogs() {
        // Confirmation dialog
        findViewById<Button>(R.id.btnConfirmDialog).setOnClickListener {
            VolineDialog.confirm(
                this,
                title = "Confirm Action",
                message = "Are you sure you want to proceed with this action?",
                confirmText = "Yes, Proceed",
                cancelText = "Cancel",
                onConfirm = {
                    Toast.makeText(this, "Confirmed!", Toast.LENGTH_SHORT).show()
                },
                onCancel = {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Destructive dialog
        findViewById<Button>(R.id.btnDestructiveDialog).setOnClickListener {
            VolineDialog.destructive(
                this,
                title = "Delete Item",
                message = "This action cannot be undone. Are you sure you want to delete this item?",
                destructiveText = "Delete",
                cancelText = "Keep",
                onDestructive = {
                    Toast.makeText(this, "Item deleted!", Toast.LENGTH_SHORT).show()
                },
                onCancel = {
                    Toast.makeText(this, "Kept the item", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Two button dialog
        findViewById<Button>(R.id.btnTwoButtonDialog).setOnClickListener {
            VolineDialog.show(this) {
                title = "Save Changes?"
                message = "You have unsaved changes. Would you like to save them before leaving?"
                primaryButtonText = "Save"
                secondaryButtonText = "Discard"
                onPrimaryClick = {
                    Toast.makeText(this@DialogExamplesActivity, "Changes saved!", Toast.LENGTH_SHORT).show()
                }
                onSecondaryClick = {
                    Toast.makeText(this@DialogExamplesActivity, "Changes discarded", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupCustomDialogs() {
        // Non-cancelable dialog
        findViewById<Button>(R.id.btnNonCancelableDialog).setOnClickListener {
            VolineDialog.show(this) {
                title = "Non-Cancelable"
                message = "This dialog cannot be dismissed by tapping outside or pressing back. You must click the button."
                setCancelable(false)
            }
        }

        // Dialog with custom button styles
        findViewById<Button>(R.id.btnCustomButtonStyles).setOnClickListener {
            VolineDialog.show(this) {
                title = "Custom Button Styles"
                message = "This dialog has custom button styles - text style for secondary button."
                primaryButtonText = "Primary"
                secondaryButtonText = "Secondary"
            }
        }

        // Dialog without gradient
        findViewById<Button>(R.id.btnNoGradientDialog).setOnClickListener {
            VolineDialog.show(this) {
                title = "Flat Button Style"
                message = "This dialog has flat (non-gradient) buttons."
            }
        }

        // Dialog with builder pattern
        findViewById<Button>(R.id.btnBuilderPatternDialog).setOnClickListener {
            VolineDialog.Builder(this)
                .title("Builder Pattern")
                .message("This dialog was created using the Builder pattern.")
                .type(DialogType.INFO)
                .primaryButton("Got it")
                .onPrimaryClick {
                    Toast.makeText(this, "Builder pattern dialog dismissed", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Custom icon dialog
        findViewById<Button>(R.id.btnCustomIconDialog).setOnClickListener {
            VolineDialog.show(this) {
                title = "Custom Icon"
                message = "This dialog has a custom icon instead of the default."
                iconRes = R.drawable.ic_launcher_foreground
                showDefaultIcon = false
            }
        }

        // No icon dialog
        findViewById<Button>(R.id.btnNoIconDialog).setOnClickListener {
            VolineDialog.show(this) {
                title = "No Icon Dialog"
                message = "This dialog has no icon at all."
                showDefaultIcon = false
            }
        }

        // Long message dialog
        findViewById<Button>(R.id.btnLongMessageDialog).setOnClickListener {
            VolineDialog.show(this) {
                title = "Terms and Conditions"
                message = "By using this application, you agree to be bound by these Terms and Conditions. " +
                        "We reserve the right to modify these terms at any time. Your continued use of the " +
                        "application constitutes acceptance of any modifications. Please read these terms carefully " +
                        "before using our services. If you do not agree with any part of these terms, please do not " +
                        "use our application."
                primaryButtonText = "I Agree"
                secondaryButtonText = "Decline"
            }
        }
    }
}
