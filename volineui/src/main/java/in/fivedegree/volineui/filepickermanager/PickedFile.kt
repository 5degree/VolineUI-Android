@file:Suppress("unused")

package `in`.fivedegree.volineui.filepickermanager

import android.net.Uri
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * A successfully validated (and optionally copied) picked file.
 *
 * @property id Stable id for Compose keys / list ops
 * @property uri Content Uri from SAF (always set)
 * @property displayName User-visible name
 * @property mimeType Resolved MIME type when available
 * @property sizeBytes Size in bytes (0 if unknown before copy)
 * @property extension Lowercase extension without dot
 * @property localFile Local copy when [FilePickerConfig.copyToAppStorage] was true
 * @property pageCount PDF page count when applicable
 * @property imageWidth Image width when applicable
 * @property imageHeight Image height when applicable
 */
data class PickedFile(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val extension: String,
    val localFile: File? = null,
    val pageCount: Int? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
) {
    val sizeDisplay: String
        get() = formatFileSize(sizeBytes)

    val isPdf: Boolean
        get() = extension == "pdf" || mimeType.equals("application/pdf", ignoreCase = true)

    val isImage: Boolean
        get() = mimeType?.startsWith("image/", ignoreCase = true) == true ||
            extension in IMAGE_EXTENSIONS

    val isCsv: Boolean
        get() = extension == "csv" ||
            mimeType?.contains("csv", ignoreCase = true) == true

    /**
     * Uri suitable for content access. Prefer [FilePickerManager.openFile] for viewing,
     * which uses FileProvider for local copies (never expose raw file:// URIs).
     */
    fun openableUri(): Uri = uri

    companion object {
        private val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif"
        )

        fun formatFileSize(bytes: Long): String {
            if (bytes < 0) return "—"
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb)
            val gb = mb / 1024.0
            return String.format(Locale.US, "%.2f GB", gb)
        }

        fun extensionFromName(name: String?): String {
            if (name.isNullOrBlank()) return ""
            val trimmed = name.trim()
            val dot = trimmed.lastIndexOf('.')
            if (dot < 0 || dot == trimmed.lastIndex) return ""
            return trimmed.substring(dot + 1).lowercase(Locale.US)
        }
    }
}
