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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.cropintellix.volineui.locationmanager.LocationException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
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
    
    // Settings client for location settings requests
    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(application)
    
    // Activity result launcher for location settings - registered per activity
    private var locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    
    // Pending callback to execute after location is enabled
    private var pendingLocationCallback: ((LocationResult?) -> Unit)? = null
    private var pendingLocationTimeout: Long = 30_000
    private var pendingIsCached: Boolean = false

    init {
        // Register activity lifecycle callbacks to automatically track current activity
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                    registerLocationSettingsLauncher(activity)
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
    private fun getCachedLocationInternal(callback: (LocationResult?) -> Unit, promptForLocation: Boolean = true) {
        // Check if location services are enabled
        if (!isLocationEnabled && promptForLocation) {
            promptEnableLocation(
                callback = callback,
                isCached = true
            )
            return
        }
        
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
            // Prompt user to enable location services
            promptEnableLocation(
                timeout = timeout,
                callback = callback,
                isCached = false
            )
            return
        }

        // Track if callback has been invoked to prevent duplicate calls
        var callbackInvoked = false
        val callbackLock = Any()
        
        // Create a one-time location request
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMaxUpdateDelayMillis(timeout)
            .setMinUpdateIntervalMillis(0)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: GmsLocationResult) {
                synchronized(callbackLock) {
                    if (callbackInvoked) return
                    callbackInvoked = true
                }
                
                // Remove this callback to stop further updates
                fusedLocationClient.removeLocationUpdates(this)
                
                val location = result.lastLocation
                if (location != null) {
                    callback(location.toLocationResult(isFromCache = false))
                } else {
                    // Fallback to cached location
                    getCachedLocationInternal(callback, promptForLocation = false)
                }
            }
        }

        // Set up a timeout handler to ensure callback is always invoked
        val timeoutHandler = android.os.Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            synchronized(callbackLock) {
                if (callbackInvoked) return@Runnable
                callbackInvoked = true
            }
            
            // Remove the location callback since we're timing out
            fusedLocationClient.removeLocationUpdates(locationCallback)
            
            // Fallback to cached location on timeout
            getCachedLocationInternal(callback, promptForLocation = false)
        }
        
        // Schedule timeout - use the provided timeout value
        timeoutHandler.postDelayed(timeoutRunnable, timeout)

        // Request location update
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnSuccessListener {
            // Location updates started successfully
        }.addOnFailureListener {
            // Cancel the timeout since we're handling failure immediately
            timeoutHandler.removeCallbacks(timeoutRunnable)
            
            synchronized(callbackLock) {
                if (callbackInvoked) return@addOnFailureListener
                callbackInvoked = true
            }
            
            // Fallback to cached location
            getCachedLocationInternal(callback, promptForLocation = false)
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
     * Register the location settings launcher for the current activity
     */
    private fun registerLocationSettingsLauncher(activity: ComponentActivity) {
        try {
            locationSettingsLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // Location was enabled, execute pending callback
                    executePendingLocationRequest()
                } else {
                    // User declined to enable location
                    pendingLocationCallback?.invoke(null)
                    clearPendingRequest()
                }
            }
        } catch (e: Exception) {
            // Activity might already be started, ignore registration errors
        }
    }
    
    /**
     * Prompt user to enable location services using Google Play Services dialog
     */
    private fun promptEnableLocation(
        timeout: Long = 30_000,
        callback: (LocationResult?) -> Unit,
        isCached: Boolean
    ) {
        val activity = currentActivityRef?.get()
        if (activity == null) {
            callback(null)
            return
        }
        
        // Store pending request info
        pendingLocationCallback = callback
        pendingLocationTimeout = timeout
        pendingIsCached = isCached
        
        // Build location request for settings check
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000)
            .setMinUpdateIntervalMillis(5_000)
            .build()
        
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Shows the dialog even if settings are already adequate
            .build()
        
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                // Location settings are already satisfied, proceed with location request
                executePendingLocationRequest()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        // Show the system dialog to enable location
                        val launcher = locationSettingsLauncher
                        if (launcher != null) {
                            val intentSenderRequest = IntentSenderRequest.Builder(
                                exception.resolution.intentSender
                            ).build()
                            launcher.launch(intentSenderRequest)
                        } else {
                            // Fallback: show manual dialog if launcher not available
                            showLocationServicesDialog(callback)
                        }
                    } catch (e: Exception) {
                        // Fallback to manual dialog
                        showLocationServicesDialog(callback)
                    }
                } else {
                    // Location settings are inadequate and cannot be resolved
                    showLocationServicesDialog(callback)
                }
            }
    }
    
    /**
     * Execute the pending location request after location is enabled
     */
    @Suppress("MissingPermission")
    private fun executePendingLocationRequest() {
        val callback = pendingLocationCallback ?: return
        val isCached = pendingIsCached
        val timeout = pendingLocationTimeout
        
        clearPendingRequest()
        
        if (isCached) {
            getCachedLocationInternal(callback, promptForLocation = false)
        } else {
            // Re-check if location is now enabled
            if (isLocationEnabled) {
                fetchFreshLocation(timeout, callback)
            } else {
                callback(null)
            }
        }
    }
    
    /**
     * Clear pending location request
     */
    private fun clearPendingRequest() {
        pendingLocationCallback = null
        pendingLocationTimeout = 30_000
        pendingIsCached = false
    }
    
    /**
     * Fetch fresh location (called after location is confirmed enabled)
     */
    @Suppress("MissingPermission")
    private fun fetchFreshLocation(timeout: Long, callback: (LocationResult?) -> Unit) {
        // Track if callback has been invoked to prevent duplicate calls
        var callbackInvoked = false
        val callbackLock = Any()
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMaxUpdateDelayMillis(timeout)
            .setMinUpdateIntervalMillis(0)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: GmsLocationResult) {
                synchronized(callbackLock) {
                    if (callbackInvoked) return
                    callbackInvoked = true
                }
                
                // Remove this callback to stop further updates
                fusedLocationClient.removeLocationUpdates(this)
                
                val location = result.lastLocation
                if (location != null) {
                    callback(location.toLocationResult(isFromCache = false))
                } else {
                    getCachedLocationInternal(callback, promptForLocation = false)
                }
            }
        }

        // Set up a timeout handler to ensure callback is always invoked
        val timeoutHandler = android.os.Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            synchronized(callbackLock) {
                if (callbackInvoked) return@Runnable
                callbackInvoked = true
            }
            
            // Remove the location callback since we're timing out
            fusedLocationClient.removeLocationUpdates(locationCallback)
            
            // Fallback to cached location on timeout
            getCachedLocationInternal(callback, promptForLocation = false)
        }
        
        // Schedule timeout
        timeoutHandler.postDelayed(timeoutRunnable, timeout)

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnSuccessListener {
            // Location updates started successfully
        }.addOnFailureListener {
            // Cancel the timeout since we're handling failure immediately
            timeoutHandler.removeCallbacks(timeoutRunnable)
            
            synchronized(callbackLock) {
                if (callbackInvoked) return@addOnFailureListener
                callbackInvoked = true
            }
            
            getCachedLocationInternal(callback, promptForLocation = false)
        }
    }
    
    /**
     * Show fallback dialog for enabling location services manually
     */
    private fun showLocationServicesDialog(callback: (LocationResult?) -> Unit) {
        val activity = currentActivityRef?.get()
        if (activity == null) {
            callback(null)
            return
        }
        
        AlertDialog.Builder(activity)
            .setTitle("Enable Location")
            .setMessage("Location services are required for this feature. Please enable GPS in settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openLocationSettings()
                callback(null)
            }
            .setNegativeButton("Cancel") { _, _ ->
                callback(null)
            }
            .setCancelable(false)
            .show()
    }

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
