package `in`.fivedegree.volineui.camera

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import `in`.fivedegree.volineui.photocapturemanager.FaceMeshBox
import `in`.fivedegree.volineui.photocapturemanager.FaceMeshPoint
import `in`.fivedegree.volineui.photocapturemanager.FaceMeshResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Runs face-mesh detection on a finished photo and returns the mesh
 * in that photo's pixel space.
 *
 * Called on the processing thread, after the pipeline (EXIF rotation,
 * watermark, compression), so the points line up with the exact
 * delivered bytes -- which is what the host sends to its server.
 * The largest face wins when several are found, since a person
 * capture frames one main subject.
 *
 * A mesh failure never throws: detection errors, a timeout, or no
 * face at all all yield [FaceMeshResult.EMPTY], because the photo
 * itself is still valid and the host -- not the library -- decides
 * what an empty mesh means. The detector is created per call and
 * closed in finally, mirroring the OCR recognizer's lifecycle.
 */
internal fun detectFaceMesh(bitmap: Bitmap): FaceMeshResult {
    val detector = try {
        FaceMeshDetection.getClient(
            FaceMeshDetectorOptions.Builder()
                .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                .build()
        )
    } catch (e: Exception) {
        Log.w("VolineCamera", "Face mesh detector unavailable", e)
        return FaceMeshResult.EMPTY
    }
    try {
        val latch = CountDownLatch(1)
        var result: FaceMeshResult = FaceMeshResult.EMPTY
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { meshes ->
                val mesh = meshes.maxByOrNull { faceMesh ->
                    val box = faceMesh.boundingBox
                    box.width() * box.height()
                }
                result = if (mesh == null) {
                    FaceMeshResult.EMPTY
                } else {
                    val box = mesh.boundingBox
                    FaceMeshResult(
                        points = mesh.allPoints.map { point ->
                            val position = point.position
                            FaceMeshPoint(
                                x = position.x,
                                y = position.y,
                                z = position.z
                            )
                        },
                        boundingBox = FaceMeshBox(
                            left = box.left.toFloat(),
                            top = box.top.toFloat(),
                            right = box.right.toFloat(),
                            bottom = box.bottom.toFloat()
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.w("VolineCamera", "Face mesh detection failed", e)
            }
            .addOnCompleteListener {
                latch.countDown()
            }
        if (!latch.await(MESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            Log.w("VolineCamera", "Face mesh detection timed out")
        }
        return result
    } catch (e: Exception) {
        Log.w("VolineCamera", "Face mesh detection failed", e)
        return FaceMeshResult.EMPTY
    } finally {
        try {
            detector.close()
        } catch (_: Exception) {
            // Closing must never crash processing.
        }
    }
}

private const val MESH_TIMEOUT_SECONDS = 10L
