package `in`.fivedegree.volineui.camera

/**
 * The person-guide gate, in pure Kotlin (no Android types) so it is
 * JVM-testable.
 *
 * The overlay draws the person silhouette (the host-supplied outline,
 * viewBox 668x748) centred in the preview, at 70% of the preview
 * height, clamped so it also fits the preview width with a 5% margin
 * on each side. The same geometry, expressed as fractions of the
 * frame, is what the live segmentation mask is tested against: mask
 * aspect equals preview aspect (same image, rotation-corrected), so
 * fractions transfer between the two without any pixel mapping.
 *
 * A frame counts as aligned when most of the detected person lies
 * inside the outline AND the outline is substantially filled. Both
 * conditions matter: the first alone would pass a tiny distant
 * person dead-centre, the second alone would pass a person
 * overflowing the frame. The thresholds are starting values for
 * device tuning, not tuned constants -- if testing shows they are
 * too strict or too loose, they change here, in one place.
 */
data class OutlineFractions(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class PersonGateResult(
    /** Whether the shutter may be enabled for this frame. */
    val aligned: Boolean,
    /** Foreground pixels inside the outline / all foreground pixels. */
    val insideFraction: Float,
    /** Foreground pixels inside the outline / outline pixel count. */
    val fillFraction: Float
)

/** Aspect of the person outline path (width / height). */
internal const val PERSON_OUTLINE_ASPECT = 668f / 748f

/**
 * Outline rect as fractions of the frame, for a frame of the given
 * aspect (width / height). Pure function of the aspect, so the
 * overlay drawing and the mask gate always agree.
 */
internal fun personOutlineFractions(frameAspect: Float): OutlineFractions {
    require(frameAspect > 0f) { "frameAspect must be positive" }
    // Height fraction: 70% of the frame height, unless the outline
    // would then overflow the frame width (narrow portrait frames),
    // in which case shrink to fit 90% of the width instead.
    val heightFraction = minOf(0.70f, 0.90f * frameAspect / PERSON_OUTLINE_ASPECT)
    val widthFraction = heightFraction * PERSON_OUTLINE_ASPECT / frameAspect
    val left = (1f - widthFraction) / 2f
    val top = (1f - heightFraction) / 2f
    return OutlineFractions(
        left = left,
        top = top,
        right = 1f - left,
        bottom = 1f - top
    )
}

/**
 * Test one segmentation mask against the outline.
 *
 * @param maskWidth mask width in pixels; @param maskHeight likewise.
 * @param confidences row-major foreground confidences, maskWidth *
 *   maskHeight values in [0, 1]. A pixel counts as person at or
 *   above [foregroundThreshold].
 * @param outline the outline rect as fractions of the frame.
 */
internal fun evaluatePersonGate(
    maskWidth: Int,
    maskHeight: Int,
    confidences: FloatArray,
    outline: OutlineFractions,
    foregroundThreshold: Float = 0.5f,
    insideThreshold: Float = 0.80f,
    fillThreshold: Float = 0.35f
): PersonGateResult {
    require(maskWidth > 0 && maskHeight > 0) { "mask dimensions must be positive" }
    require(confidences.size == maskWidth * maskHeight) {
        "confidences must hold exactly maskWidth * maskHeight values"
    }
    val leftPx = (outline.left * maskWidth).toInt().coerceIn(0, maskWidth)
    val topPx = (outline.top * maskHeight).toInt().coerceIn(0, maskHeight)
    val rightPx = (outline.right * maskWidth).toInt().coerceIn(0, maskWidth)
    val bottomPx = (outline.bottom * maskHeight).toInt().coerceIn(0, maskHeight)
    val outlinePixels = (rightPx - leftPx) * (bottomPx - topPx)
    var foregroundTotal = 0
    var foregroundInside = 0
    for (y in 0 until maskHeight) {
        for (x in 0 until maskWidth) {
            if (confidences[y * maskWidth + x] >= foregroundThreshold) {
                foregroundTotal++
                if (x in leftPx until rightPx && y in topPx until bottomPx) {
                    foregroundInside++
                }
            }
        }
    }
    if (foregroundTotal == 0 || outlinePixels <= 0) {
        return PersonGateResult(
            aligned = false,
            insideFraction = 0f,
            fillFraction = 0f
        )
    }
    val insideFraction = foregroundInside.toFloat() / foregroundTotal
    val fillFraction = foregroundInside.toFloat() / outlinePixels
    return PersonGateResult(
        aligned = insideFraction >= insideThreshold && fillFraction >= fillThreshold,
        insideFraction = insideFraction,
        fillFraction = fillFraction
    )
}
