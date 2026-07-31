@file:Suppress("unused")

package `in`.fivedegree.volineui.filepickermanager

/**
 * Outcome of a file pick / validation / open operation.
 */
sealed class FilePickerResult {

    /**
     * One or more files passed validation.
     *
     * @property files Accepted files (may be a subset if some failed and were reported via [Error])
     * @property rejectedCount How many selected items were rejected
     */
    data class Success(
        val files: List<PickedFile>,
        val rejectedCount: Int = 0,
    ) : FilePickerResult() {
        val firstOrNull: PickedFile? get() = files.firstOrNull()
        val isEmpty: Boolean get() = files.isEmpty()
    }

    /**
     * A validation or operational failure for a specific selection item (or the whole flow).
     *
     * @property type Machine-readable error for host handling
     * @property message Human-readable detail
     * @property fileName Optional display name of the offending file
     * @property uri Optional Uri of the offending file
     */
    data class Error(
        val type: FilePickerErrorType,
        val message: String,
        val fileName: String? = null,
        val uri: android.net.Uri? = null,
    ) : FilePickerResult()

    /** User dismissed the system picker without selecting. */
    data object Cancelled : FilePickerResult()

    /** Validation / copy is running on a background thread. */
    data object Processing : FilePickerResult()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isCancelled: Boolean get() = this is Cancelled
    val isProcessing: Boolean get() = this is Processing
}
