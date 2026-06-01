package `in`.fivedegree.volineuiandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import `in`.fivedegree.volineui.LocationManager
import `in`.fivedegree.volineui.PermissionManager
import `in`.fivedegree.volineui.locationmanager.LocationException
import `in`.fivedegree.volineui.locationmanager.LocationResult
import `in`.fivedegree.volineui.locationmanager.LocationStatus
import `in`.fivedegree.volineuiandroid.databinding.ActivityLocationExamplesBinding

@SuppressLint("SetTextI18n")
/**
 * Location Examples Activity - Demonstrates all features of LocationManager
 *
 * Features demonstrated:
 * 1. Get cached location (instant)
 * 2. Get latest location (fresh, one-time)
 * 3. Start location streaming with different intervals (5s, 10s)
 * 4. Stop specific subscription
 * 5. Stop all subscriptions
 * 6. Check location status
 * 7. Open Google Maps with current location
 * 8. Request location permission
 * 9. Open location settings
 */
class LocationExamplesActivity : AppCompatActivity() {

    private var binding: ActivityLocationExamplesBinding? = null
    private val b get() = binding!!

    // LocationManager instance (no initialization needed!)
    private val locationManager get() = LocationManager.instance

    // Store subscription IDs for management
    private val subscriptionIds = mutableListOf<String>()

