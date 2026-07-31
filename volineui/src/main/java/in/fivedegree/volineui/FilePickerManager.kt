@file:Suppress("unused")

package `in`.fivedegree.volineui

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import `in`.fivedegree.volineui.filepickermanager.FilePickerConfig
import `in`.fivedegree.volineui.filepickermanager.FilePickerErrorType
import `in`.fivedegree.volineui.filepickermanager.FilePickerException
import `in`.fivedegree.volineui.filepickermanager.FilePickerResult
import `in`.fivedegree.volineui.filepickermanager.PickedFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * FilePickerManager — reusable SAF document picker with validation and optional local copy.
 *
 * Features:
 * - Single / multi document pick via Activity Result API
 * - MIME + extension + size validation (enforced while streaming, not only metadata)
 * - Optional PDF page-count and image dimension checks
 * - Optional copy into app cache for stable paths (path-traversal hardened)
 * - Open in default app (FileProvider-aware)
 * - Automatic activity lifecycle tracking (same pattern as [PhotoCaptureManager])
 *
 * Usage:
 * ```kotlin
 * // Application.onCreate
 * FilePickerManager.init(this)
 *
 * FilePickerManager.instance.pickFiles(
 *     FilePickerConfig.pdf(maxFileSizeBytes = 5_000_000)
 * ) { result ->
 *     when (result) {
 *         is FilePickerResult.Processing -> { }
 *         is FilePickerResult.Success -> { }
 *         is FilePickerResult.Error -> { }
 *         is FilePickerResult.Cancelled -> { }
 *     }
 * }
 * ```
 */
