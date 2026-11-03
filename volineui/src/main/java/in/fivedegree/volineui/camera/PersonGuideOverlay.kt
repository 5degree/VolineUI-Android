package `in`.fivedegree.volineui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.graphics.PathParser

/**
 * The person capture guide overlay, drawn over the live preview.
 *
 * A black 0.4-alpha scrim covers the preview with the person
 * silhouette cut out of it (even-odd fill), and the silhouette
 * outline itself is stroked green while the person is framed inside
 * it, red while they are not -- the same green/red language as the
 * design. The silhouette is the host-supplied outline path
 * (viewBox 668x748), scaled to the same fractions
 * [personOutlineFractions] uses for the shutter gate, so what the
 * user sees and what the gate tests are the same shape.
 *
 * The path is closed, so its interior is the person shape even
 * though the source SVG only strokes it.
 */
@Composable
internal fun PersonGuideOverlay(
    aligned: Boolean,
    modifier: Modifier = Modifier
) {
    val basePath = remember {
        PathParser.createPathFromPathData(PERSON_OUTLINE_PATH_DATA).asComposePath()
    }
    Canvas(modifier = modifier) {
        val fractions = personOutlineFractions(size.width / size.height)
        val left = fractions.left * size.width
        val top = fractions.top * size.height
        val outlineWidth = (fractions.right - fractions.left) * size.width
        val scale = outlineWidth / 668f
        // Scale first, then translate: the matrix post-multiplies,
        // so translate-then-scale composes to T * S, which applies
        // the scale to the path before moving it into place.
        val drawn = Path().apply {
            addPath(basePath)
            transform(
                Matrix().apply {
                    translate(left, top)
                    scale(scale, scale)
                }
            )
        }
        // Scrim everywhere except inside the silhouette.
        val scrim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addPath(drawn)
        }
        drawPath(scrim, color = Color.Black.copy(alpha = 0.4f))
        // Outline: green = framed, red = not framed. The source SVG
        // strokes in red with width 5 in viewBox units; the width
        // scales with the outline.
        drawPath(
            path = drawn,
            color = if (aligned) Color(0xFF00A651) else Color.Red,
            style = Stroke(
                width = 5f * scale,
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            )
        )
    }
}

/** Host-supplied person silhouette, exactly as in ic_person_outline.svg. */
private const val PERSON_OUTLINE_PATH_DATA =
    "M399.992 34.5764C386.884 8.38447 343.816 0.901061 287.641 2.77191C212.74 6.51362 165.927 32.7055 171.544 79.4768L188.397 201.082C158.437 210.436 156.564 285.27 195.887 303.979C207.122 360.105 229.593 390.038 259.553 410.618L246.445 440.551C143.457 464.872 64.8108 491.064 38.5955 556.544L3.01758 693.116C223.975 762.961 444.308 762.961 664.018 693.116L628.44 556.544C602.224 491.064 523.578 464.872 420.59 440.551L407.482 410.618C437.443 390.038 459.913 360.105 471.148 303.979C510.471 285.27 508.598 210.436 478.638 201.082L480.511 107.54C480.511 57.0266 450.55 34.5764 399.992 34.5764Z"
