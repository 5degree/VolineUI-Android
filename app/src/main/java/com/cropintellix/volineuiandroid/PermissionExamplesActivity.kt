package com.cropintellix.volineuiandroid

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cropintellix.volineui.PermissionManager
import com.cropintellix.volineui.permissionmanager.PermissionStatus
import com.cropintellix.volineuiandroid.databinding.ActivityPermissionExamplesBinding

/**
 * Permission Examples Activity - Demonstrates all features of PermissionManager
 * 
 * Features demonstrated:
 * 1. Single permission request (CAMERA)
 * 2. Multiple permission requests (CAMERA + LOCATION + STORAGE)
 * 3. Permission status checking
 * 4. Handling permanently denied permissions with settings redirect
 * 5. Checking all configured permissions
 * 6. Viewing granted/denied permission lists
 */
class PermissionExamplesActivity : AppCompatActivity() {

    private var binding: ActivityPermissionExamplesBinding? = null
    private val b get() = binding!!
    
    // Permission Manager instance (no initialization needed!)
    private val permissionManager get() = PermissionManager.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPermissionExamplesBinding.inflate(layoutInflater)
        setContentView(b.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupUI()
        updatePermissionStatus()
    }
    
    private fun setupUI() {
        // Example 1: Request single permission (Camera)
        b.btnRequestCamera.setOnClickListener {
            requestCameraPermission()
        }
        
        // Example 2: Request multiple permissions
        b.btnRequestMultiple.setOnClickListener {
            requestMultiplePermissions()
        }
        
        // Example 3: Request all configured permissions
        b.btnRequestAll.setOnClickListener {
            requestAllPermissions()
        }
        
        // Example 4: Check permission status
        b.btnCheckStatus.setOnClickListener {
            checkPermissionStatus()
        }
        
        // Example 5: View granted permissions
        b.btnViewGranted.setOnClickListener {
            viewGrantedPermissions()
        }
        
        // Example 6: View denied permissions
        b.btnViewDenied.setOnClickListener {
            viewDeniedPermissions()
        }
        
        // Example 7: Open app settings
        b.btnOpenSettings.setOnClickListener {
            permissionManager.openAppSettings()
            showToast("Opening app settings...")
        }
        
        // Example 8: Refresh status display
        b.btnRefresh.setOnClickListener {
            updatePermissionStatus()
            showToast("Permission status refreshed")
        }
    }
    
    /**
     * Example 1: Request single permission with proper error handling
     */
    private fun requestCameraPermission() {
        permissionManager.requestPermission(Manifest.permission.CAMERA) { result ->
            when {
                result.isGranted -> {
                    showToast("✓ Camera permission granted!")
                    updatePermissionStatus()
                    // Now you can use the camera
                    b.tvStatus.append("\n\n✓ You can now use the camera")
                }
                result.isPermanentlyDenied -> {
                    showPermanentlyDeniedDialog("Camera")
                }
                else -> {
                    if (result.shouldShowRationale) {
                        showRationaleDialog("Camera", 
                            "Camera permission is needed to take photos and scan documents.")
                    } else {
                        showToast("✗ Camera permission denied")
                    }
                    updatePermissionStatus()
                }
            }
        }
    }
    
    /**
     * Example 2: Request multiple permissions at once
     */
    private fun requestMultiplePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        
        permissionManager.requestPermissions(*permissions) { results ->
            val granted = results.filter { it.value.isGranted }.keys
            val denied = results.filter { !it.value.isGranted }.keys
            
            val message = buildString {
                if (granted.isNotEmpty()) {
                    append("✓ Granted: ${granted.joinToString { it.split(".").last() }}\n")
                }
                if (denied.isNotEmpty()) {
                    append("✗ Denied: ${denied.joinToString { it.split(".").last() }}")
                }
            }
            
            showToast(message)
            updatePermissionStatus()
            
            // Check if any are permanently denied
            val permanentlyDenied = results.filter { it.value.isPermanentlyDenied }
            if (permanentlyDenied.isNotEmpty()) {
                showPermanentlyDeniedDialog(
                    permanentlyDenied.keys.joinToString { it.split(".").last() }
                )
            }
        }

