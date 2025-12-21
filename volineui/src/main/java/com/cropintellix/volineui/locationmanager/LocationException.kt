@file:Suppress("unused")

package com.cropintellix.volineui.locationmanager

/**
 * LocationException - Custom exception for location-related errors
 * 
 * Thrown when there's an error in location handling, such as not initializing
 * the LocationManager, missing permissions, or disabled location services.
 */
class LocationException(message: String) : Exception(message) {
    
    companion object {
        /**
         * Error: LocationManager.init() was not called
         */
        const val ERROR_NOT_INITIALIZED = 
            "LocationManager not initialized. Call LocationManager.init() in your Application class."
        
        /**
         * Error: Location permission not granted
         */
        const val ERROR_PERMISSION_DENIED = 
            "Location permission not granted. Request location permission before accessing location."
        
        /**
         * Error: Location services are disabled on device
         */
        const val ERROR_SERVICES_DISABLED = 
            "Location services are disabled. Please enable location services in device settings."
        
        /**
         * Error: Location fetch operation timeout
         */
        const val ERROR_TIMEOUT = 
            "Location fetch timeout. Unable to get location within the specified time limit."
        
        /**
         * Error: Location provider unavailable
         */
        const val ERROR_UNAVAILABLE = 
            "Location provider unavailable. Check device location settings and try again."
        
        /**
         * Error: Google Play Services not available
         */
        const val ERROR_PLAY_SERVICES_UNAVAILABLE = 
            "Google Play Services is required for location services but is not available on this device."
    }
}
