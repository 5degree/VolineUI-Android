@file:Suppress("unused")

package com.cropintellix.volineui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

/**
 * PermissionManager - A comprehensive and reusable permission manager for Android
 * 
 * Features:
 * - Simple one-time initialization in Application class
 * - Automatic activity lifecycle tracking (no manual attach/detach needed)
 * - Check permission status from anywhere
 * - Request permissions with callbacks
 * - Handle permanently denied permissions
 * - Thread-safe singleton pattern
 * - Zero boilerplate in activities
 * 
 * Usage:
 * ```kotlin
 * // 1. Initialize once in Application class
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         PermissionManager.init(this, 
 *             Manifest.permission.CAMERA,
 *             Manifest.permission.ACCESS_FINE_LOCATION
 *         )
 *     }
 * }
 * 
 * // 2. Use anywhere in your app
 * PermissionManager.instance.requestPermission(Manifest.permission.CAMERA) { result ->
 *     if (result.isGranted) {
 *         openCamera()
 *     }
 * }
 * ```
 */
class PermissionManager private constructor(
    private val application: Application,
    private val configuredPermissions: List<String>
) {
    
    // Current activity reference (weak to avoid memory leaks)
    private var currentActivityRef: WeakReference<ComponentActivity>? = null
    
    // Permission launchers for single and multiple requests
    private var singlePermissionLauncher: ActivityResultLauncher<String>? = null
    private var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    
    // Callbacks for permission requests
    private var singlePermissionCallback: ((PermissionResult) -> Unit)? = null
    private var multiplePermissionCallback: ((Map<String, PermissionResult>) -> Unit)? = null
    
    // Track requested permissions to detect permanent denial
    private val requestedPermissions = mutableSetOf<String>()
    
    init {
        // Register activity lifecycle callbacks to automatically track current activity
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                    registerPermissionLaunchers(activity)
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
                    singlePermissionLauncher = null
                    multiplePermissionLauncher = null
                }
            }
        })
    }
    
    /**
     * Register permission launchers for the current activity
     */
    private fun registerPermissionLaunchers(activity: ComponentActivity) {
        // Single permission launcher
        singlePermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val permission = requestedPermissions.lastOrNull() ?: return@registerForActivityResult
            val status = if (isGranted) {
                PermissionStatus.GRANTED
            } else {
                if (shouldShowRationale(permission)) {
                    PermissionStatus.DENIED
                } else {
                    PermissionStatus.PERMANENTLY_DENIED
                }
            }
            
            val result = PermissionResult(
                permission = permission,
                status = status,
                shouldShowRationale = shouldShowRationale(permission)
            )
            
            singlePermissionCallback?.invoke(result)
            singlePermissionCallback = null
        }
        
        // Multiple permissions launcher
        multiplePermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            val results = permissionsMap.map { (permission, isGranted) ->
                val status = if (isGranted) {
                    PermissionStatus.GRANTED
                } else {
                    if (shouldShowRationale(permission)) {
                        PermissionStatus.DENIED
                    } else {
                        PermissionStatus.PERMANENTLY_DENIED
                    }
                }
                
                permission to PermissionResult(
                    permission = permission,
                    status = status,
                    shouldShowRationale = shouldShowRationale(permission)
                )
            }.toMap()
            
            multiplePermissionCallback?.invoke(results)
            multiplePermissionCallback = null
        }
    }
    
    /**
     * Check the status of a single permission
     * 
     * @param permission The permission to check (e.g., Manifest.permission.CAMERA)
     * @return Current status of the permission
     */
    fun checkPermission(permission: String): PermissionStatus {
        return when {
            ContextCompat.checkSelfPermission(application, permission) == 
                PackageManager.PERMISSION_GRANTED -> PermissionStatus.GRANTED
            
            !requestedPermissions.contains(permission) -> PermissionStatus.NOT_REQUESTED
            
            shouldShowRationale(permission) -> PermissionStatus.DENIED
            
            else -> PermissionStatus.PERMANENTLY_DENIED
        }
    }
    
    /**
     * Check the status of multiple permissions
     * 
     * @param permissions Vararg of permissions to check
     * @return Map of permission to its status
     */
    fun checkPermissions(vararg permissions: String): Map<String, PermissionStatus> {
        return permissions.associateWith { checkPermission(it) }
    }
    
    /**
     * Check if all configured permissions are granted
     * 
     * @return true if all permissions are granted, false otherwise
     */
    fun hasAllRequiredPermissions(): Boolean {
        return configuredPermissions.all { 
            checkPermission(it) == PermissionStatus.GRANTED 
        }
    }
    
    /**
     * Get list of granted permissions from configured permissions
     */
    val grantedPermissions: List<String>
        get() = configuredPermissions.filter { 
            checkPermission(it) == PermissionStatus.GRANTED 
        }
    
    /**
     * Get list of denied permissions (not permanently) from configured permissions
     */
    val deniedPermissions: List<String>
        get() = configuredPermissions.filter { 
            checkPermission(it) == PermissionStatus.DENIED 
        }
    
    /**
     * Get list of permanently denied permissions from configured permissions
     */
    val permanentlyDeniedPermissions: List<String>
        get() = configuredPermissions.filter { 
            checkPermission(it) == PermissionStatus.PERMANENTLY_DENIED 
        }
    
    /**
     * Request a single permission
     * 
     * @param permission The permission to request
     * @param callback Callback invoked with the result
     * @throws PermissionException if no activity is available
     */
    fun requestPermission(permission: String, callback: (PermissionResult) -> Unit) {
        // Check if already granted
        if (checkPermission(permission) == PermissionStatus.GRANTED) {
            callback(PermissionResult(permission, PermissionStatus.GRANTED, false))
            return
        }
        
        // Ensure we have an active activity
        val activity = currentActivityRef?.get() 
            ?: throw PermissionException(PermissionException.ERROR_ACTIVITY_DESTROYED)
        
        // Store callback and mark permission as requested
        singlePermissionCallback = callback
        requestedPermissions.add(permission)
        
        // Launch permission request
        singlePermissionLauncher?.launch(permission)
    }
    
    /**
     * Request multiple permissions
     * 
     * @param permissions Vararg of permissions to request
     * @param callback Callback invoked with map of results
     * @throws PermissionException if no activity is available
     */
    fun requestPermissions(vararg permissions: String, callback: (Map<String, PermissionResult>) -> Unit) {
        // Filter out already granted permissions
        val permissionsToRequest = permissions.filter { 
            checkPermission(it) != PermissionStatus.GRANTED 
        }
        
        // If all already granted, return immediately
        if (permissionsToRequest.isEmpty()) {
            val results = permissions.associateWith { 
                PermissionResult(it, PermissionStatus.GRANTED, false) 
            }
            callback(results)
            return
        }
        
        // Ensure we have an active activity
        val activity = currentActivityRef?.get() 
            ?: throw PermissionException(PermissionException.ERROR_ACTIVITY_DESTROYED)
        
        // Store callback and mark permissions as requested
        multiplePermissionCallback = callback
        requestedPermissions.addAll(permissions)
        
        // Launch permission request
        multiplePermissionLauncher?.launch(permissionsToRequest.toTypedArray())
    }
    
    /**
     * Request all configured permissions
     * 
     * @param callback Callback invoked with map of results
     */
    fun requestAllConfigured(callback: (Map<String, PermissionResult>) -> Unit) {
        requestPermissions(*configuredPermissions.toTypedArray(), callback = callback)
    }
    
    /**
     * Check if should show rationale for a permission
     * 
     * @param permission The permission to check
     * @return true if rationale should be shown
     */
    fun shouldShowRationale(permission: String): Boolean {
        val activity = currentActivityRef?.get() ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    /**
     * Open app settings page for manual permission grant
     * This is useful when permissions are permanently denied
     */
    fun openAppSettings() {
        val activity = currentActivityRef?.get() ?: application
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", application.packageName, null)
            if (activity === application) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        activity.startActivity(intent)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: PermissionManager? = null
        
        /**
         * Initialize the PermissionManager
         * 
         * Call this once in your Application class's onCreate()
         * 
         * @param application Application instance
         * @param permissions All permissions your app needs
         */
        @JvmStatic
        fun init(application: Application, vararg permissions: String) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = PermissionManager(application, permissions.toList())
                    }
                }
            }
        }
        
        /**
         * Get the singleton instance
         * 
         * @throws PermissionException if not initialized
         */
        @JvmStatic
        val instance: PermissionManager
            get() = INSTANCE 
                ?: throw PermissionException(PermissionException.ERROR_NOT_INITIALIZED)
    }
}