    // Track last location for maps
    private var lastLocation: LocationResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLocationExamplesBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        updateLocationStatus()
    }

    private fun setupUI() {
        // Get Location
        b.btnGetCached.setOnClickListener { getCachedLocation() }
        b.btnGetLatest.setOnClickListener { getLatestLocation() }

        // Location Streaming
        b.btnStart5s.setOnClickListener { startLocationStream(5000, "5s") }
        b.btnStart10s.setOnClickListener { startLocationStream(10000, "10s") }
        b.btnStopRecent.setOnClickListener { stopRecentSubscription() }
        b.btnStopAll.setOnClickListener { stopAllSubscriptions() }

        // Utilities
        b.btnCheckStatus.setOnClickListener { checkLocationStatus() }
        b.btnOpenMaps.setOnClickListener { openInMaps() }

        // Settings
        b.btnRequestPermission.setOnClickListener { requestLocationPermission() }
        b.btnOpenLocationSettings.setOnClickListener {
            locationManager.openLocationSettings()
            showToast("Opening location settings...")
        }
    }

    /**
     * Example 1: Get cached location (instant, no network call)
     */
    private fun getCachedLocation() {
        locationManager.getCachedLocation { location ->
            if (location != null) {
                lastLocation = location
                displayLocation(location, "Cached Location")
                showToast("Got cached location!")
            } else {
                b.tvCurrentLocation.text =
                    "⚠️ No cached location available\n\nTry getting latest location first."
                showToast("No cached location available")
            }
        }
    }

    /**
     * Example 2: Get latest location (fresh, one-time)
     */
    private fun getLatestLocation() {
        b.tvCurrentLocation.text = "🔄 Fetching latest location...\n\nThis may take a few seconds."

        locationManager.getLatestLocation { location ->
            if (location != null) {
                lastLocation = location
                displayLocation(location, "Latest Location")
                showToast("Got latest location!")
            } else {
                b.tvCurrentLocation.text = "❌ Unable to get location\n\n" +
                        "Check permissions and location services."
            }
        }
    }

    /**
     * Example 3: Start location streaming with specified interval
     */
    private fun startLocationStream(intervalMs: Long, label: String) {
        try {
            val subscriptionId = locationManager.startLocationUpdates(intervalMs) { location ->
                lastLocation = location
                displayLocation(location, "Streaming ($label interval)")
            }

            subscriptionIds.add(subscriptionId)
            updateSubscriptionCount()
            showToast("Started location stream ($label)")

        } catch (e: LocationException) {
            locationManager.handleLocationException(e)
        }
    }

    /**
     * Example 4: Stop most recent subscription
     */
    private fun stopRecentSubscription() {
        if (subscriptionIds.isEmpty()) {
            showToast("No active subscriptions")
            return
        }

        val subscriptionId = subscriptionIds.removeLastOrNull()
        if (subscriptionId != null) {
            locationManager.stopLocationUpdates(subscriptionId)
            updateSubscriptionCount()
            showToast("Stopped recent subscription")
        }
    }

    /**
     * Example 5: Stop all subscriptions
     */
    private fun stopAllSubscriptions() {
        if (subscriptionIds.isEmpty()) return

        locationManager.stopAllLocationUpdates()
        subscriptionIds.clear()
        updateSubscriptionCount()
        showToast("Stopped all subscriptions")
    }

    /**
     * Example 6: Check location status
     */
    private fun checkLocationStatus() {
        val status = locationManager.checkLocationStatus()
        val hasPermission = locationManager.hasLocationPermission
        val isEnabled = locationManager.isLocationEnabled

        val message = buildString {
            append("Location Status Check\n\n")
            append("Overall Status: ${status.name}\n\n")
            append("Permission: ${if (hasPermission) "✓ Granted" else "✗ Denied"}\n")
            append("Location Services: ${if (isEnabled) "✓ Enabled" else "✗ Disabled"}\n")
            append("Active Streams: ${locationManager.activeSubscriptionCount}\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Location Status")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .apply {
                when (status) {
                    LocationStatus.PERMISSION_DENIED -> {
                        setNeutralButton("Request Permission") { _, _ ->
                            requestLocationPermission()
                        }
                    }

                    LocationStatus.SERVICES_DISABLED -> {
                        setNeutralButton("Open Settings") { _, _ ->
                            locationManager.openLocationSettings()
                        }
                    }

                    else -> {}
                }
            }
            .show()

        updateLocationStatus()
    }

    /**
     * Example 7: Open current location in Google Maps
     */
    private fun openInMaps() {
        val location = lastLocation
        if (location == null) {
            showToast("No location available. Get location first.")
            return
        }

        try {
            val uri = Uri.parse(location.toGoogleMapsUrl())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser if Google Maps not installed
            val uri = Uri.parse(location.toGoogleMapsUrl())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    /**
     * Example 8: Request location permission
     */
    private fun requestLocationPermission() {
        PermissionManager.instance.requestLocationPermission { results ->
            if (results.isGranted) {
                showToast("✓ Location permission granted!")
                updateLocationStatus()
            } else {
                if (results.isPermanentlyDenied) {
                    locationManager.showPermanentlyDeniedDialog()
                } else {
                    showToast("✗ Location permission denied")
                }
            }
        }
    }

    /**
     * Display location data in the UI
     */
    private fun displayLocation(location: LocationResult, title: String) {
        val displayText = buildString {
            append("📍 $title\n\n")
            append("Coordinates: ${location.coordinatesString}\n")
            append("Accuracy: ${location.accuracyString}\n")
            append("Altitude: ${location.altitudeString}\n")
            append("Speed: ${location.speedKmhString}\n")
            append("Bearing: ${location.bearingString}\n")
            append("Time: ${location.getTimeAgo()}\n")
            if (location.isFromCache) {
                append("\n⚡ From cache")
            }
        }

        b.tvCurrentLocation.text = displayText
    }

    /**
     * Update location status display
     */
    private fun updateLocationStatus() {
        val status = locationManager.checkLocationStatus()
        val hasPermission = locationManager.hasLocationPermission
        val isEnabled = locationManager.isLocationEnabled

        val statusText = buildString {
            val statusIcon = when (status) {
                LocationStatus.AVAILABLE -> "✓"
                LocationStatus.PERMISSION_DENIED -> "✗"
                LocationStatus.SERVICES_DISABLED -> "⚠"
                LocationStatus.UNAVAILABLE -> "❌"
            }

            append("$statusIcon Status: ${status.name}\n\n")
            append("Permission: ${if (hasPermission) "✓ Granted" else "✗ Denied"}\n")
            append("Services: ${if (isEnabled) "✓ Enabled" else "✗ Disabled"}")
        }

        b.tvLocationStatus.text = statusText
    }

    /**
     * Update subscription count display
     */
    private fun updateSubscriptionCount() {
        val count = locationManager.activeSubscriptionCount
        b.tvSubscriptions.text = "Active subscriptions: $count"
    }

    /**
     * Helper to show toast messages
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Update status when returning from settings
        updateLocationStatus()
        updateSubscriptionCount()
    }

    override fun onPause() {
        super.onPause()
        // Optional: Stop location updates when app goes to background
        // Uncomment if you want to save battery
        // stopAllSubscriptions()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up all subscriptions
        stopAllSubscriptions()
        binding = null
    }
}
