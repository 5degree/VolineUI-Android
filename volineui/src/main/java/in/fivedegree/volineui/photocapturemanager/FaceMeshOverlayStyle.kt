package `in`.fivedegree.volineui.photocapturemanager

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * How [in.fivedegree.volineui.PhotoCaptureManager.drawFaceMeshOverlay]
 * draws a mesh on a photo.
 *
 * The defaults reproduce the demo's overlay: a green wireframe, white
 * dots on the mesh points, and a green bounding box, all drawn in the
 * photo's own pixel space. A host that wants a different look changes
 * the colours, or turns a layer off entirely -- a host that only wants
 * the wireframe passes `drawDots = false, drawBox = false`, and a
 * host that only wants dots passes `drawWireframe = false`.
 *
 * Stroke widths and dot size are not configurable: they scale with
 * the photo's width inside the drawing function, so the mesh stays
 * visible at any resolution without the host doing the maths.
 */
data class FaceMeshOverlayStyle(
    /** Colour of the triangle edges (the wireframe). */
    @ColorInt val wireframeColor: Int = Color.GREEN,
    /** Colour of the dots drawn on the mesh points. */
    @ColorInt val dotColor: Int = Color.WHITE,
    /** Colour of the bounding-box rectangle. */
    @ColorInt val boxColor: Int = Color.GREEN,
    /** Draw the triangle edges. False leaves the dots and box. */
    val drawWireframe: Boolean = true,
    /** Draw a dot on every mesh point. False leaves the wireframe and box. */
    val drawDots: Boolean = true,
    /** Draw the bounding-box rectangle. False leaves the wireframe and dots. */
    val drawBox: Boolean = true
)
