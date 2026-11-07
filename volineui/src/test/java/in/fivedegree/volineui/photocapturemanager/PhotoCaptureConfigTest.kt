package `in`.fivedegree.volineui.photocapturemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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

    @Test
    fun captureGuideDefaultsToNone() {
        assertEquals(CaptureGuide.NONE, PhotoCaptureConfig().captureGuide)
    }

    @Test
    fun personGuideIsPreserved() {
        val config = PhotoCaptureConfig(captureGuide = CaptureGuide.PERSON)
        assertEquals(CaptureGuide.PERSON, config.captureGuide)
    }

    @Test
    fun documentGuideIsPreserved() {
        val config = PhotoCaptureConfig(captureGuide = CaptureGuide.DOCUMENT)
        assertEquals(CaptureGuide.DOCUMENT, config.captureGuide)
    }

    // A ~100 m square around a fixed point, the same ring the
    // GeofenceTest uses.
    private val squareArea = listOf(
        GeoPoint(19.07555, 72.87725),
        GeoPoint(19.07555, 72.87815),
        GeoPoint(19.07645, 72.87815),
        GeoPoint(19.07645, 72.87725)
    )

    @Test
    fun geofenceDefaultsToOff() {
        val config = PhotoCaptureConfig()
        assertTrue(config.allowedCaptureArea.isEmpty())
        assertEquals(
            PhotoCaptureConfig.GeofenceLocationMode.FRESH_WITH_CACHE_FALLBACK,
            config.geofenceLocationMode
        )
        assertEquals(30_000L, config.geofenceLocationTimeoutMs)
        assertEquals(10f, config.geofenceMaxAccuracyM, 0f)
    }

    @Test
    fun validGeofencePolygonIsPreserved() {
        val config = PhotoCaptureConfig(
            allowedCaptureArea = squareArea,
            geofenceLocationMode = PhotoCaptureConfig.GeofenceLocationMode.FRESH_ONLY,
            geofenceLocationTimeoutMs = 10_000,
            geofenceMaxAccuracyM = 5f
        )
        assertEquals(squareArea, config.allowedCaptureArea)
        assertEquals(
            PhotoCaptureConfig.GeofenceLocationMode.FRESH_ONLY,
            config.geofenceLocationMode
        )
        assertEquals(10_000L, config.geofenceLocationTimeoutMs)
        assertEquals(5f, config.geofenceMaxAccuracyM, 0f)
    }

    @Test
    fun geofenceWithFewerThanThreePointsIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoCaptureConfig(allowedCaptureArea = squareArea.take(2))
        }
    }

    @Test
    fun geofenceLatitudeOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoCaptureConfig(
                allowedCaptureArea = listOf(
                    GeoPoint(91.0, 72.87725),
                    GeoPoint(19.07555, 72.87815),
                    GeoPoint(19.07645, 72.87770)
                )
            )
        }
    }

    @Test
    fun geofenceLongitudeOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoCaptureConfig(
                allowedCaptureArea = listOf(
                    GeoPoint(19.07555, 181.0),
                    GeoPoint(19.07555, 72.87815),
                    GeoPoint(19.07645, 72.87770)
                )
            )
        }
    }

    @Test
    fun degenerateGeofencePolygonIsRejected() {
        // Three collinear points enclose nothing.
        assertThrows(IllegalArgumentException::class.java) {
            PhotoCaptureConfig(
                allowedCaptureArea = listOf(
                    GeoPoint(19.07555, 72.87725),
                    GeoPoint(19.07555, 72.87770),
                    GeoPoint(19.07555, 72.87815)
                )
            )
        }
        // Three identical points enclose nothing either.
        assertThrows(IllegalArgumentException::class.java) {
            PhotoCaptureConfig(
                allowedCaptureArea = listOf(
                    GeoPoint(19.0760, 72.8777),
                    GeoPoint(19.0760, 72.8777),
                    GeoPoint(19.0760, 72.8777)
                )
            )
        }
    }

    @Test
    fun geofenceTimeoutMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoCaptureConfig(
                allowedCaptureArea = squareArea,
                geofenceLocationTimeoutMs = 0
            )
        }
    }

    @Test
    fun geofenceAccuracyMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoCaptureConfig(
                allowedCaptureArea = squareArea,
                geofenceMaxAccuracyM = 0f
            )
        }
    }
}
