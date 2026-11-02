@file:Suppress("unused")

package `in`.fivedegree.volineui.photocapturemanager

/**
 * OcrField - One piece of data a host wants collected from a document photo.
 *
 * The host passes a list of these in [PhotoCaptureConfig.ocrFields]. After
 * the user confirms the photo, the camera shows an OCR step where the user
 * picks a field and then taps the words on the photo that make up that
 * field's value. Nothing is typed by hand: a field's value is always built
 * from words the recognizer actually found on the photo.
 *
 * @property key Stable identifier the host uses to read the value back
 *   from [PhotoCaptureResult.Success.ocrData]. Keys are host-defined and
 *   are never shown to the user, so they can stay stable even if the
 *   label text changes.
 * @property label Text shown to the user in the OCR step, e.g. "Person Name".
 * @property required If true, the user cannot finish the OCR step until
 *   this field has at least one selected word. Optional fields may be
 *   left empty and are then simply absent from [PhotoCaptureResult.Success.ocrData].
 */
data class OcrField(
    val key: String,
    val label: String,
    val required: Boolean = false
)
