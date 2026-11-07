package `in`.fivedegree.volineui.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import `in`.fivedegree.volineui.photocapturemanager.FaceMeshOverlayStyle
import `in`.fivedegree.volineui.photocapturemanager.FaceMeshResult

/**
 * Draws a face mesh over a copy of [photo] and returns it; the
 * original bitmap is left untouched.
 *
 * The mesh points are in the photo's own pixel space, so no
 * view-scale mapping is needed. Dot and stroke sizes scale with the
 * photo so the mesh stays visible at any resolution. A triangle
 * whose indices fall outside the points list is skipped rather than
 * drawn wrong -- it can only come from a corrupt payload. An empty
 * mesh draws nothing, and the returned copy is still a valid (if
 * unmarked) photo.
 *
 * Internal: hosts use
 * [in.fivedegree.volineui.PhotoCaptureManager.drawFaceMeshOverlay],
 * which delegates here.
 */
internal fun renderFaceMeshOverlay(
    photo: Bitmap,
    mesh: FaceMeshResult,
    style: FaceMeshOverlayStyle
): Bitmap {
    val overlay = photo.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(overlay)

    if (style.drawBox) {
        mesh.boundingBox?.let { box ->
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = style.boxColor
                this.style = Paint.Style.STROKE
                strokeWidth = maxOf(4f, photo.width / 300f)
            }
            canvas.drawRect(box.left, box.top, box.right, box.bottom, boxPaint)
        }
    }

    if (style.drawWireframe) {
        val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.wireframeColor
            this.style = Paint.Style.STROKE
            strokeWidth = maxOf(2f, photo.width / 600f)
        }
        for (triangle in mesh.triangles) {
            val a = mesh.points.getOrNull(triangle.first) ?: continue
            val b = mesh.points.getOrNull(triangle.second) ?: continue
            val c = mesh.points.getOrNull(triangle.third) ?: continue
            canvas.drawLine(a.x, a.y, b.x, b.y, wirePaint)
            canvas.drawLine(b.x, b.y, c.x, c.y, wirePaint)
            canvas.drawLine(c.x, c.y, a.x, a.y, wirePaint)
        }
    }

    if (style.drawDots) {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.dotColor
            this.style = Paint.Style.FILL
        }
        val dotRadius = maxOf(3f, photo.width / 400f)
        for (point in mesh.points) {
            canvas.drawCircle(point.x, point.y, dotRadius, dotPaint)
        }
    }

    return overlay
}
