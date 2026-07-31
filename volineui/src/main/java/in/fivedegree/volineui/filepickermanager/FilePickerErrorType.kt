@file:Suppress("unused")

package `in`.fivedegree.volineui.filepickermanager

/**
 * Typed errors for [FilePickerManager] / [AdvancedFilePicker] validation and pick flows.
 *
 * Use with [onError] callbacks so the host app can decide toast/dialog/logging.
 */
enum class FilePickerErrorType {
    /** [FilePickerManager.init] was never called. */
    NOT_INITIALIZED,

    /** No foreground [androidx.activity.ComponentActivity] available. */
    ACTIVITY_DESTROYED,

    /** A pick operation is already in progress. */
    ALREADY_IN_PROGRESS,

    /** Selected MIME type is not allowed by config. */
    INVALID_TYPE,

    /** File extension is not allowed by config. */
    INVALID_EXTENSION,

    /** File exceeds [FilePickerConfig.maxFileSizeBytes]. */
    FILE_TOO_LARGE,

    /** File is below [FilePickerConfig.minFileSizeBytes]. */
    FILE_TOO_SMALL,

    /** Empty / zero-byte file. */
    FILE_EMPTY,

    /** Selection would exceed [FilePickerConfig.maxFileCount]. */
    MAX_COUNT_EXCEEDED,

    /** PDF page count outside configured min/max. */
    PDF_PAGE_COUNT_INVALID,

    /** Image width/height outside configured bounds. */
    IMAGE_DIMENSION_INVALID,

    /** Could not read metadata or content from the Uri. */
    READ_FAILED,

    /** Failed to copy Uri into app storage. */
    COPY_FAILED,

    /** Local file missing or inaccessible. */
    FILE_NOT_FOUND,

    /** No installed app can open this file type. */
    NO_APP_TO_OPEN,

    /** Failed to launch the default viewer. */
    OPEN_FAILED,

    /** Catch-all for unexpected failures. */
    UNKNOWN,
}