class FilePickerManager private constructor(
    private val application: Application,
) {

    private var currentActivityRef: WeakReference<ComponentActivity>? = null

    private var singleLauncher: ActivityResultLauncher<Array<String>>? = null
    private var multiLauncher: ActivityResultLauncher<Array<String>>? = null

    private var currentCallback: ((FilePickerResult) -> Unit)? = null
    private var currentConfig: FilePickerConfig? = null
    private var currentExistingCount: Int = 0
    private val isBusy = AtomicBoolean(false)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread(r, "FilePickerManager-Worker").apply { isDaemon = true }
    }

    init {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                    registerLaunchers(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (activity is ComponentActivity) {
                    currentActivityRef = WeakReference(activity)
                }
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivityRef?.get() === activity) {
                    currentActivityRef = null
                    singleLauncher = null
                    multiLauncher = null
                    // Avoid leaving callers stuck in LOADING if the host activity dies mid-pick.
                    if (isBusy.getAndSet(false)) {
                        val cb = currentCallback
                        currentCallback = null
                        currentConfig = null
                        currentExistingCount = 0
                        mainHandler.post {
                            cb?.invoke(
                                FilePickerResult.Error(
                                    FilePickerErrorType.ACTIVITY_DESTROYED,
                                    FilePickerException.ERROR_ACTIVITY_DESTROYED,
                                )
                            )
                        }
                    }
                }
            }
        })
    }

    private fun registerLaunchers(activity: ComponentActivity) {
        singleLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            handlePickerUris(if (uri != null) listOf(uri) else emptyList())
        }

        multiLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            handlePickerUris(uris.orEmpty())
        }
    }

    /**
     * Launch the system document picker.
     *
     * @param config Pick + validation rules
     * @param existingFileCount Files already held by the UI (counts against [FilePickerConfig.maxFileCount])
     * @param callback Invoked on main thread for Processing / Success / Error / Cancelled
     */
    fun pickFiles(
        config: FilePickerConfig = FilePickerConfig(),
        existingFileCount: Int = config.existingUris.size,
        callback: (FilePickerResult) -> Unit,
    ) {
        currentActivityRef?.get()
            ?: throw FilePickerException(
                FilePickerException.ERROR_ACTIVITY_DESTROYED,
                FilePickerErrorType.ACTIVITY_DESTROYED,
            )

        if (!isBusy.compareAndSet(false, true)) {
            callback(
                FilePickerResult.Error(
                    FilePickerErrorType.ALREADY_IN_PROGRESS,
                    FilePickerException.ERROR_ALREADY_IN_PROGRESS,
                )
            )
            return
        }

        val remaining = config.remainingSlots(existingFileCount)
        if (remaining <= 0) {
            isBusy.set(false)
            callback(
                FilePickerResult.Error(
                    FilePickerErrorType.MAX_COUNT_EXCEEDED,
                    "Maximum of ${config.maxFileCount} file(s) already selected.",
                )
            )
            return
        }

        currentCallback = callback
        currentConfig = config
        currentExistingCount = existingFileCount.coerceAtLeast(0)

        val mimeTypes = config.mimeTypesForPicker()
        try {
            if (config.allowMultiple && remaining > 1) {
                multiLauncher?.launch(mimeTypes)
                    ?: throw FilePickerException(
                        FilePickerException.ERROR_ACTIVITY_DESTROYED,
                        FilePickerErrorType.ACTIVITY_DESTROYED,
                    )
            } else {
                singleLauncher?.launch(mimeTypes)
                    ?: throw FilePickerException(
                        FilePickerException.ERROR_ACTIVITY_DESTROYED,
                        FilePickerErrorType.ACTIVITY_DESTROYED,
                    )
            }
        } catch (e: FilePickerException) {
            resetSession()
            throw e
        } catch (e: Exception) {
            resetSession()
            callback(
                FilePickerResult.Error(
                    FilePickerErrorType.UNKNOWN,
                    e.message ?: "Failed to launch file picker",
                )
            )
        }
    }

    /**
     * Convenience single-file pick (forces allowMultiple = false).
     */
    fun pickFile(
        config: FilePickerConfig = FilePickerConfig(),
        callback: (FilePickerResult) -> Unit,
    ) {
        pickFiles(
            config = config.copy(allowMultiple = false, maxFileCount = 1),
            existingFileCount = 0,
            callback = callback,
        )
    }

    /**
     * Open a picked file in the default installed app.
     */
    fun openFile(context: Context, file: PickedFile): FilePickerResult {
        return try {
            val local = file.localFile
            if (local != null && !local.exists()) {
                return FilePickerResult.Error(
                    FilePickerErrorType.FILE_NOT_FOUND,
                    "Local file no longer exists: ${file.displayName}",
                    file.displayName,
                    file.uri,
                )
            }

            val uri = resolveShareableUri(context, file)
            if (uri.scheme != ContentResolver.SCHEME_CONTENT &&
                uri.scheme != ContentResolver.SCHEME_FILE
            ) {
                return FilePickerResult.Error(
                    FilePickerErrorType.OPEN_FAILED,
                    "Unsupported URI scheme for opening.",
                    file.displayName,
                    file.uri,
                )
            }

            val mime = resolveOpenMimeType(context, file, uri)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(context.contentResolver, file.displayName, uri)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            val chooser = Intent.createChooser(viewIntent, "Open with").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
            FilePickerResult.Success(listOf(file))
        } catch (_: ActivityNotFoundException) {
            FilePickerResult.Error(
                FilePickerErrorType.NO_APP_TO_OPEN,
                "No app found to open ${file.displayName}",
                file.displayName,
                file.uri,
            )
        } catch (e: Exception) {
            FilePickerResult.Error(
                FilePickerErrorType.OPEN_FAILED,
                e.message ?: "Failed to open file",
                file.displayName,
                file.uri,
            )
        }
    }

    /**
     * Validate (and optionally copy) an existing Uri without launching the picker.
     * Useful for restoring from saved state.
     */
    fun validateUri(
        uri: Uri,
        config: FilePickerConfig = FilePickerConfig(),
        callback: (FilePickerResult) -> Unit,
    ) {
        if (uri == Uri.EMPTY || uri.scheme.isNullOrBlank()) {
            callback(
                FilePickerResult.Error(
                    FilePickerErrorType.READ_FAILED,
                    "Invalid Uri.",
                )
            )
            return
        }
        if (!isBusy.compareAndSet(false, true)) {
            callback(
                FilePickerResult.Error(
                    FilePickerErrorType.ALREADY_IN_PROGRESS,
                    FilePickerException.ERROR_ALREADY_IN_PROGRESS,
                )
            )
            return
        }
        currentCallback = callback
        currentConfig = config
        currentExistingCount = 0
        deliver(FilePickerResult.Processing)
        processUris(listOf(uri), config, existingCount = 0)
    }

    private fun handlePickerUris(uris: List<Uri>) {
        val callback = currentCallback
        val config = currentConfig
        if (callback == null || config == null) {
            resetSession()
            return
        }

        if (uris.isEmpty()) {
            deliver(FilePickerResult.Cancelled)
            return
        }

        deliver(FilePickerResult.Processing)
        processUris(uris, config, existingCount = currentExistingCount)
    }

    private fun processUris(
        uris: List<Uri>,
        config: FilePickerConfig,
        existingCount: Int,
    ) {
        val activity = currentActivityRef?.get()
        val appContext = application.applicationContext

        worker.execute {
            val accepted = mutableListOf<PickedFile>()
            val errors = mutableListOf<FilePickerResult.Error>()
            var remaining = config.remainingSlots(existingCount)
            val seen = config.existingUris.toMutableSet()

            for (uri in uris) {
                if (remaining <= 0) {
                    errors += FilePickerResult.Error(
                        FilePickerErrorType.MAX_COUNT_EXCEEDED,
                        "Only ${config.maxFileCount} file(s) allowed.",
                        uriToDisplayName(appContext, uri),
                        uri,
                    )
                    continue
                }

                val uriKey = uri.toString()
                if (config.rejectDuplicates && !seen.add(uriKey)) {
                    continue
                }

                if (!config.copyToAppStorage && config.takePersistablePermission) {
                    tryTakePersistablePermission(activity, uri)
                }

                when (val outcome = validateAndBuild(appContext, uri, config)) {
                    is ValidateOk -> {
                        accepted += outcome.file
                        remaining--
                    }
                    is ValidateErr -> errors += outcome.error
                }
            }

            mainHandler.post {
                val cb = currentCallback
                errors.forEach { cb?.invoke(it) }

                when {
                    accepted.isNotEmpty() -> {
                        cb?.invoke(
                            FilePickerResult.Success(
                                files = accepted,
                                rejectedCount = errors.size,
                            )
                        )
                    }
                    errors.isNotEmpty() -> {
                        // Errors already delivered.
                    }
                    else -> cb?.invoke(FilePickerResult.Cancelled)
                }
                resetSession()
            }
        }
    }

    private sealed class ValidateOutcome
    private data class ValidateOk(val file: PickedFile) : ValidateOutcome()
    private data class ValidateErr(val error: FilePickerResult.Error) : ValidateOutcome()

    private fun validateAndBuild(
        context: Context,
        uri: Uri,
        config: FilePickerConfig,
    ): ValidateOutcome {
        val resolver = context.contentResolver
        val meta = queryMeta(resolver, uri)
        val displayName = sanitizeDisplayName(
            meta.displayName?.takeIf { it.isNotBlank() }
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: "file"
        )

        val extension = PickedFile.extensionFromName(displayName).ifEmpty {
            extensionFromMime(meta.mimeType)
        }

        val mimeType = meta.mimeType
            ?: mimeFromExtension(extension)
            ?: try {
                resolver.getType(uri)
            } catch (_: Exception) {
                null
            }

        val allowedExt = config.normalizedExtensions()
        val mimeRules = config.allowedMimeTypes
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("*/*") }
        val allowsAnyMime = mimeRules.any { it == "*/*" }

        if (mimeType.isNullOrBlank()) {
            if (!allowsAnyMime && allowedExt.isEmpty()) {
                return ValidateErr(
                    FilePickerResult.Error(
                        FilePickerErrorType.INVALID_TYPE,
                        "Could not determine file type.",
                        displayName,
                        uri,
                    )
                )
            }
        } else if (!mimeMatches(mimeType, mimeRules)) {
            return ValidateErr(
                FilePickerResult.Error(
                    FilePickerErrorType.INVALID_TYPE,
                    "File type '$mimeType' is not allowed.",
                    displayName,
                    uri,
                )
            )
        }

        if (allowedExt.isNotEmpty() && extension !in allowedExt) {
            return ValidateErr(
                FilePickerResult.Error(
                    FilePickerErrorType.INVALID_EXTENSION,
                    "Extension '.$extension' is not allowed.",
                    displayName,
                    uri,
                )
            )
        }

        var sizeBytes = meta.sizeBytes
        if (sizeBytes < 0) {
            // Cap streaming measurement to avoid unbounded reads of lying providers.
            sizeBytes = measureSize(resolver, uri, config.maxFileSizeBytes)
        }

        if (sizeBytes == SIZE_TOO_LARGE) {
            return ValidateErr(
                FilePickerResult.Error(
                    FilePickerErrorType.FILE_TOO_LARGE,
                    "File exceeds maximum ${PickedFile.formatFileSize(config.maxFileSizeBytes)}.",
                    displayName,
                    uri,
                )
            )
        }

        if (sizeBytes == 0L) {
            return ValidateErr(
                FilePickerResult.Error(
                    FilePickerErrorType.FILE_EMPTY,
                    "File is empty.",
                    displayName,
                    uri,
                )
            )
        }

        // Unknown size with no local copy would skip enforcement — refuse rather than accept.
        if (sizeBytes < 0) {
            if (!config.copyToAppStorage) {
                return ValidateErr(
                    FilePickerResult.Error(
                        FilePickerErrorType.READ_FAILED,
                        "Could not determine file size.",
                        displayName,
                        uri,
                    )
                )
            }
            // Will enforce during copy below.
        } else {
            if (sizeBytes < config.minFileSizeBytes) {
                return ValidateErr(
                    FilePickerResult.Error(
                        FilePickerErrorType.FILE_TOO_SMALL,
                        "File is too small (${PickedFile.formatFileSize(sizeBytes)}). " +
                            "Minimum is ${PickedFile.formatFileSize(config.minFileSizeBytes)}.",
                        displayName,
                        uri,
                    )
                )
            }
            if (sizeBytes > config.maxFileSizeBytes) {
                return ValidateErr(
                    FilePickerResult.Error(
                        FilePickerErrorType.FILE_TOO_LARGE,
                        "File is too large (${PickedFile.formatFileSize(sizeBytes)}). " +
                            "Maximum is ${PickedFile.formatFileSize(config.maxFileSizeBytes)}.",
                        displayName,
                        uri,
                    )
                )
            }
        }

        var pageCount: Int? = null
        var imageWidth: Int? = null
        var imageHeight: Int? = null

        val isPdf = extension == "pdf" || mimeType.equals("application/pdf", true)
        val needsPdfCheck = config.minPdfPageCount != null || config.maxPdfPageCount != null
        if (isPdf && needsPdfCheck) {
            pageCount = readPdfPageCount(context, uri)
            if (pageCount == null) {
                return ValidateErr(
                    FilePickerResult.Error(
                        FilePickerErrorType.READ_FAILED,
                        "Could not read PDF page count.",
                        displayName,
                        uri,
                    )
                )
            }
            pageCount?.let { pages ->
                config.minPdfPageCount?.let { min ->
                    if (pages < min) {
                        return ValidateErr(
                            FilePickerResult.Error(
                                FilePickerErrorType.PDF_PAGE_COUNT_INVALID,
                                "PDF has $pages page(s); minimum is $min.",
                                displayName,
                                uri,
                            )
                        )
                    }
                }
                config.maxPdfPageCount?.let { max ->
                    if (pages > max) {
                        return ValidateErr(
                            FilePickerResult.Error(
                                FilePickerErrorType.PDF_PAGE_COUNT_INVALID,
                                "PDF has $pages page(s); maximum is $max.",
                                displayName,
                                uri,
                            )
                        )
                    }
                }
            }
        } else if (isPdf) {
            pageCount = readPdfPageCount(context, uri)
        }

        val needsImageCheck = config.minImageWidth != null || config.minImageHeight != null ||
            config.maxImageWidth != null || config.maxImageHeight != null
        val isImage = mimeType?.startsWith("image/", true) == true ||
            extension in IMAGE_EXTENSIONS
        if (needsImageCheck && isImage) {
            val bounds = readImageBounds(resolver, uri)
            if (bounds == null) {
                return ValidateErr(
                    FilePickerResult.Error(
                        FilePickerErrorType.READ_FAILED,
                        "Could not read image dimensions.",
                        displayName,
                        uri,
                    )
                )
            }
            imageWidth = bounds.first
            imageHeight = bounds.second
            val w = bounds.first
            val h = bounds.second
            config.minImageWidth?.let {
                if (w < it) {
                    return ValidateErr(
                        FilePickerResult.Error(
                            FilePickerErrorType.IMAGE_DIMENSION_INVALID,
                            "Image width ${w}px is below minimum ${it}px.",
                            displayName,
                            uri,
                        )
                    )
                }
            }
            config.minImageHeight?.let {
                if (h < it) {
                    return ValidateErr(
                        FilePickerResult.Error(
                            FilePickerErrorType.IMAGE_DIMENSION_INVALID,
                            "Image height ${h}px is below minimum ${it}px.",
                            displayName,
                            uri,
                        )
                    )
                }
            }
            config.maxImageWidth?.let {
                if (w > it) {
                    return ValidateErr(
                        FilePickerResult.Error(
                            FilePickerErrorType.IMAGE_DIMENSION_INVALID,
                            "Image width ${w}px exceeds maximum ${it}px.",
                            displayName,
                            uri,
                        )
                    )
                }
            }
            config.maxImageHeight?.let {
                if (h > it) {
                    return ValidateErr(
                        FilePickerResult.Error(
                            FilePickerErrorType.IMAGE_DIMENSION_INVALID,
                            "Image height ${h}px exceeds maximum ${it}px.",
                            displayName,
                            uri,
                        )
                    )
                }
            }
        }

        var localFile: File? = null
        if (config.copyToAppStorage) {
            try {
                localFile = copyUriToStorage(
                    context = context,
                    uri = uri,
                    displayName = displayName,
                    subfolder = config.storageSubfolder,
                    maxBytes = config.maxFileSizeBytes,
                )
                sizeBytes = localFile.length()
                if (sizeBytes < config.minFileSizeBytes) {
                    localFile.delete()
                    return ValidateErr(
                        FilePickerResult.Error(
                            FilePickerErrorType.FILE_TOO_SMALL,
                            "File is too small (${PickedFile.formatFileSize(sizeBytes)}).",
                            displayName,
                            uri,
                        )
                    )
                }
                if (sizeBytes > config.maxFileSizeBytes) {
                    localFile.delete()
                    return ValidateErr(
                        FilePickerResult.Error(
                            FilePickerErrorType.FILE_TOO_LARGE,
                            "File is too large (${PickedFile.formatFileSize(sizeBytes)}).",
                            displayName,
                            uri,
                        )
                    )
                }
            } catch (e: SizeLimitExceededException) {
                return ValidateErr(
                    FilePickerResult.Error(
                        FilePickerErrorType.FILE_TOO_LARGE,
                        e.message ?: "File exceeds maximum size.",
                        displayName,
                        uri,
                    )
                )
            } catch (e: Exception) {
                return ValidateErr(
                    FilePickerResult.Error(
                        FilePickerErrorType.COPY_FAILED,
                        e.message ?: "Failed to copy file into app storage.",
                        displayName,
                        uri,
                    )
                )
            }
        }

        return ValidateOk(
            PickedFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes.coerceAtLeast(0),
                extension = extension,
                localFile = localFile,
                pageCount = pageCount,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
        )
    }

    private data class FileMeta(
        val displayName: String?,
        val sizeBytes: Long,
        val mimeType: String?,
    )

    private fun queryMeta(resolver: ContentResolver, uri: Uri): FileMeta {
        var name: String? = null
        var size = -1L
        try {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {
            // fall through
        }
        val mime = try {
            resolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        return FileMeta(name, size, mime)
    }

    /**
     * Stream-count size with hard stop above [maxBytes].
     * @return size, [SIZE_TOO_LARGE] if over limit, or -1 on failure
     */
    private fun measureSize(resolver: ContentResolver, uri: Uri, maxBytes: Long): Long {
        return try {
            resolver.openInputStream(uri)?.use { input ->
                readSizeCapped(input, maxBytes)
            } ?: -1L
        } catch (e: SizeLimitExceededException) {
            SIZE_TOO_LARGE
        } catch (_: Exception) {
            -1L
        }
    }

    private fun readSizeCapped(input: InputStream, maxBytes: Long): Long {
        var total = 0L
        val buffer = ByteArray(DEFAULT_BUFFER)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw SizeLimitExceededException(maxBytes)
        }
        return total
    }

    private fun copyUriToStorage(
        context: Context,
        uri: Uri,
        displayName: String,
        subfolder: String,
        maxBytes: Long,
    ): File {
        val safeSubfolder = sanitizeSubfolder(subfolder)
        val cacheRoot = context.cacheDir.canonicalFile
        val dir = File(cacheRoot, safeSubfolder).canonicalFile
        if (!dir.path.startsWith(cacheRoot.path + File.separator) && dir != cacheRoot) {
            throw IOException("Invalid storage subfolder")
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Unable to create storage directory")
        }

        val unique = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}_$displayName"
        val outFile = File(dir, unique).canonicalFile
        if (!outFile.path.startsWith(dir.path + File.separator)) {
            throw IOException("Invalid output path")
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    var total = 0L
                    val buffer = ByteArray(DEFAULT_BUFFER)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBytes) {
                            throw SizeLimitExceededException(maxBytes)
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            } ?: throw IOException("Unable to open input stream for $displayName")
        } catch (e: Exception) {
            outFile.delete()
            throw e
        }

        if (!outFile.exists() || outFile.length() <= 0L) {
            outFile.delete()
            throw IOException("Copied file is empty or missing")
        }
        return outFile
    }

    private fun readPdfPageCount(context: Context, uri: Uri): Int? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer -> renderer.pageCount }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readImageBounds(resolver: ContentResolver, uri: Uri): Pair<Int, Int>? {
        return try {
            resolver.openInputStream(uri)?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    options.outWidth to options.outHeight
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun tryTakePersistablePermission(activity: ComponentActivity?, uri: Uri) {
        if (activity == null) return
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            activity.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Provider may not support persistable permissions.
        } catch (_: Exception) {
        }
    }

    private fun resolveShareableUri(context: Context, file: PickedFile): Uri {
        val local = file.localFile
        if (local != null && local.exists()) {
            val cacheRoot = context.cacheDir.canonicalFile
            val localCanonical = try {
                local.canonicalFile
            } catch (_: Exception) {
                null
            }
            // Only expose files that live under our cache via FileProvider.
            if (localCanonical != null &&
                (localCanonical.path.startsWith(cacheRoot.path + File.separator) ||
                    localCanonical == cacheRoot)
            ) {
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    localCanonical,
                )
            }
        }
        return file.uri
    }

    private fun resolveOpenMimeType(context: Context, file: PickedFile, uri: Uri): String {
        file.mimeType?.takeIf { it.isNotBlank() && it != "*/*" }?.let { return it }
        try {
            context.contentResolver.getType(uri)
                ?.takeIf { it.isNotBlank() && it != "*/*" }
                ?.let { return it }
        } catch (_: Exception) {
        }
        mimeFromExtension(file.extension)?.let { return it }
        // Prefer octet-stream over */* to avoid overly broad intent matching.
        return "application/octet-stream"
    }

    private fun mimeMatches(mime: String, allowed: List<String>): Boolean {
        val rules = allowed.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("*/*") }
        if (rules.any { it == "*/*" }) return true
        val lower = mime.lowercase(Locale.US)
        return rules.any { rule ->
            val r = rule.lowercase(Locale.US)
            when {
                r == "*/*" -> true
                r.endsWith("/*") -> {
                    val prefix = r.removeSuffix("/*")
                    prefix.isNotEmpty() && lower.startsWith("$prefix/")
                }
                else -> lower == r
            }
        }
    }

    private fun mimeFromExtension(extension: String): String? {
        if (extension.isBlank()) return null
        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase(Locale.US))
    }

    private fun extensionFromMime(mime: String?): String {
        if (mime.isNullOrBlank()) return ""
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mime)
            ?.lowercase(Locale.US)
            .orEmpty()
    }

    private fun uriToDisplayName(context: Context, uri: Uri): String {
        return sanitizeDisplayName(
            queryMeta(context.contentResolver, uri).displayName
                ?: uri.lastPathSegment
                ?: "file"
        )
    }

    private fun deliver(result: FilePickerResult) {
        val cb = currentCallback
        val finish = result !is FilePickerResult.Processing
        val post = {
            cb?.invoke(result)
            if (finish) {
                resetSession()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) post()
        else mainHandler.post(post)
    }

    private fun resetSession() {
        isBusy.set(false)
        currentCallback = null
        currentConfig = null
        currentExistingCount = 0
    }

    private class SizeLimitExceededException(maxBytes: Long) : IOException(
        "File exceeds maximum ${PickedFile.formatFileSize(maxBytes)}."
    )

    companion object {
        private const val DEFAULT_BUFFER = 8 * 1024
        private const val SIZE_TOO_LARGE = -2L
        private const val MAX_DISPLAY_NAME_LENGTH = 180
        private val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif"
        )
        private val SAFE_SUBFOLDER = Regex("^[A-Za-z0-9._-]+$")

        internal fun sanitizeSubfolder(raw: String): String {
            val trimmed = raw.trim().trim('/', '\\')
            if (trimmed.isEmpty() || trimmed == "." || trimmed.contains("..") ||
                !SAFE_SUBFOLDER.matches(trimmed)
            ) {
                return "picked_files"
            }
            return trimmed
        }

        internal fun sanitizeDisplayName(name: String): String {
            val cleaned = name
                .replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "_")
                .trim()
                .ifBlank { "file" }
            if (cleaned.length <= MAX_DISPLAY_NAME_LENGTH) return cleaned
            val ext = PickedFile.extensionFromName(cleaned)
            val base = cleaned.substringBeforeLast('.', cleaned)
                .take(MAX_DISPLAY_NAME_LENGTH - if (ext.isNotEmpty()) ext.length + 1 else 0)
            return if (ext.isNotEmpty()) "$base.$ext" else base
        }

        @Volatile
        private var INSTANCE: FilePickerManager? = null

        @JvmStatic
        fun init(application: Application) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = FilePickerManager(application)
                    }
                }
            }
        }

        @JvmStatic
        val instance: FilePickerManager
            get() = INSTANCE
                ?: throw FilePickerException(
                    FilePickerException.ERROR_NOT_INITIALIZED,
                    FilePickerErrorType.NOT_INITIALIZED,
                )

        @JvmStatic
        val isInitialized: Boolean
            get() = INSTANCE != null
    }
}
