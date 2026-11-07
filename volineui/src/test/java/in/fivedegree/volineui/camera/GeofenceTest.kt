package `in`.fivedegree.volineui.camera

import `in`.fivedegree.volineui.locationmanager.LocationResult
import `in`.fivedegree.volineui.photocapturemanager.GeoPoint
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the geofence geometry and evaluation contract.
 *
 * The polygon is a ~100 m square around a fixed point (sides of
 * 0.0009 degrees, about 50 m each way at this latitude -- the exact
 * metres do not matter, the tests only need a small ring whose
 * inside and outside are unambiguous). Ray casting is planar, so
 * these tests pin the planar behaviour: winding order must not
 * matter, boundary points count as inside, and a concave ring must
 * exclude its notch.
 */
class GeofenceTest {

    // A square: (19.0760, 72.8777) +/- 0.00045 degrees.
    private val square = listOf(
        GeoPoint(19.07555, 72.87725),
        GeoPoint(19.07555, 72.87815),
        GeoPoint(19.07645, 72.87815),
        GeoPoint(19.07645, 72.87725)
    )
    private val centre = GeoPoint(19.0760, 72.8777)

    private fun config(
        polygon: List<GeoPoint> = square,
        mode: PhotoCaptureConfig.GeofenceLocationMode =
            PhotoCaptureConfig.GeofenceLocationMode.FRESH_WITH_CACHE_FALLBACK,
        maxAccuracyM: Float = 10f
    ) = GeofenceConfig(
        polygon = polygon,
        mode = mode,
        timeoutMs = 30_000,
        maxAccuracyM = maxAccuracyM
    )

    private fun location(
        latitude: Double,
        longitude: Double,
        accuracy: Float? = 5f,
        isFromCache: Boolean = false
    ) = LocationResult(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        isFromCache = isFromCache
    )

    @Test
    fun centreIsInside() {
        assertTrue(isInsidePolygon(centre, square))
    }

    @Test
    fun farAwayIsOutside() {
        assertFalse(isInsidePolygon(GeoPoint(19.0800, 72.8777), square))
    }

    @Test
    fun pointOnEdgeCountsAsInside() {
        // Midpoint of the square's south edge.
        assertTrue(isInsidePolygon(GeoPoint(19.07555, 72.8777), square))
    }

    @Test
    fun pointOnVertexCountsAsInside() {
        assertTrue(isInsidePolygon(square.first(), square))
    }

    @Test
    fun windingOrderDoesNotMatter() {
        val reversed = square.reversed()
        assertTrue(isInsidePolygon(centre, reversed))
        assertFalse(isInsidePolygon(GeoPoint(19.0800, 72.8777), reversed))
    }

    @Test
    fun concavePolygonExcludesItsNotch() {
        // An L-shape: the square with its north-east quarter cut out.
        val lShape = listOf(
            GeoPoint(19.07555, 72.87725),
            GeoPoint(19.07555, 72.87815),
            GeoPoint(19.07600, 72.87815),
            GeoPoint(19.07600, 72.87770),
            GeoPoint(19.07645, 72.87770),
            GeoPoint(19.07645, 72.87725)
        )
        // In the south leg: inside.
        assertTrue(isInsidePolygon(GeoPoint(19.07580, 72.87770), lShape))
        // In the cut-out north-east notch: outside.
        assertFalse(isInsidePolygon(GeoPoint(19.07620, 72.87790), lShape))
    }

    @Test
    fun fewerThanThreePointsIsNeverInside() {
        assertFalse(isInsidePolygon(centre, square.take(2)))
        assertFalse(isInsidePolygon(centre, emptyList()))
    }

    @Test
    fun triangleMinimumWorks() {
        val triangle = listOf(
            GeoPoint(19.07555, 72.87725),
            GeoPoint(19.07555, 72.87815),
            GeoPoint(19.07645, 72.87770)
        )
        assertTrue(isInsidePolygon(GeoPoint(19.07590, 72.87770), triangle))
        assertFalse(isInsidePolygon(GeoPoint(19.07640, 72.87810), triangle))
    }

    @Test
    fun accurateFreshFixInsideIsAllowed() {
        val result = evaluateGeofence(location(19.0760, 72.8777), config())
        assertEquals(GeofenceCheckResult.Allowed, result)
    }

    @Test
    fun accurateFreshFixOutsideIsDeniedAsOutside() {
        val result = evaluateGeofence(location(19.0800, 72.8777), config())
        assertEquals(
            GeofenceCheckResult.Denied(GeofenceDenialReason.OUTSIDE_AREA),
            result
        )
    }

    @Test
    fun nullLocationIsUnavailableNotOutside() {
        val result = evaluateGeofence(null, config())
        assertEquals(
            GeofenceCheckResult.Denied(GeofenceDenialReason.LOCATION_UNAVAILABLE),
            result
        )
    }

    @Test
    fun cachedFixIsRejectedByFreshOnly() {
        val result = evaluateGeofence(
            location(19.0760, 72.8777, isFromCache = true),
            config(mode = PhotoCaptureConfig.GeofenceLocationMode.FRESH_ONLY)
        )
        assertEquals(
            GeofenceCheckResult.Denied(GeofenceDenialReason.LOCATION_UNAVAILABLE),
            result
        )
    }

    @Test
    fun cachedFixIsAcceptedByFallbackMode() {
        val result = evaluateGeofence(
            location(19.0760, 72.8777, isFromCache = true),
            config(mode = PhotoCaptureConfig.GeofenceLocationMode.FRESH_WITH_CACHE_FALLBACK)
        )
        assertEquals(GeofenceCheckResult.Allowed, result)
    }

    @Test
    fun cachedFixIsAcceptedByCacheOnly() {
        val result = evaluateGeofence(
            location(19.0760, 72.8777, isFromCache = true),
            config(mode = PhotoCaptureConfig.GeofenceLocationMode.CACHE_ONLY)
        )
        assertEquals(GeofenceCheckResult.Allowed, result)
    }

    @Test
    fun accuracyWorseThanLimitIsDenied() {
        val result = evaluateGeofence(location(19.0760, 72.8777, accuracy = 15f), config())
        assertEquals(
            GeofenceCheckResult.Denied(GeofenceDenialReason.ACCURACY_TOO_LOW, 15f),
            result
        )
    }

    @Test
    fun accuracyExactlyAtLimitPasses() {
        val result = evaluateGeofence(location(19.0760, 72.8777, accuracy = 10f), config())
        assertEquals(GeofenceCheckResult.Allowed, result)
    }

    @Test
    fun unknownAccuracyFailsTheAccuracyGate() {
        val result = evaluateGeofence(location(19.0760, 72.8777, accuracy = null), config())
        assertEquals(
            GeofenceCheckResult.Denied(GeofenceDenialReason.ACCURACY_TOO_LOW, null),
            result
        )
    }

    @Test
    fun accuracyIsCheckedBeforeThePolygon() {
        // Outside AND inaccurate: the answer is the accuracy denial,
        // not the outside denial, so the user is told to wait for a
        // better fix rather than that they are in the wrong place.
        val result = evaluateGeofence(location(19.0800, 72.8777, accuracy = 50f), config())
        assertEquals(
            GeofenceCheckResult.Denied(GeofenceDenialReason.ACCURACY_TOO_LOW, 50f),
            result
        )
    }
}
