package `in`.fivedegree.volineui.photocapturemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the host-driven camera options' contract: a host that passes
 * nothing gets a 4:3-locked capture with no front camera, and a host
 * that opts into free aspect switching or the front camera gets
 * exactly what it asked for.
 */
class PhotoCaptureConfigTest {

    @Test
    fun defaultsAreLocked43WithFrontCameraOff() {
        val config = PhotoCaptureConfig()
        assertEquals(PhotoCaptureConfig.AspectRatio.RATIO_4_3, config.aspectRatio)
        assertFalse(config.allowFrontCamera)
    }

    @Test
    fun hostChoicesArePreserved() {
        val config = PhotoCaptureConfig(
            aspectRatio = PhotoCaptureConfig.AspectRatio.NONE,
            allowFrontCamera = true
        )
        assertEquals(PhotoCaptureConfig.AspectRatio.NONE, config.aspectRatio)
        assertTrue(config.allowFrontCamera)
    }

    @Test
    fun locked169IsPreserved() {
        val config = PhotoCaptureConfig(
            aspectRatio = PhotoCaptureConfig.AspectRatio.RATIO_16_9
        )
        assertEquals(PhotoCaptureConfig.AspectRatio.RATIO_16_9, config.aspectRatio)
        assertFalse(config.allowFrontCamera)
    }
}
