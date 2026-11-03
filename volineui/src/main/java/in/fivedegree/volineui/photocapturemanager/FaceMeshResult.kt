package `in`.fivedegree.volineui.photocapturemanager

/**
 * One face-mesh point in the delivered photo's pixel space: x is the
 * horizontal pixel, y the vertical pixel, z the depth relative to the
 * face (same units as x/y in ML Kit's output). Pure data, no Android
 * types, so the host can send it to a server as JSON without any
 * conversion layer.
 */
data class FaceMeshPoint(
    val x: Float,
    val y: Float,
    val z: Float
)

/**
 * The face's bounding box in the delivered photo's pixel space.
 */
data class FaceMeshBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/**
 * The face mesh for a person capture, in the delivered photo's pixel
 * space (after EXIF rotation, watermarking, and compression, so the
 * points line up with the exact bytes the host received).
 *
 * An empty result -- no points and a null box -- means a mesh was
 * requested but no usable face was found in the finished photo. It
 * deliberately does not fail the capture: the photo itself is still a
 * valid person photo, and the host decides what an empty mesh means
 * for its server call. A null [PhotoCaptureResult.Success.faceMesh]
 * is different: it means no mesh was requested at all.
 */
data class FaceMeshResult(
    val points: List<FaceMeshPoint>,
    val boundingBox: FaceMeshBox?
) {
    companion object {
        /** The empty result: requested, but no face found. */
        val EMPTY = FaceMeshResult(points = emptyList(), boundingBox = null)
    }
}
