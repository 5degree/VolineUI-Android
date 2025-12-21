@file:Suppress("unused")

package com.cropintellix.volineui.permissionmanager

/**
 * PermissionResult - Result data for a permission request
 * 
 * Contains all relevant information about a permission request result,
 * making it easy to handle the outcome in a callback.
 * 
 * @property permission The permission that was requested (e.g., Manifest.permission.CAMERA)
 * @property status Current status of the permission after the request
 * @property shouldShowRationale Whether app should show rationale to explain why permission is needed
 */
data class PermissionResult(
    val permission: String,
    val status: PermissionStatus,
    val shouldShowRationale: Boolean
) {
    /**
     * Convenience property - true if permission is granted
     */
    val isGranted: Boolean
        get() = status == PermissionStatus.GRANTED
    
    /**
     * Convenience property - true if permission is denied (not permanently)
     */
    val isDenied: Boolean
        get() = status == PermissionStatus.DENIED
    
    /**
     * Convenience property - true if permission is permanently denied
     */
    val isPermanentlyDenied: Boolean
        get() = status == PermissionStatus.PERMANENTLY_DENIED
}
