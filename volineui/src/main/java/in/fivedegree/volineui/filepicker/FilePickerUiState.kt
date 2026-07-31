@file:Suppress("unused")

package `in`.fivedegree.volineui.filepicker

/**
 * Visual / logical state of [in.fivedegree.volineui.compose.AdvancedFilePicker].
 */
enum class FilePickerUiState {
    /** No files selected — showing placeholder. */
    EMPTY,

    /** Pick / validation / copy in progress. */
    LOADING,

    /** One or more files ready. */
    LOADED,

    /** Last operation failed (placeholder may show error styling). */
    ERROR,
}
