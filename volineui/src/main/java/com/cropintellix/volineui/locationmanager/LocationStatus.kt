@file:Suppress("unused")

package com.cropintellix.volineui.locationmanager

/**
 * LocationStatus - Represents the current state of location services
 * 
 * This enum provides a clear representation of location availability,
 * making it easy to handle different location scenarios in your app.
 */
enum class LocationStatus {
    /**
     * Location is available - services enabled and permission granted
     * App can fetch and receive location updates
     */
    AVAILABLE,
    
    /**
     * Location permission not granted
     * App should request location permission from user
     */
    PERMISSION_DENIED,
    
    /**
     * Location services are disabled on the device
     * App should guide user to enable location in device settings
     */
    SERVICES_DISABLED,
    
    /**
     * Location provider is unavailable
     * This may be temporary (e.g., poor GPS signal)
     */
    UNAVAILABLE
}
