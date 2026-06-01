@file:Suppress("unused")

package `in`.fivedegree.volineui.locationmanager

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LocationResult - Complete location information with convenience properties
 * 
 * Contains all relevant location data including coordinates, accuracy,
 * speed, bearing, and helpful formatted properties.
 * 
 * @property latitude Latitude in degrees
 * @property longitude Longitude in degrees
 * @property accuracy Estimated horizontal accuracy in meters (null if unavailable)
 * @property altitude Altitude in meters above WGS 84 reference ellipsoid (null if unavailable)
 * @property bearing Bearing in degrees (0-360, where 0 is north) (null if unavailable)
 * @property speed Speed in meters per second (null if unavailable)
 * @property timestamp Time when this location was determined, in milliseconds since epoch
 * @property isFromCache Whether this location was retrieved from cache
 */
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromCache: Boolean = false
) {
    
    /**
     * Formatted coordinates string (e.g., "37.4220, -122.0841")
     */
    val coordinatesString: String
        get() = String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    
    /**
     * Formatted latitude string (e.g., "37.422000")
     */
    val latitudeString: String
        get() = String.format(Locale.US, "%.6f", latitude)
    
    /**
     * Formatted longitude string (e.g., "-122.084100")
     */
    val longitudeString: String
        get() = String.format(Locale.US, "%.6f", longitude)
    
    /**
     * Formatted accuracy string (e.g., "15.0m" or "N/A")
     */
    val accuracyString: String
        get() = accuracy?.let { String.format(Locale.US, "%.1fm", it) } ?: "N/A"
    
    /**
     * Formatted altitude string (e.g., "123.5m" or "N/A")
     */
    val altitudeString: String
        get() = altitude?.let { String.format(Locale.US, "%.1fm", it) } ?: "N/A"
    
    /**
     * Formatted bearing string (e.g., "45.0°" or "N/A")
     */
    val bearingString: String
        get() = bearing?.let { String.format(Locale.US, "%.1f°", it) } ?: "N/A"
    
    /**
     * Formatted speed string in m/s (e.g., "12.5 m/s" or "N/A")
     */
    val speedString: String
        get() = speed?.let { String.format(Locale.US, "%.1f m/s", it) } ?: "N/A"
    
    /**
     * Formatted speed string in km/h (e.g., "45.0 km/h" or "N/A")
     */
    val speedKmhString: String
        get() = speed?.let { String.format(Locale.US, "%.1f km/h", it * 3.6f) } ?: "N/A"
    
    /**
     * Formatted timestamp string (e.g., "2024-12-02 19:13:12")
     */
    val timestampString: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    
    /**
     * Returns time ago string (e.g., "2 minutes ago")
     */
    fun getTimeAgo(): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 1000 -> "Just now"
            diff < 60_000 -> "${diff / 1000} seconds ago"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            else -> "${diff / 86400_000} days ago"
        }
    }
    
    /**
     * Convert to Google Maps URL
     */
    fun toGoogleMapsUrl(): String {
        return "https://www.google.com/maps?q=$latitude,$longitude"
    }
    
    /**
     * Comprehensive string representation
     */
    override fun toString(): String {
        return buildString {
            append("Location(")
            append("lat=$latitudeString, ")
            append("lon=$longitudeString")
            accuracy?.let { append(", accuracy=$accuracyString") }
            altitude?.let { append(", altitude=$altitudeString") }
            bearing?.let { append(", bearing=$bearingString") }
            speed?.let { append(", speed=$speedKmhString") }
            append(", time=${getTimeAgo()}")
            if (isFromCache) append(", cached")
            append(")")
        }
    }
}
