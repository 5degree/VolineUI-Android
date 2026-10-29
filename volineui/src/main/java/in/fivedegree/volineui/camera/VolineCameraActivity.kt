@file:Suppress("unused")

package `in`.fivedegree.volineui.camera

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.io.File

/**
 * Library-owned in-app camera.
 *
 * Launched only by [in.fivedegree.volineui.PhotoCaptureManager]; it never
 * launches an external camera app, and there is deliberately no fallback to
 * one. On failure it finishes with [RESULT_ERROR] and an error message extra,
 * which the manager turns into a [in.fivedegree.volineui.photocapturemanager.PhotoCaptureResult.Error].
 *
 * The caller passes the file the photo must be written to via
 * [EXTRA_OUTPUT_PATH]. On "Use photo" the activity finishes with
 * [android.app.Activity.RESULT_OK] and the same path; on close / back it
 * finishes with [android.app.Activity.RESULT_CANCELED].
 *
 * Theme: no theme is set on this activity in the library manifest, so it
 * inherits the host application's theme. The Compose colour scheme below is
 * derived from the host theme's colour attributes where they resolve
 * (colorPrimary, colorOnPrimary, colorSurface, colorOnSurface, colorPrimaryContainer),
 * falling back to the default Material3 dark scheme for a camera surface.
 * That is how the camera controls pick up the host project's colours instead
 * of a hardcoded VolineUI palette, while the preview itself stays black.
 */
class VolineCameraActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
        if (outputPath.isNullOrBlank()) {
            setResult(
                RESULT_ERROR,
                Intent().putExtra(EXTRA_ERROR_MESSAGE, "Camera output file was not provided.")
            )
            finish()
            return
        }

        setContent {
            MaterialTheme(colorScheme = hostColorScheme()) {
                CameraScreen(
                    outputFile = File(outputPath),
                    onUsePhoto = { file ->
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(EXTRA_OUTPUT_PATH, file.absolutePath)
                        )
                        finish()
                    },
                    onClose = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onError = { message ->
                        setResult(
                            RESULT_ERROR,
                            Intent().putExtra(EXTRA_ERROR_MESSAGE, message)
                        )
                        finish()
                    }
                )
            }
        }
    }

    /**
     * Build a colour scheme from the host application's theme attributes.
     * The library depends on Material, so the Material colour attributes
     * resolve through the activity theme when the host uses a Material /
     * Material3 theme. Any attribute that does not resolve falls back to the
     * default dark camera scheme, so the preview and controls stay usable
     * even in a host with an unrelated theme.
     */
    @Composable
    private fun hostColorScheme(): androidx.compose.material3.ColorScheme {
        val context = LocalContext.current
        val fallback = darkColorScheme()
        return remember(context) {
            fun resolve(attr: Int): Color? {
                val typedValue = TypedValue()
                return if (context.theme.resolveAttribute(attr, typedValue, true) &&
                    typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                    typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT
                ) {
                    Color(typedValue.data)
                } else {
                    null
                }
            }
            fallback.copy(
                // colorPrimary is not defined by the Material library itself
                // (its R.txt has no such attr); it comes from the framework /
                // AppCompat theme, as the rest of this library already uses it.
                primary = resolve(android.R.attr.colorPrimary)
                    ?: fallback.primary,
                onPrimary = resolve(com.google.android.material.R.attr.colorOnPrimary)
                    ?: fallback.onPrimary,
                primaryContainer = resolve(com.google.android.material.R.attr.colorPrimaryContainer)
                    ?: fallback.primaryContainer,
                onPrimaryContainer = resolve(com.google.android.material.R.attr.colorOnPrimaryContainer)
                    ?: fallback.onPrimaryContainer,
                surface = resolve(com.google.android.material.R.attr.colorSurface)
                    ?: fallback.surface,
                onSurface = resolve(com.google.android.material.R.attr.colorOnSurface)
                    ?: fallback.onSurface
            )
        }
    }

    companion object {
        /** Absolute path of the file the captured photo must be written to. */
        const val EXTRA_OUTPUT_PATH = "voline_camera_output_path"

        /** Human-readable error message, present only with [RESULT_ERROR]. */
        const val EXTRA_ERROR_MESSAGE = "voline_camera_error_message"

        /**
         * Custom result code for a camera failure. Deliberately distinct from
         * RESULT_OK / RESULT_CANCELED so the manager never mistakes a failure
         * for a cancellation, and never falls back to a system camera app.
         */
        const val RESULT_ERROR = RESULT_FIRST_USER + 1
    }
}
