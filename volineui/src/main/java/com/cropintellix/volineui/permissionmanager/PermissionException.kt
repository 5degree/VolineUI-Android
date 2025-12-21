@file:Suppress("unused")

package com.cropintellix.volineui.permissionmanager

/**
 * PermissionException - Custom exception for permission-related errors
 * 
 * Thrown when there's an error in permission handling, such as not initializing
 * the PermissionManager or requesting an invalid permission.
 */
class PermissionException(message: String) : Exception(message) {
    
    companion object {
        /**
         * Error: PermissionManager.init() was not called
         */
        const val ERROR_NOT_INITIALIZED = 
            "PermissionManager not initialized. Call PermissionManager.init() in your Application class."
        
        /**
         * Error: Invalid or malformed permission string
         */
        const val ERROR_INVALID_PERMISSION = 
            "Invalid permission string provided."
        
        /**
         * Error: Activity is destroyed or not available
         */
        const val ERROR_ACTIVITY_DESTROYED = 
            "Cannot request permission - no active activity available. Make sure an activity is in the foreground."
        
        /**
         * Error: Permission not in configured list
         */
        const val ERROR_PERMISSION_NOT_CONFIGURED = 
            "Permission not in configured list. Add it to PermissionManager.init() or use requestPermission() for ad-hoc requests."
    }
}
