package `in`.fivedegree.volineuiandroid

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import `in`.fivedegree.volineui.VolineToast
import `in`.fivedegree.volineui.toast.ToastDuration
import `in`.fivedegree.volineui.toast.ToastPosition

/**
 * Example activity demonstrating the View-based VolineToast component.
 * Shows various configurations and features of the VolineToast.
 */
class ToastExamplesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toast_examples)

        setupBasicToasts()
        setupToastTypes()
        setupToastPositions()
        setupToastDurations()
        setupCustomToasts()
    }

    private fun setupBasicToasts() {
        // Default toast
        findViewById<Button>(R.id.btnDefaultToast).setOnClickListener {
            VolineToast.show(this, "This is a default toast message")
        }

        // Toast with title
        findViewById<Button>(R.id.btnToastWithTitle).setOnClickListener {
            VolineToast.show(
                this,
                message = "This is the message body of the toast",
                title = "Toast Title"
            )
        }
    }

    private fun setupToastTypes() {
        // Success toast
        findViewById<Button>(R.id.btnSuccessToast).setOnClickListener {
            VolineToast.success(this, "Operation completed successfully!")
        }

        // Success toast with title
        findViewById<Button>(R.id.btnSuccessToastWithTitle).setOnClickListener {
            VolineToast.success(
                this,
                message = "Your data has been saved.",
                title = "Success"
            )
        }

        // Error toast
        findViewById<Button>(R.id.btnErrorToast).setOnClickListener {
            VolineToast.error(this, "Something went wrong. Please try again.")
        }

        // Error toast with title
        findViewById<Button>(R.id.btnErrorToastWithTitle).setOnClickListener {
            VolineToast.error(
                this,
                message = "Unable to connect to server.",
                title = "Connection Error"
            )
        }

        // Warning toast
        findViewById<Button>(R.id.btnWarningToast).setOnClickListener {
            VolineToast.warning(this, "Your session is about to expire.")
        }

        // Warning toast with title
        findViewById<Button>(R.id.btnWarningToastWithTitle).setOnClickListener {
            VolineToast.warning(
                this,
                message = "Battery level is low.",
                title = "Warning"
            )
        }

        // Info toast
        findViewById<Button>(R.id.btnInfoToast).setOnClickListener {
            VolineToast.info(this, "A new update is available.")
        }

        // Info toast with title
        findViewById<Button>(R.id.btnInfoToastWithTitle).setOnClickListener {
            VolineToast.info(
                this,
                message = "Check out our new features!",
                title = "What's New"
            )
        }
    }

    private fun setupToastPositions() {
        // Top position
        findViewById<Button>(R.id.btnToastTop).setOnClickListener {
            VolineToast.success(
                this,
                message = "This toast appears at the top",
                position = ToastPosition.TOP
            )
        }

        // Center position (default)
        findViewById<Button>(R.id.btnToastCenter).setOnClickListener {
            VolineToast.info(
                this,
                message = "This toast appears at the center",
                position = ToastPosition.CENTER
            )
        }

        // Bottom position
        findViewById<Button>(R.id.btnToastBottom).setOnClickListener {
            VolineToast.warning(
                this,
                message = "This toast appears at the bottom",
                position = ToastPosition.BOTTOM
            )
        }
    }

    private fun setupToastDurations() {
        // Short duration
        findViewById<Button>(R.id.btnToastShort).setOnClickListener {
            VolineToast.show(
                this,
                message = "This toast disappears quickly (2s)",
                duration = ToastDuration.SHORT
            )
        }

        // Medium duration (default)
        findViewById<Button>(R.id.btnToastMedium).setOnClickListener {
            VolineToast.show(
                this,
                message = "This toast has medium duration (3.5s)",
                duration = ToastDuration.MEDIUM
            )
        }

        // Long duration
        findViewById<Button>(R.id.btnToastLong).setOnClickListener {
            VolineToast.show(
                this,
                message = "This toast stays longer (5s)",
                duration = ToastDuration.LONG
            )
        }
    }

    private fun setupCustomToasts() {
        // Custom icon toast
        findViewById<Button>(R.id.btnCustomIconToast).setOnClickListener {
            VolineToast.show(
                this,
                message = "Toast with custom icon",
                icon = R.drawable.ic_launcher_foreground
            )
        }

        // Dismiss current toast
        findViewById<Button>(R.id.btnDismissToast).setOnClickListener {
            VolineToast.dismiss()
        }

        // Multiple toasts (only last one shows)
        findViewById<Button>(R.id.btnMultipleToasts).setOnClickListener {
            VolineToast.success(this, "First toast")
            // After a delay, show another
            window.decorView.postDelayed({
                VolineToast.error(this, "Second toast replaces first")
            }, 1000)
        }
    }
}
