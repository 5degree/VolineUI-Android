@file:Suppress("unused")

package com.cropintellix.volineui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.cropintellix.volineui.locationmanager.LocationException
import com.cropintellix.volineui.locationmanager.LocationResult
import com.cropintellix.volineui.locationmanager.LocationStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import android.location.LocationManager as AndroidLocationManager
import com.google.android.gms.location.LocationResult as GmsLocationResult

/**
 * LocationManager - A comprehensive and reusable location manager for Android
 *
 * Features:
 * - Cached location retrieval (instant, no network call)
 * - Latest location fetching (fresh, one-time)
 * - Location streaming with dynamic intervals
 * - Automatic permission handling via PermissionManager
 * - Location settings validation
 * - Multiple simultaneous subscriptions support
 * - Thread-safe operations
 * - Zero boilerplate for users
 *
 * Usage:
 * ```kotlin
 * // 1. Initialize once in Application class
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         PermissionManager.init(this)
 *         LocationManager.init(this)
 *     }
 * }
 *
 * // 2. Get cached location (instant)
 * LocationManager.instance.getCachedLocation { location ->
 *     location?.let {
 *         println("Cached: ${it.coordinatesString}")
 *     }
 * }
 *
 * // 3. Get latest location (fresh, one-time)
 * LocationManager.instance.getLatestLocation { location ->
 *     location?.let {
 *         println("Latest: ${it.coordinatesString}")
 *     }
 * }
 *
 * // 4. Stream location updates
 * val subscriptionId = LocationManager.instance.startLocationUpdates(5000) { location ->
 *     println("Update: ${location.coordinatesString}")
 * }
 *
 * // 5. Stop updates
 * LocationManager.instance.stopLocationUpdates(subscriptionId)
 * ```
 */
