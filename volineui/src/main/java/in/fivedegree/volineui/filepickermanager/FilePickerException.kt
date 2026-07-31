@file:Suppress("unused")

package `in`.fivedegree.volineui.filepickermanager

/**
 * Exception for file-picker initialization and hard failures.
 */
class FilePickerException(
    message: String,
    val errorType: FilePickerErrorType = FilePickerErrorType.UNKNOWN,
) : Exception(message) {

    companion object {
        const val ERROR_NOT_INITIALIZED =
            "FilePickerManager not initialized. Call FilePickerManager.init() in your Application class."

        const val ERROR_ACTIVITY_DESTROYED =
            "Cannot pick file - no active activity available. Make sure an activity is in the foreground."

        const val ERROR_ALREADY_IN_PROGRESS =
            "A file pick operation is already in progress."
    }
}
