package `in`.fivedegree.volineui.photocapturemanager

/**
 * What kind of guided capture the host wants for this photo.
 *
 * NONE is today's behaviour: no overlay, no gating, no extra result.
 * PERSON shows a person-shaped outline over the live preview and only
 * enables the shutter while a person is framed inside it (live selfie
 * segmentation), then runs face-mesh detection on the finished photo
 * and returns it in [PhotoCaptureResult.Success.faceMesh].
 * DOCUMENT does not use this camera screen at all: the manager
 * launches Google's document-scanner UI instead, and the cleaned page
 * it returns comes back into the confirmation screen (and then the
 * OCR step, if the host asked for fields).
 */
enum class CaptureGuide {
    NONE,
    PERSON,
    DOCUMENT
}