class LocationManager private constructor(
    private val application: Application,
) {

    // FusedLocationProviderClient for accessing location services
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    // Active location subscriptions (subscription ID -> callback + LocationCallback)
    private val activeSubscriptions = ConcurrentHashMap<String, SubscriptionData>()

    /**
     * Data class to hold subscription information
     */
    private data class SubscriptionData(
        val callback: (LocationResult) -> Unit,
        val locationCallback: LocationCallback,
    )

    /**
     * Check if location permission is granted
     */
    val hasLocationPermission: Boolean
        get() = try {
            PermissionManager.instance.isLocationPermissionGranted
        } catch (e: Exception) {
            false
        }

    /**
     * Check if location services are enabled on device
     */
    val isLocationEnabled: Boolean
        get() {
            val locationManager = application.getSystemService(Context.LOCATION_SERVICE)
                    as? AndroidLocationManager ?: return false
            return locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
        }

    private var currentActivityRef: WeakReference<ComponentActivity>? = null

    init {
        // Register activity lifecycle callbacks to automatically track current activity
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivityRef?.get() == activity) {
                    currentActivityRef = null
                }
            }
        })
    }

    /**
     * Check overall location status
     *
     * @return Current location status
     */
    fun checkLocationStatus(): LocationStatus {
        return when {
            !hasLocationPermission -> LocationStatus.PERMISSION_DENIED
            !isLocationEnabled -> LocationStatus.SERVICES_DISABLED
            else -> LocationStatus.AVAILABLE
        }
    }

    /**
     * Get cached location (instant, no network call)
     *
     * Returns the last known location from the device cache. This is very fast
     * and doesn't trigger any network requests or GPS activity.
     *
     * If permission is not granted, automatically requests it and retries after grant.
     * If permission is permanently denied, a dialog is shown.
     *
     * @param callback Callback invoked with cached location (null if unavailable or permission denied)
     */
    @Suppress("MissingPermission")
    fun getCachedLocation(
        callback: (LocationResult?) -> Unit,
    ) {
        // Check permission first
        if (!hasLocationPermission) {
            // Automatically request permission
            PermissionManager.instance.requestLocationPermission { results ->
                if (results.isGranted) {
                    // Permission granted, retry getting cached location
                    getCachedLocationInternal(callback)
                } else {
                    // Permission denied, show rationale dialog
                    showPermanentlyDeniedDialog()
                    callback(null)
                }
            }
            return
        }

        getCachedLocationInternal(callback)
    }

    /**
     * Internal method to get cached location (assumes permission is granted)
     */
    @Suppress("MissingPermission")
    private fun getCachedLocationInternal(callback: (LocationResult?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                val result = location?.toLocationResult(isFromCache = true)
                callback(result)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    /**
     * Get latest location (fresh, one-time fetch)
     *
     * Fetches a fresh location update. This may take a few seconds and will
     * activate GPS/network location providers. Falls back to cached location
     * if fresh location unavailable.
     *
     * If permission is not granted, automatically requests it and retries after grant.
     * If permission is permanently denied, a dialog is shown.
     *
     * @param timeout Timeout in milliseconds (default: 30 seconds)
     * @param callback Callback invoked with location result (null if unavailable or permission denied)
     */
    @Suppress("MissingPermission")
    fun getLatestLocation(
        timeout: Long = 30_000,
        callback: (LocationResult?) -> Unit,
    ) {
        // Check permission first
        if (!hasLocationPermission) {
            // Automatically request permission
            PermissionManager.instance.requestLocationPermission { results ->
                if (results.isGranted) {
                    // Permission granted, retry getting latest location
                    getLatestLocationInternal(timeout, callback)
                } else {
                    // Permission denied, invoke callback if provided
                    showPermanentlyDeniedDialog()
                    callback(null)
                }
            }
            return
        }

        getLatestLocationInternal(timeout, callback)
    }

    /**
     * Internal method to get latest location (assumes permission is granted)
     */
    @Suppress("MissingPermission")
    private fun getLatestLocationInternal(
        timeout: Long,
        callback: (LocationResult?) -> Unit,
    ) {
        // Check if location services are enabled
        if (!isLocationEnabled) {
            // Fallback to cached location (internal version to avoid recursive permission check)
            getCachedLocationInternal(callback)
            return
        }

        // Create a one-time location request
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMaxUpdateDelayMillis(timeout)
            .setMinUpdateIntervalMillis(0)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: GmsLocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    callback(location.toLocationResult(isFromCache = false))
                } else {
                    // Fallback to cached location
                    getCachedLocationInternal(callback)
                }
            }
        }

        // Request location update
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener {
            // Fallback to cached location
            getCachedLocationInternal(callback)
        }
    }

    /**
     * Start receiving location updates at specified interval
     *
     * Continuously streams location updates at the specified interval.
     * Multiple subscriptions with different intervals are supported.
     *
     * @param intervalMillis Update interval in milliseconds (e.g., 5000 for 5 seconds)
     * @param priority Location priority (default: HIGH_ACCURACY)
     * @param callback Callback invoked with each location update
     * @return Subscription ID to use for stopping updates
     * @throws com.cropintellix.volineui.locationmanager.LocationException if permission not granted or services disabled
     */
    @Suppress("MissingPermission")
    fun startLocationUpdates(
        intervalMillis: Long,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
        callback: (LocationResult) -> Unit,
    ): String {
        // Check permission
        if (!hasLocationPermission) {
            throw LocationException(LocationException.ERROR_PERMISSION_DENIED)
        }

        // Check location services
        if (!isLocationEnabled) {
            throw LocationException(LocationException.ERROR_SERVICES_DISABLED)
        }

        // Create location request
        val locationRequest = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setWaitForAccurateLocation(false)
            .build()

        // Create location callback
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: GmsLocationResult) {
                result.lastLocation?.let { location ->
                    callback(location.toLocationResult(isFromCache = false))
                }
            }
        }

        // Generate unique subscription ID
        val subscriptionId = UUID.randomUUID().toString()

        // Store subscription
        activeSubscriptions[subscriptionId] = SubscriptionData(callback, locationCallback)

        // Start location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return subscriptionId
    }

    /**
     * Stop location updates for specific subscription
     *
     * @param subscriptionId Subscription ID returned from startLocationUpdates
     */
    fun stopLocationUpdates(subscriptionId: String) {
        activeSubscriptions[subscriptionId]?.let { subscription ->
            fusedLocationClient.removeLocationUpdates(subscription.locationCallback)
            activeSubscriptions.remove(subscriptionId)
        }
    }

    /**
     * Stop all active location update subscriptions
     */
    fun stopAllLocationUpdates() {
        activeSubscriptions.forEach { (_, subscription) ->
            fusedLocationClient.removeLocationUpdates(subscription.locationCallback)
        }
        activeSubscriptions.clear()
    }

    /**
     * Get count of active location subscriptions
     */
    val activeSubscriptionCount: Int
        get() = activeSubscriptions.size

    /**
     * Get list of active subscription IDs
     */
    val activeSubscriptionIds: List<String>
        get() = activeSubscriptions.keys.toList()

    /**
     * Show dialog for permanently denied permissions
     */
    fun showPermanentlyDeniedDialog(title: String? = null, message: String? = null) {
        currentActivityRef?.let { activityRef ->
            activityRef.get()?.let { activity ->
                AlertDialog.Builder(activity)
                    .setTitle(title ?: "Permission Required")
                    .setMessage(
                        message ?: ("Location permission is required for this feature.\n\n" +
                                "You have selected \"Don't ask again\". Please grant permission manually in app settings.")
                    )
                    .setPositiveButton("Open Settings") { _, _ ->
                        try {
                            PermissionManager.instance.openAppSettings()
                        } catch (_: Exception) {
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    fun handleLocationException(e: LocationException) {
        currentActivityRef?.let { activityRef ->
            activityRef.get()?.let { activity ->
                when (e.message) {
                    LocationException.ERROR_PERMISSION_DENIED -> {

                        AlertDialog.Builder(activity)
                            .setTitle("Permission Required")
                            .setMessage("Location permission is required to access location services.")
                            .setPositiveButton("Grant Permission") { _, _ ->
                                showPermanentlyDeniedDialog()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }

                    LocationException.ERROR_SERVICES_DISABLED -> {
                        AlertDialog.Builder(activity)
                            .setTitle("Location Services Disabled")
                            .setMessage("Please enable location services in device settings.")
                            .setPositiveButton("Open Settings") { _, _ ->
                                openLocationSettings()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }

                    else -> {
                        Toast.makeText(application, "Error: ${e.message}", Toast.LENGTH_SHORT)
                    }
                }
            }
        }
    }

    /**
     * Open device location settings
     *
     * Opens the system settings page where user can enable location services.
     */
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        application.startActivity(intent)
    }

    /**
     * Calculate distance between two coordinates in meters
     *
     * Uses the Haversine formula for calculating great-circle distance.
     *
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in meters
     */
    fun getDistanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Calculate bearing between two coordinates in degrees
     *
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Bearing in degrees (0-360, where 0 is north)
     */
    fun getBearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(2)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[1].let { bearing ->
            if (bearing < 0) bearing + 360f else bearing
        }
    }

    /**
     * Calculate distance between two LocationResults in meters
     */
    fun getDistanceBetween(location1: LocationResult, location2: LocationResult): Float {
        return getDistanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude
        )
    }

    /**
     * Calculate bearing between two LocationResults in degrees
     */
    fun getBearingBetween(location1: LocationResult, location2: LocationResult): Float {
        return getBearingBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude
        )
    }

    /**
     * Convert Android Location to LocationResult
     */
    private fun Location.toLocationResult(isFromCache: Boolean): LocationResult {
        return LocationResult(
            latitude = latitude,
            longitude = longitude,
            accuracy = if (hasAccuracy()) accuracy else null,
            altitude = if (hasAltitude()) altitude else null,
            bearing = if (hasBearing()) bearing else null,
            speed = if (hasSpeed()) speed else null,
            timestamp = time,
            isFromCache = isFromCache
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationManager? = null

        /**
         * Initialize the LocationManager
         *
         * Call this once in your Application class's onCreate()
         * Must be called after PermissionManager.init() for permission integration.
         *
         * @param application Application instance
         */
        @JvmStatic
        fun init(application: Application) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = LocationManager(application)
                    }
                }
            }
        }

        /**
         * Get the singleton instance
         *
         * @throws LocationException if not initialized
         */
        @JvmStatic
        val instance: LocationManager
            get() = INSTANCE
                ?: throw LocationException(LocationException.ERROR_NOT_INITIALIZED)
    }
}
