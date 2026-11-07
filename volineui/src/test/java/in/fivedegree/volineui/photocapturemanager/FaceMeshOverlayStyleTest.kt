package `in`.fivedegree.volineui.photocapturemanager

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the mesh-overlay style contract: the defaults reproduce the
 * demo's overlay (green wireframe, white dots, green box, every
 * layer on), and a host that customises it gets exactly what it
 * asked for. The Canvas drawing itself is not unit-testable here
 * (Bitmap is stubbed on the JVM); it is covered by the demo, which
 * draws through the manager on device.
 */
class FaceMeshOverlayStyleTest {

    @Test
    fun defaultsMatchTheDemoOverlay() {
        val style = FaceMeshOverlayStyle()
        assertEquals(Color.GREEN, style.wireframeColor)
        assertEquals(Color.WHITE, style.dotColor)
        assertEquals(Color.GREEN, style.boxColor)
        assertTrue(style.drawWireframe)
        assertTrue(style.drawDots)
        assertTrue(style.drawBox)
    }

    @Test
    fun hostChoicesArePreserved() {
        val style = FaceMeshOverlayStyle(
            wireframeColor = Color.RED,
            dotColor = Color.BLUE,
            boxColor = Color.YELLOW,
            drawWireframe = true,
            drawDots = false,
            drawBox = false
        )
        assertEquals(Color.RED, style.wireframeColor)
        assertEquals(Color.BLUE, style.dotColor)
        assertEquals(Color.YELLOW, style.boxColor)
        assertTrue(style.drawWireframe)
        assertFalse(style.drawDots)
        assertFalse(style.drawBox)
    }
}
