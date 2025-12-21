@file:Suppress("unused")

package com.cropintellix.volineui.permissionmanager

/**
 * PermissionStatus - Represents the current state of a runtime permission
 * 
 * This enum provides a clear representation of a permission's status,
 * making it easy to handle different permission scenarios in your app.
 */
enum class PermissionStatus {
    /**
     * Permission is granted - app has access to the protected feature
     */
    GRANTED,
    
    /**
     * Permission is denied but can be requested again
     * User denied the permission but didn't select "Don't ask again"
     */
    DENIED,
    
    /**
     * Permission is permanently denied - user selected "Don't ask again"
     * App should direct user to settings to manually grant permission
     */
    PERMANENTLY_DENIED,
    
    /**
     * Permission has never been requested yet
     */
    NOT_REQUESTED
}
