package `in`.fivedegree.volinecore.remotelogger

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import `in`.fivedegree.volinecore.remotelogger.DeviceInfoCollector.collect
import `in`.fivedegree.volinecore.remotelogger.DeviceInfoCollector.getCurrentNetworkType
import java.util.Locale
import java.util.TimeZone

/**
 * Collects device and application metadata.
 *
 * Most fields are captured once on initialization and cached.
 * [getCurrentNetworkType] is called per log entry since connectivity changes frequently.
 */
internal object DeviceInfoCollector {

    private lateinit var application: Application
    private var cachedDeviceInfo: DeviceInfo? = null

    /**
     * Initialize with the application context. Must be called before [collect].
     */
    fun init(application: Application) {
        this.application = application
    }

    /**
     * Collects a [DeviceInfo] snapshot.
     *
     * On the first call, all fields are gathered from system APIs and cached.
     * Subsequent calls return the cached copy with an updated [DeviceInfo.networkType].
     */
    fun collect(): DeviceInfo {
        val cached = cachedDeviceInfo
        if (cached != null) {
            // Only refresh the network type on each call
            return cached.copy(networkType = getCurrentNetworkType())
        }

        val info = DeviceInfo(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            osVersion = Build.VERSION.RELEASE.orEmpty(),
            sdkVersion = Build.VERSION.SDK_INT,
            deviceId = getDeviceId(),
            appVersionName = getAppVersionName(),
            appVersionCode = getAppVersionCode(),
            screenDensity = getScreenDensityBucket(),
            screenResolution = getScreenResolution(),
            locale = Locale.getDefault().toString(),
            timezone = TimeZone.getDefault().id,
            networkType = getCurrentNetworkType(),
            isEmulator = detectEmulator()
        )
        cachedDeviceInfo = info
        return info
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String =
        try {
            Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }

    private fun getAppVersionName(): String =
        try {
            getPackageInfo().versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }

    private fun getAppVersionCode(): Long =
        try {
            val info = getPackageInfo()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (_: Exception) {
            -1L
        }

    private fun getPackageInfo(): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.packageManager.getPackageInfo(
                application.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            application.packageManager.getPackageInfo(application.packageName, 0)
        }

    @Suppress("DEPRECATION")
    private fun getScreenDensityBucket(): String {
        val densityDpi = application.resources.displayMetrics.densityDpi
        return when {
            densityDpi <= DisplayMetrics.DENSITY_LOW -> "ldpi"
            densityDpi <= DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
            densityDpi <= DisplayMetrics.DENSITY_HIGH -> "hdpi"
            densityDpi <= DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
            densityDpi <= DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
            else -> "xxxhdpi"
        }
    }

    @Suppress("DEPRECATION")
    private fun getScreenResolution(): String =
        try {
            val wm = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.currentWindowMetrics.bounds
                "${bounds.width()}x${bounds.height()}"
            } else {
                val display = wm.defaultDisplay
                val metrics = DisplayMetrics()
                display.getRealMetrics(metrics)
                "${metrics.widthPixels}x${metrics.heightPixels}"
            }
        } catch (_: Exception) {
            "unknown"
        }

    /**
     * Returns the current network type: "WiFi", "Cellular", "Ethernet", "VPN", or "None".
     */
    fun getCurrentNetworkType(): String {
        val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "None"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "None"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }
    }

    /**
     * Heuristic emulator detection based on build properties.
     */
    private fun detectEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }
}
