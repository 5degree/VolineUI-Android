@file:Suppress("unused")

package `in`.fivedegree.volineui.photocapturemanager

/**
 * One point on the Earth, in degrees. Pure data, no Android types,
 * so the host can build it from any source (a server response, a
 * map tap, a stored site boundary) without a conversion layer.
 *
 * Used for the capture-area geofence
 * ([PhotoCaptureConfig.allowedCaptureArea]): the polygon is a list
 * of these points, in order, forming one ring. The ring auto-closes
 * -- the host need not repeat the first point at the end -- and the
 * winding order does not matter.
 *
 * @property latitude Latitude in degrees, -90 to 90.
 * @property longitude Longitude in degrees, -180 to 180.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)
