@file:Suppress("unused")

package `in`.fivedegree.volineui.camera

import `in`.fivedegree.volineui.locationmanager.LocationResult
import `in`.fivedegree.volineui.photocapturemanager.GeoPoint
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureConfig

/**
 * The geofence for one capture, as the camera screen receives it
 * from the host's config (via intent extras). Null in the screen
 * means the feature is off. Internal: hosts configure the geofence
 * through [PhotoCaptureConfig], never this type directly.
 */
internal data class GeofenceConfig(
    val polygon: List<GeoPoint>,
    val mode: PhotoCaptureConfig.GeofenceLocationMode,
    val timeoutMs: Long,
    val maxAccuracyM: Float
)

/** Why a geofence check refused the shutter. */
internal enum class GeofenceDenialReason {
    /** The location was fixed and accurate, but outside the polygon. */
    OUTSIDE_AREA,

    /** No usable location arrived (timeout, GPS off, Play-services failure). */
    LOCATION_UNAVAILABLE,

    /** A fix arrived, but worse than the host's accuracy limit (or unknown). */
    ACCURACY_TOO_LOW,

    /** Location permission is not granted. */
    PERMISSION_DENIED
}

/** The outcome of one geofence check. */
internal sealed interface GeofenceCheckResult {
    data object Allowed : GeofenceCheckResult

    data class Denied(
        val reason: GeofenceDenialReason,
        val accuracy: Float? = null
    ) : GeofenceCheckResult
}

/**
 * Evaluates one location against the geofence. Pure -- no Android
 * location calls, no UI -- so it is unit-tested on the JVM.
 *
 * The order matters: an absent location is unavailable, not outside;
 * a cached fix only counts when the host's mode allows cached fixes;
 * accuracy is checked before the polygon, because a fix that cannot
 * prove it is within the accuracy limit cannot prove it is inside
 * the area. A fix with unknown accuracy fails the accuracy gate for
 * the same reason.
 */
internal fun evaluateGeofence(
    location: LocationResult?,
    config: GeofenceConfig
): GeofenceCheckResult {
    if (location == null) {
        return GeofenceCheckResult.Denied(GeofenceDenialReason.LOCATION_UNAVAILABLE)
    }
    if (config.mode == PhotoCaptureConfig.GeofenceLocationMode.FRESH_ONLY &&
        location.isFromCache
    ) {
        return GeofenceCheckResult.Denied(GeofenceDenialReason.LOCATION_UNAVAILABLE)
    }
    val accuracy = location.accuracy
    if (accuracy == null || accuracy > config.maxAccuracyM) {
        return GeofenceCheckResult.Denied(GeofenceDenialReason.ACCURACY_TOO_LOW, accuracy)
    }
    return if (isInsidePolygon(GeoPoint(location.latitude, location.longitude), config.polygon)) {
        GeofenceCheckResult.Allowed
    } else {
        GeofenceCheckResult.Denied(GeofenceDenialReason.OUTSIDE_AREA)
    }
}

/**
 * Point-in-polygon on latitude/longitude treated as planar
 * coordinates. That is correct for the site-sized polygons this
 * feature is for (tens/hundreds of metres); it is not a spherical
 * test and must not be used for very large areas.
 *
 * Ray casting (even-odd rule): a horizontal ray from the point
 * crosses the ring an odd number of times exactly when the point is
 * inside. A point on an edge or a vertex counts as inside, checked
 * first so a boundary point never depends on ray parity. The winding
 * order of the ring does not matter.
 */
internal fun isInsidePolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
    if (polygon.size < 3) return false
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        if (point.isOnSegment(a, b)) return true
    }
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i].longitude
        val yi = polygon[i].latitude
        val xj = polygon[j].longitude
        val yj = polygon[j].latitude
        if ((yi > point.latitude) != (yj > point.latitude)) {
            val xIntersect = (xj - xi) * (point.latitude - yi) / (yj - yi) + xi
            if (point.longitude < xIntersect) inside = !inside
        }
        j = i
    }
    return inside
}

/**
 * Whether this point lies on segment AB, within a small tolerance.
 * The cross product is in square degrees: for a ~100 m segment,
 * [BOUNDARY_EPSILON] corresponds to roughly a decimetre off the
 * line, which is far tighter than GPS accuracy, so only a genuine
 * boundary point matches.
 */
private fun GeoPoint.isOnSegment(a: GeoPoint, b: GeoPoint): Boolean {
    val cross = (longitude - a.longitude) * (b.latitude - a.latitude) -
        (latitude - a.latitude) * (b.longitude - a.longitude)
    if (kotlin.math.abs(cross) > BOUNDARY_EPSILON) return false
    return longitude in
        minOf(a.longitude, b.longitude) - BOUNDARY_EPSILON..
        maxOf(a.longitude, b.longitude) + BOUNDARY_EPSILON &&
        latitude in
        minOf(a.latitude, b.latitude) - BOUNDARY_EPSILON..
        maxOf(a.latitude, b.latitude) + BOUNDARY_EPSILON
}

private const val BOUNDARY_EPSILON = 1e-9