        permissionManager.checkPermission(Manifest.permission.CAMERA) == PermissionStatus.GRANTED
    }
    
    /**
     * Example 3: Request all configured permissions
     */
    private fun requestAllPermissions() {
        permissionManager.requestAllConfigured { results ->
            val totalCount = results.size
            val grantedCount = results.count { it.value.isGranted }
            
            showToast("$grantedCount/$totalCount permissions granted")
            updatePermissionStatus()
        }
    }
    
    /**
     * Example 4: Check and display permission statuses
     */
    private fun checkPermissionStatus() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        
        val statuses = permissionManager.checkPermissions(*permissions)
        
        val statusText = buildString {
            append("Permission Status Check:\n\n")
            statuses.forEach { (permission, status) ->
                val permName = permission.split(".").last()
                val statusIcon = when (status) {
                    PermissionStatus.GRANTED -> "✓"
                    PermissionStatus.DENIED -> "✗"
                    PermissionStatus.PERMANENTLY_DENIED -> "⊗"
                    PermissionStatus.NOT_REQUESTED -> "?"
                }
                append("$statusIcon $permName: ${status.name}\n")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Permission Status")
            .setMessage(statusText)
            .setPositiveButton("OK", null)
            .show()
    }
    
    /**
     * Example 5: View all granted permissions
     */
    private fun viewGrantedPermissions() {
        val granted = permissionManager.grantedPermissions
        if (granted.isEmpty()) {
            showToast("No permissions granted yet")
        } else {
            val message = "Granted Permissions:\n\n" + 
                granted.joinToString("\n") { "✓ ${it.split(".").last()}" }
            
            AlertDialog.Builder(this)
                .setTitle("Granted Permissions")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    /**
     * Example 6: View all denied permissions
     */
    private fun viewDeniedPermissions() {
        val denied = permissionManager.deniedPermissions
        val permanentlyDenied = permissionManager.permanentlyDeniedPermissions
        
        if (denied.isEmpty() && permanentlyDenied.isEmpty()) {
            showToast("No permissions denied")
        } else {
            val message = buildString {
                if (denied.isNotEmpty()) {
                    append("Denied (can retry):\n")
                    denied.forEach { append("✗ ${it.split(".").last()}\n") }
                    append("\n")
                }
                if (permanentlyDenied.isNotEmpty()) {
                    append("Permanently Denied:\n")
                    permanentlyDenied.forEach { append("⊗ ${it.split(".").last()}\n") }
                }
            }
            
            AlertDialog.Builder(this)
                .setTitle("Denied Permissions")
                .setMessage(message.trim())
                .setPositiveButton("OK", null)
                .setNegativeButton("Open Settings") { _, _ ->
                    permissionManager.openAppSettings()
                }
                .show()
        }
    }
    
    /**
     * Update the status display with current permission information
     */
    private fun updatePermissionStatus() {
        val hasAll = permissionManager.hasAllRequiredPermissions()
        val grantedCount = permissionManager.grantedPermissions.size
        val deniedCount = permissionManager.deniedPermissions.size
        val permDeniedCount = permissionManager.permanentlyDeniedPermissions.size
        
        val statusText = buildString {
            append("📊 Current Status:\n\n")
            append(if (hasAll) "✓ All required permissions granted\n\n" 
                  else "⚠ Some permissions missing\n\n")
            append("✓ Granted: $grantedCount\n")
            append("✗ Denied: $deniedCount\n")
            append("⊗ Permanently Denied: $permDeniedCount\n")
        }
        
        b.tvStatus.text = statusText
    }
    
    /**
     * Show rationale dialog explaining why permission is needed
     */
    private fun showRationaleDialog(permissionName: String, reason: String) {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("$permissionName permission is needed.\n\n$reason")
            .setPositiveButton("Grant") { _, _ ->
                // User can try again after reading rationale
                when (permissionName) {
                    "Camera" -> requestCameraPermission()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Show dialog for permanently denied permissions
     */
    private fun showPermanentlyDeniedDialog(permissionName: String) {
        AlertDialog.Builder(this)
            .setTitle("Permission Permanently Denied")
            .setMessage("$permissionName permission is required for this feature.\n\n" +
                       "You have selected \"Don't ask again\". Please grant permission manually in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                permissionManager.openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        updatePermissionStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
