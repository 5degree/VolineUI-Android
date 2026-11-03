package `in`.fivedegree.volineui.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmenter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * CameraX analyzer for the person capture guide.
 *
 * Runs the selfie segmenter in stream mode on preview frames and
 * reports whether the person is framed inside the outline. Frames
 * are throttled (at most one segmentation every 150 ms, and never
 * two in flight) because the mask, not the frame rate, is what the
 * gate needs -- and a frame is always closed exactly once, on the
 * completion path, so a slow model can never stall the camera.
 *
 * A frame that fails segmentation is ignored, not treated as
 * "not aligned": a transient model failure must not flicker the
 * shutter. The gate state simply keeps its last value until the
 * next successful frame.
 */
internal class PersonGuideAnalyzer(
    private val segmenter: Segmenter,
    private val isActive: () -> Boolean,
    private val onAligned: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val lastAnalyzedMs = AtomicLong(0)
    private val inFlight = AtomicBoolean(false)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val now = System.currentTimeMillis()
        if (!isActive() || mediaImage == null ||
            inFlight.get() || now - lastAnalyzedMs.get() < MIN_FRAME_INTERVAL_MS
        ) {
            imageProxy.close()
            return
        }
        inFlight.set(true)
        lastAnalyzedMs.set(now)
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        segmenter.process(inputImage)
            .addOnSuccessListener { mask ->
                try {
                    val width = mask.width
                    val height = mask.height
                    // The mask buffer is bytes holding floats, not a
                    // FloatBuffer: view it as floats before reading.
                    val floatBuffer = mask.buffer.asFloatBuffer()
                    val confidences = FloatArray(width * height)
                    floatBuffer.rewind()
                    floatBuffer.get(confidences)
                    // Mask aspect equals preview aspect (same image,
                    // rotation-corrected), so the outline fractions
                    // computed from the mask aspect are the same ones
                    // the overlay draws.
                    val outline = personOutlineFractions(width.toFloat() / height)
                    val result = evaluatePersonGate(width, height, confidences, outline)
                    onAligned(result.aligned)
                } catch (_: Exception) {
                    // A malformed mask must not break the camera.
                }
            }
            .addOnFailureListener {
                // Transient failure: keep the last gate value.
            }
            .addOnCompleteListener {
                inFlight.set(false)
                imageProxy.close()
            }
    }

    private companion object {
        const val MIN_FRAME_INTERVAL_MS = 150L
    }
}
