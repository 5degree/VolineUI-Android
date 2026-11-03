package `in`.fivedegree.volineui.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the person-guide gate math on synthetic masks. The
 * outline used here is the centred rect the overlay draws for a
 * square frame: 70% of the height, width from the 668/748 outline
 * aspect, which for a square frame is about 62.5% of the width.
 */
class PersonGuideGateTest {

    private val outline = personOutlineFractions(1f)

    private fun mask(width: Int, height: Int, fill: (x: Int, y: Int) -> Float): FloatArray {
        return FloatArray(width * height) { index ->
            fill(index % width, index / width)
        }
    }

    @Test
    fun emptyMaskIsNotAligned() {
        val result = evaluatePersonGate(10, 10, mask(10, 10) { _, _ -> 0f }, outline)
        assertFalse(result.aligned)
        assertEquals(0f, result.insideFraction, 0.001f)
        assertEquals(0f, result.fillFraction, 0.001f)
    }

    @Test
    fun personFullyInsideOutlineIsAligned() {
        // Foreground block covering the middle of the frame, well
        // inside the outline rect (left ~0.19, top 0.15, right ~0.81,
        // bottom 0.85 for a square frame).
        val result = evaluatePersonGate(20, 20, mask(20, 20) { x, y ->
            if (x in 6..13 && y in 5..16) 1f else 0f
        }, outline)
        assertTrue(result.aligned)
        assertEquals(1f, result.insideFraction, 0.001f)
    }

    @Test
    fun personMostlyOutsideOutlineIsNotAligned() {
        // Foreground block on the left edge, mostly outside.
        val result = evaluatePersonGate(20, 20, mask(20, 20) { x, y ->
            if (x in 0..5 && y in 5..16) 1f else 0f
        }, outline)
        assertFalse(result.aligned)
    }

    @Test
    fun tinyCentredPersonFailsFill() {
        // A tiny centred block is fully inside, but fills almost
        // none of the outline, so it must not enable the shutter.
        val result = evaluatePersonGate(20, 20, mask(20, 20) { x, y ->
            if (x in 9..10 && y in 9..10) 1f else 0f
        }, outline)
        assertEquals(1f, result.insideFraction, 0.001f)
        assertFalse(result.aligned)
    }

    @Test
    fun lowConfidencePixelsDoNotCount() {
        val result = evaluatePersonGate(20, 20, mask(20, 20) { x, y ->
            if (x in 6..13 && y in 5..16) 0.4f else 0f
        }, outline)
        assertFalse(result.aligned)
        assertEquals(0f, result.insideFraction, 0.001f)
    }

    @Test
    fun outlineFractionsFitNarrowFrames() {
        // A narrow portrait frame (9:16 display): the outline must
        // shrink to fit the width instead of overflowing it.
        val fractions = personOutlineFractions(9f / 16f)
        assertTrue(fractions.left >= 0.05f)
        assertTrue(fractions.right <= 0.95f)
        assertTrue(fractions.top >= 0f)
        assertTrue(fractions.bottom <= 1f)
    }
}
