@file:Suppress("unused")

package `in`.fivedegree.volineui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Result of running OCR on one photo.
 *
 * @property words Merged recognised words, in recognizer (reading) order.
 * @property imageWidth Width of the image the word boxes are measured in.
 * @property imageHeight Height of the image the word boxes are measured in.
 *   The boxes are in the coordinates of the EXIF-rotated image -- the
 *   same orientation as the review bitmap -- so the selection screen
 *   only has to scale them, never rotate them.
 */
data class OcrRecognition(
    val words: List<OcrWord>,
    val imageWidth: Int,
    val imageHeight: Int
)

/**
 * Run on-device ML Kit text recognition on a captured photo.
 *
 * Both bundled recognizers run: Latin and Devanagari. A document that
 * mixes scripts (an Aadhaar card has both) is therefore covered either
 * way, and [mergeOcrElements] collapses the duplicate readings of the
 * same printed word. Recognition runs on an EXIF-rotated decode of the
 * file (max dimension 2048: full detail for small print, bounded memory),
 * never on the watermarked or compressed output -- watermark text must
 * never pollute the recognised words, and the photo the host receives
 * is produced separately by the manager's pipeline.
 *
 * If one recognizer fails but the other succeeds, the successful run's
 * words are used: partial coverage still lets the user finish, and a
 * total failure is the only case that reaches the OCR failure screen.
 * Recognizers are closed and the decode bitmap recycled on every path.
 */
suspend fun recognizeOcrWords(context: Context, file: File): OcrRecognition {
    val bitmap = decodeOcrBitmap(file)
        ?: throw IOException("Could not decode photo for text recognition")
    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val devanagariRecognizer = TextRecognition.getClient(
        DevanagariTextRecognizerOptions.Builder().build()
    )
    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        // runCatching per recognizer keeps success-with-no-words (a
        // document in the other script) distinct from a recognizer
        // failure: only a failure is a failure, an empty word list is
        // a valid result that the UI reports as "no text found".
        val (latinResult, devanagariResult) = coroutineScope {
            val latin = async { runCatching { runRecognizer(latinRecognizer, image, OcrScript.LATIN) } }
            val devanagari = async {
                runCatching { runRecognizer(devanagariRecognizer, image, OcrScript.DEVANAGARI) }
            }
            latin.await() to devanagari.await()
        }
        if (latinResult.isFailure && devanagariResult.isFailure) {
            throw latinResult.exceptionOrNull()
                ?: IOException("Text recognition failed")
        }
        val latinWords = latinResult.getOrElse { emptyList() }
        val devanagariWords = devanagariResult.getOrElse { emptyList() }
        return OcrRecognition(
            words = mergeOcrElements(latinWords, devanagariWords),
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
    } finally {
        try {
            latinRecognizer.close()
        } catch (_: Exception) {
            // Closing must never mask the recognition result.
        }
        try {
            devanagariRecognizer.close()
        } catch (_: Exception) {
            // Closing must never mask the recognition result.
        }
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

private suspend fun runRecognizer(
    recognizer: TextRecognizer,
    image: InputImage,
    script: OcrScript
): List<RawOcrElement> {
    val text = recognizer.processSuspend(image)
    return text.textBlocks
        .flatMap { block -> block.lines }
        .flatMap { line -> line.elements }
        .mapNotNull { element ->
            val box = element.boundingBox ?: return@mapNotNull null
            RawOcrElement(
                text = element.text,
                box = OcrBox(
                    left = box.left.toFloat(),
                    top = box.top.toFloat(),
                    right = box.right.toFloat(),
                    bottom = box.bottom.toFloat()
                ),
                script = script
            )
        }
}

private suspend fun TextRecognizer.processSuspend(image: InputImage): Text =
    suspendCancellableCoroutine { continuation ->
        val task = process(image)
        task.addOnSuccessListener { result -> continuation.resume(result) }
        task.addOnFailureListener { error -> continuation.resumeWithException(error) }
        // ML Kit Tasks cannot be cancelled; if the caller is cancelled,
        // the recognizers are still closed by recognizeOcrWords' finally.
    }

/**
 * Decode a photo for OCR: downscaled to a 2048 max dimension and rotated
 * by its EXIF orientation, mirroring decodeReviewBitmap's rotation rules
 * (90/180/270 only, never a flip) so OCR boxes and the review image
 * agree on orientation. 2048 keeps small document print legible, unlike
 * the review bitmap's 1080, while bounding the decode to roughly 22 MB.
 */
private fun decodeOcrBitmap(file: File): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        val maxDimension = 2048
        while (bounds.outWidth / sampleSize > maxDimension ||
            bounds.outHeight / sampleSize > maxDimension
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        val degrees = when (ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        rotated
    } catch (_: Exception) {
        null
    }
}
