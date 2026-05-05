package com.cropintellix.volineuiandroid

import android.Manifest
import android.app.Application
import android.os.Build
import com.cropintellix.volineui.LocationManager
import com.cropintellix.volineui.PermissionManager
import com.cropintellix.volineui.PhotoCaptureManager

/**
 * Application class for VolineUI Android Demo
 * 
 * This is where we initialize the managers once for the entire app.
 * After this initialization, all activities can use the manager instances
 * without any additional setup!
 */
class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PermissionManager with all permissions needed by the app
        // This is a ONE-TIME setup - no need to do this in every activity!
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
        
        // Initialize all managers
        PermissionManager.init(this, *permissions)
        LocationManager.init(this)
        PhotoCaptureManager.init(this)
    }
}
