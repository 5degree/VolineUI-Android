@file:Suppress("unused")

package `in`.fivedegree.volineui.filepickermanager

/**
 * Configuration for file pick, validation, and storage behavior.
 *
 * @property allowedMimeTypes MIME types accepted by the system picker and post-validation.
 *   Supports wildcards (e.g. image slash-star, any-type). Empty list is treated as any type.
 * @property allowedExtensions Optional extension whitelist (e.g. `"pdf"`, `".csv"`).
 *   Empty = no extension filter. Matched case-insensitively without requiring a leading dot.
 * @property maxFileSizeBytes Maximum allowed size per file (inclusive). Default 10 MB.
 * @property minFileSizeBytes Minimum allowed size per file (inclusive). Default 1 byte.
 * @property allowMultiple Whether the system picker allows multi-select.
 * @property maxFileCount Absolute max files in the selection (including already-held files).
 * @property copyToAppStorage If true, copy content into app cache/files for stable access.
 *   If false, keep the SAF Uri (and try to take persistable read permission).
 * @property storageSubfolder Subfolder under cache (or files) when copying.
 * @property takePersistablePermission When not copying, attempt persistable URI permission.
 * @property minPdfPageCount Optional minimum PDF page count (inclusive).
 * @property maxPdfPageCount Optional maximum PDF page count (inclusive).
 * @property minImageWidth Optional minimum image width in px.
 * @property minImageHeight Optional minimum image height in px.
 * @property maxImageWidth Optional maximum image width in px.
 * @property maxImageHeight Optional maximum image height in px.
 * @property rejectDuplicates Skip Uris that match an already-selected Uri string.
 * @property existingUris Uris already held by the UI; used for duplicate + remaining-slot checks.
 */
data class FilePickerConfig(
    val allowedMimeTypes: List<String> = listOf("*/*"),
    val allowedExtensions: List<String> = emptyList(),
    val maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
    val minFileSizeBytes: Long = 1L,
    val allowMultiple: Boolean = false,
    val maxFileCount: Int = if (allowMultiple) DEFAULT_MAX_FILE_COUNT else 1,
    val copyToAppStorage: Boolean = true,
    val storageSubfolder: String = "picked_files",
    val takePersistablePermission: Boolean = true,
    val minPdfPageCount: Int? = null,
    val maxPdfPageCount: Int? = null,
    val minImageWidth: Int? = null,
    val minImageHeight: Int? = null,
    val maxImageWidth: Int? = null,
    val maxImageHeight: Int? = null,
    val rejectDuplicates: Boolean = true,
    val existingUris: List<String> = emptyList(),
) {
    init {
        require(maxFileSizeBytes > 0) { "maxFileSizeBytes must be > 0" }
        require(minFileSizeBytes >= 0) { "minFileSizeBytes must be >= 0" }
        require(minFileSizeBytes <= maxFileSizeBytes) {
            "minFileSizeBytes must be <= maxFileSizeBytes"
        }
        require(maxFileCount > 0) { "maxFileCount must be > 0" }
        if (!allowMultiple) {
            require(maxFileCount == 1) {
                "maxFileCount must be 1 when allowMultiple is false"
            }
        }
        minPdfPageCount?.let { require(it > 0) { "minPdfPageCount must be > 0" } }
        maxPdfPageCount?.let { require(it > 0) { "maxPdfPageCount must be > 0" } }
        if (minPdfPageCount != null && maxPdfPageCount != null) {
            require(minPdfPageCount <= maxPdfPageCount) {
                "minPdfPageCount must be <= maxPdfPageCount"
            }
        }
        listOf(minImageWidth, minImageHeight, maxImageWidth, maxImageHeight).forEach { dim ->
            dim?.let { require(it > 0) { "Image dimension limits must be > 0" } }
        }
        if (minImageWidth != null && maxImageWidth != null) {
            require(minImageWidth <= maxImageWidth) { "minImageWidth must be <= maxImageWidth" }
        }
        if (minImageHeight != null && maxImageHeight != null) {
            require(minImageHeight <= maxImageHeight) { "minImageHeight must be <= maxImageHeight" }
        }
        require(isSafeStorageSubfolder(storageSubfolder)) {
            "storageSubfolder must be a simple name (letters, digits, . _ -) without path separators"
        }
    }

    /** Normalized MIME list for the system picker (never empty). */
    fun mimeTypesForPicker(): Array<String> {
        val types = allowedMimeTypes
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("*/*") }
        return types.toTypedArray()
    }

    /** Normalized extensions without leading dots, lowercase. */
    fun normalizedExtensions(): Set<String> =
        allowedExtensions
            .map { it.trim().lowercase().removePrefix(".") }
            .filter { it.isNotEmpty() }
            .toSet()

    fun remainingSlots(existingCount: Int = existingUris.size): Int =
        (maxFileCount - existingCount).coerceAtLeast(0)

    companion object {
        const val DEFAULT_MAX_FILE_SIZE_BYTES: Long = 10L * 1024L * 1024L // 10 MB
        const val DEFAULT_MAX_FILE_COUNT: Int = 10
        private val SAFE_SUBFOLDER = Regex("^[A-Za-z0-9._-]+$")

        private fun isSafeStorageSubfolder(value: String): Boolean {
            val trimmed = value.trim()
            return trimmed.isNotEmpty() &&
                trimmed != "." &&
                !trimmed.contains("..") &&
                !trimmed.contains('/') &&
                !trimmed.contains('\\') &&
                SAFE_SUBFOLDER.matches(trimmed)
        }

        /** Common PDF-only preset. */
        fun pdf(
            maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
            allowMultiple: Boolean = false,
            maxFileCount: Int = if (allowMultiple) DEFAULT_MAX_FILE_COUNT else 1,
            minPdfPageCount: Int? = null,
            maxPdfPageCount: Int? = null,
        ) = FilePickerConfig(
            allowedMimeTypes = listOf("application/pdf"),
            allowedExtensions = listOf("pdf"),
            maxFileSizeBytes = maxFileSizeBytes,
            allowMultiple = allowMultiple,
            maxFileCount = maxFileCount,
            minPdfPageCount = minPdfPageCount,
            maxPdfPageCount = maxPdfPageCount,
        )

        /** Common CSV/spreadsheet preset. */
        fun csv(
            maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
            allowMultiple: Boolean = false,
            maxFileCount: Int = if (allowMultiple) DEFAULT_MAX_FILE_COUNT else 1,
        ) = FilePickerConfig(
            allowedMimeTypes = listOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/vnd.ms-excel",
                "text/plain",
            ),
            allowedExtensions = listOf("csv"),
            maxFileSizeBytes = maxFileSizeBytes,
            allowMultiple = allowMultiple,
            maxFileCount = maxFileCount,
        )

        /** Images only. */
        fun images(
            maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
            allowMultiple: Boolean = false,
            maxFileCount: Int = if (allowMultiple) DEFAULT_MAX_FILE_COUNT else 1,
            maxImageWidth: Int? = null,
            maxImageHeight: Int? = null,
        ) = FilePickerConfig(
            allowedMimeTypes = listOf("image/*"),
            allowedExtensions = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic"),
            maxFileSizeBytes = maxFileSizeBytes,
            allowMultiple = allowMultiple,
            maxFileCount = maxFileCount,
            maxImageWidth = maxImageWidth,
            maxImageHeight = maxImageHeight,
        )
    }
}
