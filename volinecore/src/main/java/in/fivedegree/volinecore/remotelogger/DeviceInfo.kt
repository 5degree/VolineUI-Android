@file:Suppress("unused")

package `in`.fivedegree.volinecore.remotelogger

/**
 * Immutable snapshot of the device and app environment.
 *
 * Collected once on [VolineLogger.init] by [DeviceInfoCollector] and attached
 * to every [LogEntry]. The [networkType] field is refreshed per log call since
 * connectivity can change frequently.
 *
 * @property manufacturer Device manufacturer (e.g., "Samsung").
 * @property model Device model (e.g., "SM-G991B").
 * @property brand Device brand (e.g., "samsung").
 * @property osVersion Android version string (e.g., "14").
 * @property sdkVersion Android SDK int (e.g., 34).
 * @property deviceId Unique device identifier from [android.provider.Settings.Secure.ANDROID_ID].
 * @property appVersionName Application version name from PackageInfo.
 * @property appVersionCode Application version code from PackageInfo.
 * @property screenDensity Display density bucket (e.g., "xxhdpi").
 * @property screenResolution Screen resolution string (e.g., "1080x2400").
 * @property locale Current device locale (e.g., "en_IN").
 * @property timezone Current timezone ID (e.g., "Asia/Kolkata").
 * @property networkType Current network type — refreshed per log call.
 * @property isEmulator Whether the device appears to be an emulator.
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val osVersion: String,
    val sdkVersion: Int,
    val deviceId: String,
    val appVersionName: String,
    val appVersionCode: Long,
    val screenDensity: String,
    val screenResolution: String,
    val locale: String,
    val timezone: String,
    val networkType: String,
    val isEmulator: Boolean
)
