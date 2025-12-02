package com.cropintellix.volineuiandroid

import android.Manifest
import android.app.Application
import android.os.Build
import com.cropintellix.volineui.PermissionManager

/**
 * Application class for VolineUI Android Demo
 * 
 * This is where we initialize the PermissionManager once for the entire app.
 * After this initialization, all activities can use PermissionManager.instance
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
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        PermissionManager.init(this, *permissions)
    }
}
