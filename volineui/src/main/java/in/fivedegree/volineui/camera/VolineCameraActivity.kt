@file:Suppress("unused")

package `in`.fivedegree.volineui.camera

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import `in`.fivedegree.volineui.photocapturemanager.CaptureGuide
import `in`.fivedegree.volineui.photocapturemanager.OcrField
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
 * OCR mode: when the caller also passes OCR fields ([EXTRA_OCR_KEYS] /
 * [EXTRA_OCR_LABELS] / [EXTRA_OCR_REQUIRED]), confirming a photo moves
 * to an OCR step instead of finishing, and Done finishes with
 * [android.app.Activity.RESULT_OK] plus the path and the collected
 * values ([EXTRA_OCR_DATA_KEYS] / [EXTRA_OCR_DATA_VALUES]).
 *
 * OCR-only mode: when the caller passes [EXTRA_INPUT_PATH] instead of
 * an output path (a gallery photo), the capture UI is skipped and the
 * screen starts at the OCR step for that file. OCR fields are required
 * in this mode: without fields there is nothing to collect, so a
 * missing or malformed field set finishes with [RESULT_ERROR].
 *
 * Confirm mode: when the caller passes [EXTRA_CONFIRM_PATH] (an
 * already-existing photo, e.g. a document-scanner page), the screen
 * opens directly on the review step for that file: no capture UI,
 * no camera binding. Retake here does not recapture -- it finishes
 * with [RESULT_RETAKE] so the caller can produce a fresh photo
 * (for documents, by relaunching the scanner). Use/Continue behave
 * as in camera mode, including the OCR step when fields were given.
 *
 * Guided capture: [EXTRA_CAPTURE_GUIDE] carries the host's capture
 * guide. PERSON enables the live person guide in the camera screen;
 * it is ignored in confirm mode, where there is no live preview.
 *
 * Camera options: the caller passes the host's aspect choice via
 * [EXTRA_ASPECT_RATIO] (a PhotoCaptureConfig AspectRatio name) and
 * front-camera permission via [EXTRA_ALLOW_FRONT_CAMERA]. A locked
 * ratio fixes the camera and hides the aspect pill; NONE leaves the
 * pill up for session-only switching. An absent or unknown ratio
 * extra locks to 4:3, matching the config's default, so an older
 * caller that sends no extra still gets the default behaviour.
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
        val inputPath = intent.getStringExtra(EXTRA_INPUT_PATH)
        val confirmPath = intent.getStringExtra(EXTRA_CONFIRM_PATH)
        val (ocrFields, ocrFieldsMalformed) = readOcrFields(intent)
        val fixedAspect = readFixedAspect(intent)
        val allowFrontCamera = intent.getBooleanExtra(EXTRA_ALLOW_FRONT_CAMERA, false)
        val captureGuide = readCaptureGuide(intent)
        if (confirmPath != null) {
            // Confirm mode (e.g. a document-scanner page): no camera
            // is involved, so no output path is required. Malformed
            // OCR fields are still an error, since the OCR step
            // cannot run reliably on them.
            if (ocrFieldsMalformed) {
                Log.w(TAG, "VolineCamera finishing with RESULT_ERROR: invalid OCR fields for confirm photo")
                setResult(
                    RESULT_ERROR,
                    Intent().putExtra(EXTRA_ERROR_MESSAGE, "OCR fields were invalid.")
                )
                finish()
                return
            }
        } else if (inputPath != null) {
            // OCR-only mode (gallery photo): fields are mandatory here.
            if (ocrFieldsMalformed || ocrFields.isEmpty()) {
                Log.w(TAG, "VolineCamera finishing with RESULT_ERROR: invalid OCR fields for input photo")
                setResult(
                    RESULT_ERROR,
                    Intent().putExtra(
                        EXTRA_ERROR_MESSAGE,
                        "OCR fields were not provided for the picked photo."
                    )
                )
                finish()
                return
            }
        } else if (outputPath.isNullOrBlank()) {
            Log.w(TAG, "VolineCamera finishing with RESULT_ERROR: no output path in intent")
            setResult(
                RESULT_ERROR,
                Intent().putExtra(EXTRA_ERROR_MESSAGE, "Camera output file was not provided.")
            )
            finish()
            return
        } else if (ocrFieldsMalformed) {
            Log.w(TAG, "VolineCamera finishing with RESULT_ERROR: malformed OCR fields")
            setResult(
                RESULT_ERROR,
                Intent().putExtra(EXTRA_ERROR_MESSAGE, "OCR fields were invalid.")
            )
            finish()
            return
        }

        // In OCR-only mode the "output" file is the picked photo itself:
        // the capture UI never runs, so nothing is ever written to it,
        // and the OCR step returns it unchanged. Confirm mode is the
        // same shape: the file is the photo to confirm, not a file
        // to capture into.
        val outputFile = File(confirmPath ?: inputPath ?: outputPath!!)
        val ocrInputFile = inputPath?.let { File(it) }
        val reviewFile = confirmPath?.let { File(it) }

        setContent {
            MaterialTheme(colorScheme = hostColorScheme()) {
                CameraScreen(
                    outputFile = outputFile,
                    onUsePhoto = { file ->
                        Log.d(TAG, "VolineCamera finishing with RESULT_OK (Use photo)")
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(EXTRA_OUTPUT_PATH, file.absolutePath)
                        )
                        finish()
                    },
                    ocrFields = ocrFields,
                    inputFile = ocrInputFile,
                    fixedAspect = fixedAspect,
                    allowFrontCamera = allowFrontCamera,
                    captureGuide = captureGuide,
                    reviewFile = reviewFile,
                    onRetake = {
                        Log.d(TAG, "VolineCamera finishing with RESULT_RETAKE (confirm retake)")
                        setResult(RESULT_RETAKE)
                        finish()
                    },
                    onOcrDone = { file, ocrData ->
                        Log.d(TAG, "VolineCamera finishing with RESULT_OK (OCR Done, ${ocrData.size} fields)")
                        setResult(
                            RESULT_OK,
                            Intent()
                                .putExtra(EXTRA_OUTPUT_PATH, file.absolutePath)
                                .putExtra(EXTRA_OCR_DATA_KEYS, ocrData.keys.toTypedArray())
                                .putExtra(EXTRA_OCR_DATA_VALUES, ocrData.values.toTypedArray())
                        )
                        finish()
                    },
                    onClose = {
                        Log.d(TAG, "VolineCamera finishing with RESULT_CANCELED (close/back)")
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onError = { message ->
                        Log.w(TAG, "VolineCamera finishing with RESULT_ERROR: $message")
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
     * Translate the host's capture guide into the screen's guide.
     * Absent or unknown values mean no guide, the config's default.
     */
    private fun readCaptureGuide(intent: Intent): CaptureGuide {
        return when (intent.getStringExtra(EXTRA_CAPTURE_GUIDE)) {
            "PERSON" -> CaptureGuide.PERSON
            "DOCUMENT" -> CaptureGuide.DOCUMENT
            else -> CaptureGuide.NONE
        }
    }

    /**
     * Translate the host's aspect choice into the ratio the camera
     * locks to, or null for NONE (user switching). Absent or unknown
     * values lock to 4:3, the config's default.
     */
    private fun readFixedAspect(intent: Intent): CameraAspect? {
        return when (intent.getStringExtra(EXTRA_ASPECT_RATIO)) {
            "RATIO_16_9" -> CameraAspect.SIXTEEN_NINE
            "NONE" -> null
            else -> CameraAspect.FOUR_THREE
        }
    }

    /**
     * Read the host's OCR fields from the intent's parallel arrays.
     * Returns the fields plus a malformed flag: the arrays must all be
     * present and the same length, or the set is unusable. No arrays at
     * all means "no OCR", which is only valid for the camera path.
     */
    private fun readOcrFields(intent: Intent): Pair<List<OcrField>, Boolean> {
        val keys = intent.getStringArrayExtra(EXTRA_OCR_KEYS) ?: return emptyList<OcrField>() to false
        val labels = intent.getStringArrayExtra(EXTRA_OCR_LABELS)
        val required = intent.getBooleanArrayExtra(EXTRA_OCR_REQUIRED)
        if (labels == null || required == null ||
            labels.size != keys.size || required.size != keys.size
        ) {
            return emptyList<OcrField>() to true
        }
        return keys.mapIndexed { index, key ->
            OcrField(key = key, label = labels[index], required = required[index])
        } to false
    }

    override fun onDestroy() {
        // Distinguishes a deliberate finish() (isFinishing=true) from a
        // system destroy/recreation (isFinishing=false) in logcat, which
        // was the missing piece when diagnosing an instant close.
        Log.d(TAG, "VolineCamera onDestroy isFinishing=$isFinishing")
        super.onDestroy()
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
        private const val TAG = "VolineCamera"

        /** Absolute path of the file the captured photo must be written to. */
        const val EXTRA_OUTPUT_PATH = "voline_camera_output_path"

        /** Absolute path of an existing photo to run the OCR step on (gallery mode). */
        const val EXTRA_INPUT_PATH = "voline_camera_input_path"

        /** Absolute path of an existing photo to confirm (confirm mode). */
        const val EXTRA_CONFIRM_PATH = "voline_camera_confirm_path"

        /** Host capture guide: a CaptureGuide name. */
        const val EXTRA_CAPTURE_GUIDE = "voline_camera_capture_guide"

        /** Host aspect choice: a PhotoCaptureConfig AspectRatio name. */
        const val EXTRA_ASPECT_RATIO = "voline_camera_aspect_ratio"

        /** Whether the host allows switching to the front camera. */
        const val EXTRA_ALLOW_FRONT_CAMERA = "voline_camera_allow_front_camera"

        /** Human-readable error message, present only with [RESULT_ERROR]. */
        const val EXTRA_ERROR_MESSAGE = "voline_camera_error_message"

        /** OCR field keys, parallel to [EXTRA_OCR_LABELS] and [EXTRA_OCR_REQUIRED]. */
        const val EXTRA_OCR_KEYS = "voline_camera_ocr_keys"

        /** OCR field labels shown to the user, parallel to [EXTRA_OCR_KEYS]. */
        const val EXTRA_OCR_LABELS = "voline_camera_ocr_labels"

        /** OCR field required flags, parallel to [EXTRA_OCR_KEYS]. */
        const val EXTRA_OCR_REQUIRED = "voline_camera_ocr_required"

        /** Collected OCR values' keys on RESULT_OK, parallel to [EXTRA_OCR_DATA_VALUES]. */
        const val EXTRA_OCR_DATA_KEYS = "voline_camera_ocr_data_keys"

        /** Collected OCR values on RESULT_OK, parallel to [EXTRA_OCR_DATA_KEYS]. */
        const val EXTRA_OCR_DATA_VALUES = "voline_camera_ocr_data_values"

        /**
         * Custom result code for a camera failure. Deliberately distinct from
         * RESULT_OK / RESULT_CANCELED so the manager never mistakes a failure
         * for a cancellation, and never falls back to a system camera app.
         */
        const val RESULT_ERROR = RESULT_FIRST_USER + 1

        /**
         * Custom result code for "scan again" from confirm mode: the
         * user tapped Retake on a photo that came from elsewhere
         * (e.g. the document scanner), so the manager should produce
         * a fresh photo and show the confirm screen again, rather
         * than treating it as a cancellation.
         */
        const val RESULT_RETAKE = RESULT_FIRST_USER + 2
    }
}
