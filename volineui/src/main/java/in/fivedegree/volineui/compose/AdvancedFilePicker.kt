@file:Suppress("unused")

package `in`.fivedegree.volineui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import `in`.fivedegree.volineui.FilePickerManager
import `in`.fivedegree.volineui.R
import `in`.fivedegree.volineui.filepicker.FilePickerColors
import `in`.fivedegree.volineui.filepicker.FilePickerDefaults
import `in`.fivedegree.volineui.filepicker.FilePickerUiState
import `in`.fivedegree.volineui.filepickermanager.FilePickerConfig
import `in`.fivedegree.volineui.filepickermanager.FilePickerErrorType
import `in`.fivedegree.volineui.filepickermanager.FilePickerException
import `in`.fivedegree.volineui.filepickermanager.FilePickerResult
import `in`.fivedegree.volineui.filepickermanager.PickedFile
import `in`.fivedegree.volineui.imageview.ActionButtonConfig
import `in`.fivedegree.volineui.imageview.ImageViewDefaults
import java.io.File
import kotlin.jvm.JvmName
import androidx.compose.ui.graphics.toArgb

/**
 * Advanced file picker for Jetpack Compose — mirrors [AdvancedImageView] / [ImageCarousel]
 * patterns for non-image documents (PDF, CSV, etc.).
 *
 * Features:
 * - Config-driven MIME / extension / size / PDF page / image dimension validation
 * - Single-file placeholder or multi-file horizontal card carousel
 * - Delete, bottom-right action chips (`[Open] + custom`), loading & error states
 * - [FilePickerManager] integration (or custom [onPickRequest])
 * - Typed [onError] callbacks for host-owned UX
 *
 * File-based usage (like [ImageCarousel]):
 * ```kotlin
 * var files by remember { mutableStateOf<List<File>>(emptyList()) }
 *
 * AdvancedFilePicker(
 *     files = files,
 *     onFilesChange = { files = it },
 *     config = FilePickerConfig.pdf(allowMultiple = true),
 *     label = "Documents",
 *     actionButtons = { file, index -> listOf(/* Upload, Info, … */) },
 * )
 * ```
 */
@Composable
@JvmName("AdvancedFilePickerJavaFiles")
fun AdvancedFilePicker(
    files: List<File>,
    onFilesChange: (List<File>) -> Unit,
    modifier: Modifier = Modifier,
    config: FilePickerConfig = FilePickerConfig(),
    // Label
    label: String = "",
    labelGap: Dp = FilePickerDefaults.LabelGap,
    labelTextStyle: TextStyle = FilePickerDefaults.LabelTextStyle,
    // Layout
    cornerRadius: Dp = FilePickerDefaults.CornerRadius,
    borderWidth: Dp = FilePickerDefaults.BorderWidth,
    colors: FilePickerColors = FilePickerDefaults.colors(),
    // Placeholder
    placeholderText: String = FilePickerDefaults.PlaceholderText,
    placeholderIconResId: Int = 0,
    placeholderIconSize: Dp = FilePickerDefaults.PlaceholderIconSize,
    placeholderTextSize: TextUnit = FilePickerDefaults.PlaceholderTextSize,
    placeholderGap: Dp = FilePickerDefaults.PlaceholderGap,
    // Carousel (multi)
    carouselHeight: Dp = FilePickerDefaults.CarouselHeight,
    itemWidth: Dp = FilePickerDefaults.ItemWidth,
    itemSpacing: Dp = FilePickerDefaults.ItemSpacing,
    // Features
    showDeleteButton: Boolean = true,
    enableOpenInApp: Boolean = true,
    showLoadingIndicator: Boolean = true,
    enablePickOnTap: Boolean = true,
    isLoading: Boolean = false,
    /** Per-file action chips; built-in Open (if enabled) is prepended automatically. */
    actionButtons: (File, Int) -> List<ActionButtonConfig> = { _, _ -> emptyList() },
    // Callbacks
    onFileClick: ((File, Int) -> Unit)? = null,
    onFileDelete: ((File, Int) -> Unit)? = null,
    onOpenClick: ((File, Int) -> Unit)? = null,
    onPickRequest: ((FilePickerConfig) -> Unit)? = null,
    onPickResult: ((FilePickerResult) -> Unit)? = null,
    onError: ((FilePickerErrorType, String) -> Unit)? = null,
    onStateChange: ((FilePickerUiState) -> Unit)? = null,
) {
    // File API requires local copies so onFilesChange can return java.io.File.
    val effectiveConfig = remember(config) { config.copy(copyToAppStorage = true) }
    val pickedFiles = remember(files.map { it.absolutePath }) {
        files.map { PickedFile.from(it) }
    }
    val hostActionButtons = actionButtons

    AdvancedFilePicker(
        files = pickedFiles,
        modifier = modifier,
        config = effectiveConfig,
        label = label,
        labelGap = labelGap,
        labelTextStyle = labelTextStyle,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        colors = colors,
        placeholderText = placeholderText,
        placeholderIconResId = placeholderIconResId,
        placeholderIconSize = placeholderIconSize,
        placeholderTextSize = placeholderTextSize,
        placeholderGap = placeholderGap,
        carouselHeight = carouselHeight,
        itemWidth = itemWidth,
        itemSpacing = itemSpacing,
        showDeleteButton = showDeleteButton,
        enableOpenInApp = enableOpenInApp,
        showLoadingIndicator = showLoadingIndicator,
        enablePickOnTap = enablePickOnTap,
        isLoading = isLoading,
        actionButtons = { index ->
            val javaFile = files.getOrNull(index) ?: return@AdvancedFilePicker emptyList()
            hostActionButtons(javaFile, index)
        },
        onFilesChange = { next ->
            onFilesChange(next.mapNotNull { it.asLocalFile() })
        },
        onFileClick = onFileClick?.let { cb ->
            { picked, index -> picked.asLocalFile()?.let { cb(it, index) } }
        },
        onFileDelete = onFileDelete?.let { cb ->
            { picked, index -> picked.asLocalFile()?.let { cb(it, index) } }
        },
        onOpenClick = onOpenClick?.let { cb ->
            { picked, index -> picked.asLocalFile()?.let { cb(it, index) } }
        },
        onPickRequest = onPickRequest,
        onPickResult = onPickResult,
        onError = onError,
        onStateChange = onStateChange,
    )
}

/**
 * Low-level API with [PickedFile] (Uri + metadata). Prefer the [File]-based overload
 * when you only need local files, matching [ImageCarousel].
 *
 * ```kotlin
 * val state = rememberAdvancedFilePickerState()
 *
 * AdvancedFilePicker(
 *     state = state,
 *     config = FilePickerConfig.pdf(maxFileSizeBytes = 5_000_000, allowMultiple = true),
 *     label = "Documents",
 *     onError = { type, message -> },
 * )
 * ```
 */
@Composable
fun AdvancedFilePicker(
    files: List<PickedFile> = emptyList(),
    modifier: Modifier = Modifier,
    config: FilePickerConfig = FilePickerConfig(),
    // Label
    label: String = "",
    labelGap: Dp = FilePickerDefaults.LabelGap,
    labelTextStyle: TextStyle = FilePickerDefaults.LabelTextStyle,
    // Layout
    cornerRadius: Dp = FilePickerDefaults.CornerRadius,
    borderWidth: Dp = FilePickerDefaults.BorderWidth,
    colors: FilePickerColors = FilePickerDefaults.colors(),
    // Placeholder
    placeholderText: String = FilePickerDefaults.PlaceholderText,
    placeholderIconResId: Int = 0,
    placeholderIconSize: Dp = FilePickerDefaults.PlaceholderIconSize,
    placeholderTextSize: TextUnit = FilePickerDefaults.PlaceholderTextSize,
    placeholderGap: Dp = FilePickerDefaults.PlaceholderGap,
    // Carousel (multi)
    carouselHeight: Dp = FilePickerDefaults.CarouselHeight,
    itemWidth: Dp = FilePickerDefaults.ItemWidth,
    itemSpacing: Dp = FilePickerDefaults.ItemSpacing,
    // Features
    showDeleteButton: Boolean = true,
    enableOpenInApp: Boolean = true,
    showLoadingIndicator: Boolean = true,
    enablePickOnTap: Boolean = true,
    isLoading: Boolean = false,
    /**
     * Per-item action chips (bottom-right). Built-in Open chip is prepended when
     * [enableOpenInApp] is true: `[Open] + actionButtons(index)`.
     */
    actionButtons: (Int) -> List<ActionButtonConfig> = { emptyList() },
    // Callbacks
    onFilesChange: ((List<PickedFile>) -> Unit)? = null,
    onFileClick: ((PickedFile, Int) -> Unit)? = null,
    onFileDelete: ((PickedFile, Int) -> Unit)? = null,
    onOpenClick: ((PickedFile, Int) -> Unit)? = null,
    onPickRequest: ((FilePickerConfig) -> Unit)? = null,
    onPickResult: ((FilePickerResult) -> Unit)? = null,
    onError: ((FilePickerErrorType, String) -> Unit)? = null,
    onStateChange: ((FilePickerUiState) -> Unit)? = null,
) {
    val context = LocalContext.current
    var internalLoading by remember { mutableStateOf(false) }
    var uiState by remember { mutableStateOf(FilePickerUiState.EMPTY) }
    var lastErrorMessage by remember { mutableStateOf<String?>(null) }

    val effectiveLoading = isLoading || internalLoading

    fun updateState(newState: FilePickerUiState) {
        if (uiState != newState) {
            uiState = newState
            onStateChange?.invoke(newState)
        }
    }

    LaunchedEffect(files, effectiveLoading, lastErrorMessage) {
        when {
            effectiveLoading -> updateState(FilePickerUiState.LOADING)
            files.isNotEmpty() -> {
                lastErrorMessage = null
                updateState(FilePickerUiState.LOADED)
            }
            lastErrorMessage != null -> updateState(FilePickerUiState.ERROR)
            else -> updateState(FilePickerUiState.EMPTY)
        }
    }

    val effectiveConfig = remember(config, files) {
        config.copy(existingUris = files.map { it.uri.toString() })
    }

    val canAddMore = files.size < effectiveConfig.maxFileCount

    fun emitError(type: FilePickerErrorType, message: String) {
        lastErrorMessage = message
        onError?.invoke(type, message)
    }

    fun handlePickResult(result: FilePickerResult) {
        when (result) {
            is FilePickerResult.Processing -> {
                internalLoading = true
                onPickResult?.invoke(result)
            }
            is FilePickerResult.Cancelled -> {
                internalLoading = false
                onPickResult?.invoke(result)
            }
            is FilePickerResult.Error -> {
                internalLoading = false
                emitError(result.type, result.message)
                onPickResult?.invoke(result)
            }
            is FilePickerResult.Success -> {
                internalLoading = false
                if (result.files.isEmpty()) {
                    onPickResult?.invoke(result)
                    return
                }
                val previous = files
                val merged = if (effectiveConfig.allowMultiple) {
                    val existingKeys = previous.map { it.uri.toString() }.toMutableSet()
                    val additions = result.files.filter { existingKeys.add(it.uri.toString()) }
                    (previous + additions).take(effectiveConfig.maxFileCount)
                } else {
                    result.files.take(1)
                }
                // Drop orphaned cache copies that are no longer referenced.
                val keptIds = merged.map { it.id }.toSet()
                previous
                    .filter { it.id !in keptIds }
                    .forEach { orphan -> orphan.localFile?.delete() }
                // Update files first so host state is current before onPickResult observers run.
                onFilesChange?.invoke(merged)
                onPickResult?.invoke(result)
            }
        }
    }

    fun launchPick() {
        if (!enablePickOnTap || effectiveLoading || !canAddMore) return

        if (onPickRequest != null) {
            onPickRequest(effectiveConfig)
            return
        }

        try {
            FilePickerManager.instance.pickFiles(
                config = effectiveConfig,
                existingFileCount = files.size,
            ) { handlePickResult(it) }
        } catch (e: FilePickerException) {
            emitError(e.errorType, e.message ?: "File picker error")
        } catch (e: Exception) {
            emitError(FilePickerErrorType.UNKNOWN, e.message ?: "File picker error")
        }
    }

    fun openFile(file: PickedFile, index: Int) {
        if (onOpenClick != null) {
            onOpenClick(file, index)
            return
        }
        if (!enableOpenInApp) return
        try {
            val result = FilePickerManager.instance.openFile(context, file)
            // Do not emit Success on onPickResult — that would look like a new pick to hosts.
            if (result is FilePickerResult.Error) {
                onPickResult?.invoke(result)
                emitError(result.type, result.message)
            }
        } catch (e: FilePickerException) {
            emitError(e.errorType, e.message ?: "Open failed")
        } catch (e: Exception) {
            emitError(FilePickerErrorType.OPEN_FAILED, e.message ?: "Open failed")
        }
    }

    fun deleteFile(file: PickedFile, index: Int) {
        val next = files.toMutableList().also {
            if (index in it.indices) it.removeAt(index)
            else it.removeAll { f -> f.id == file.id }
        }
        // Update list first so host UI reading state.files sees the new list in onFileDelete.
        onFilesChange?.invoke(next)
        onFileDelete?.invoke(file, index)
        // Best-effort cleanup of copied cache file
        file.localFile?.delete()
    }

    val shape = RoundedCornerShape(cornerRadius)

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(text = label, style = labelTextStyle)
            Spacer(modifier = Modifier.height(labelGap))
        }

        if (files.isEmpty() && !effectiveConfig.allowMultiple) {
            // Single-file empty / loading / error placeholder (AdvancedImageView style)
            SingleFilePlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FilePickerDefaults.DefaultHeight)
                    .height(FilePickerDefaults.DefaultHeight),
                shape = shape,
                borderWidth = borderWidth,
                colors = colors,
                uiState = uiState,
                placeholderText = lastErrorMessage?.takeIf { uiState == FilePickerUiState.ERROR }
                    ?: placeholderText,
                placeholderIconResId = placeholderIconResId,
                placeholderIconSize = placeholderIconSize,
                placeholderTextSize = placeholderTextSize,
                placeholderGap = placeholderGap,
                showLoadingIndicator = showLoadingIndicator,
                isLoading = effectiveLoading,
                onClick = { launchPick() },
            )
        } else if (files.isEmpty() && effectiveConfig.allowMultiple) {
            // Multi mode but empty — show add tile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(carouselHeight),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                if (effectiveLoading) {
                    FileProcessingCard(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight(),
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        colors = colors,
                    )
                }
                if (canAddMore && !effectiveLoading) {
                    FileAddCard(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight(),
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        colors = colors,
                        onClick = { launchPick() },
                    )
                }
            }
        } else if (!effectiveConfig.allowMultiple) {
            // Single selected file
            val file = files.first()
            FileCard(
                file = file,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FilePickerDefaults.DefaultHeight)
                    .height(FilePickerDefaults.DefaultHeight),
                cornerRadius = cornerRadius,
                borderWidth = borderWidth,
                colors = colors,
                compact = false,
                showDeleteButton = showDeleteButton,
                actionButtons = resolveFileActionButtons(
                    enableOpenInApp = enableOpenInApp,
                    openIconTint = colors.openIconTint,
                    custom = actionButtons(0),
                    onOpen = { openFile(file, 0) },
                ),
                onClick = { onFileClick?.invoke(file, 0) },
                onDelete = { deleteFile(file, 0) },
            )
            if (effectiveLoading) {
                // Soft overlay indicator while replacing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(FilePickerDefaults.LoadingIndicatorSize),
                        color = colors.loadingColor,
                        strokeWidth = FilePickerDefaults.LoadingIndicatorStrokeWidth,
                    )
                }
            }
        } else {
            // Multi-file carousel
            val scroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(carouselHeight)
                    .horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                files.forEachIndexed { index, file ->
                    key(file.id) {
                        FileCard(
                            file = file,
                            modifier = Modifier
                                .width(itemWidth)
                                .fillMaxHeight(),
                            cornerRadius = cornerRadius,
                            borderWidth = borderWidth,
                            colors = colors,
                            compact = true,
                            showDeleteButton = showDeleteButton,
                            actionButtons = resolveFileActionButtons(
                                enableOpenInApp = enableOpenInApp,
                                openIconTint = colors.openIconTint,
                                custom = actionButtons(index),
                                onOpen = { openFile(file, index) },
                            ),
                            onClick = { onFileClick?.invoke(file, index) },
                            onDelete = { deleteFile(file, index) },
                        )
                    }
                }

                if (effectiveLoading) {
                    FileProcessingCard(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight(),
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        colors = colors,
                    )
                }

                if (canAddMore && !effectiveLoading) {
                    FileAddCard(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight(),
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        colors = colors,
                        onClick = { launchPick() },
                    )
                }
            }
        }
    }
}

/**
 * State-holder overload — preferred for form screens.
 */
@Composable
fun AdvancedFilePicker(
    state: AdvancedFilePickerState,
    modifier: Modifier = Modifier,
    config: FilePickerConfig = FilePickerConfig(),
    label: String = "",
    labelGap: Dp = FilePickerDefaults.LabelGap,
    labelTextStyle: TextStyle = FilePickerDefaults.LabelTextStyle,
    cornerRadius: Dp = FilePickerDefaults.CornerRadius,
    borderWidth: Dp = FilePickerDefaults.BorderWidth,
    colors: FilePickerColors = FilePickerDefaults.colors(),
    placeholderText: String = FilePickerDefaults.PlaceholderText,
    placeholderIconResId: Int = 0,
    placeholderIconSize: Dp = FilePickerDefaults.PlaceholderIconSize,
    placeholderTextSize: TextUnit = FilePickerDefaults.PlaceholderTextSize,
    placeholderGap: Dp = FilePickerDefaults.PlaceholderGap,
    carouselHeight: Dp = FilePickerDefaults.CarouselHeight,
    itemWidth: Dp = FilePickerDefaults.ItemWidth,
    itemSpacing: Dp = FilePickerDefaults.ItemSpacing,
    showDeleteButton: Boolean = true,
    enableOpenInApp: Boolean = true,
    showLoadingIndicator: Boolean = true,
    enablePickOnTap: Boolean = true,
    actionButtons: (Int) -> List<ActionButtonConfig> = { emptyList() },
    onFileClick: ((PickedFile, Int) -> Unit)? = null,
    onFileDelete: ((PickedFile, Int) -> Unit)? = null,
    onOpenClick: ((PickedFile, Int) -> Unit)? = null,
    onPickRequest: ((FilePickerConfig) -> Unit)? = null,
    onPickResult: ((FilePickerResult) -> Unit)? = null,
    onError: ((FilePickerErrorType, String) -> Unit)? = null,
    onStateChange: ((FilePickerUiState) -> Unit)? = null,
) {
    AdvancedFilePicker(
        files = state.files,
        modifier = modifier,
        config = config,
        label = label,
        labelGap = labelGap,
        labelTextStyle = labelTextStyle,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        colors = colors,
        placeholderText = placeholderText,
        placeholderIconResId = placeholderIconResId,
        placeholderIconSize = placeholderIconSize,
        placeholderTextSize = placeholderTextSize,
        placeholderGap = placeholderGap,
        carouselHeight = carouselHeight,
        itemWidth = itemWidth,
        itemSpacing = itemSpacing,
        showDeleteButton = showDeleteButton,
        enableOpenInApp = enableOpenInApp,
        showLoadingIndicator = showLoadingIndicator,
        enablePickOnTap = enablePickOnTap,
        isLoading = state.isForceLoading,
        actionButtons = actionButtons,
        onFilesChange = { state.replaceFiles(it) },
        onFileClick = onFileClick,
        onFileDelete = onFileDelete,
        onOpenClick = onOpenClick,
        onPickRequest = onPickRequest,
        onPickResult = { result ->
            when (result) {
                is FilePickerResult.Processing -> state.showLoading()
                is FilePickerResult.Success,
                is FilePickerResult.Error,
                is FilePickerResult.Cancelled,
                -> state.hideLoading()
            }
            onPickResult?.invoke(result)
        },
        onError = onError,
        onStateChange = { newState ->
            state.currentState = newState
            onStateChange?.invoke(newState)
        },
    )
}

// region Private UI pieces

@Composable
private fun SingleFilePlaceholder(
    modifier: Modifier,
    shape: RoundedCornerShape,
    borderWidth: Dp,
    colors: FilePickerColors,
    uiState: FilePickerUiState,
    placeholderText: String,
    placeholderIconResId: Int,
    placeholderIconSize: Dp,
    placeholderTextSize: TextUnit,
    placeholderGap: Dp,
    showLoadingIndicator: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.backgroundColor, shape)
            .then(
                if (borderWidth > 0.dp) Modifier.border(borderWidth, colors.borderColor(uiState), shape)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = !isLoading,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (!isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(
                        id = when {
                            placeholderIconResId != 0 -> placeholderIconResId
                            uiState == FilePickerUiState.ERROR -> R.drawable.ic_clear
                            else -> R.drawable.ic_file
                        }
                    ),
                    contentDescription = "Select file",
                    modifier = Modifier.size(placeholderIconSize),
                    tint = colors.placeholderIconColor(uiState),
                )
                Spacer(modifier = Modifier.height(placeholderGap))
                Text(
                    text = placeholderText,
                    style = TextStyle(
                        fontSize = placeholderTextSize,
                        color = colors.placeholderTextColor(uiState),
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        AnimatedVisibility(
            visible = isLoading && showLoadingIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(FilePickerDefaults.LoadingIndicatorSize),
                color = colors.loadingColor,
                strokeWidth = FilePickerDefaults.LoadingIndicatorStrokeWidth,
            )
        }
    }
}

@Composable
private fun FileCard(
    file: PickedFile,
    modifier: Modifier,
    cornerRadius: Dp,
    borderWidth: Dp,
    colors: FilePickerColors,
    compact: Boolean,
    showDeleteButton: Boolean,
    actionButtons: List<ActionButtonConfig>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val iconRes = remember(file.mimeType, file.extension) { iconForFile(file) }
    val actionScroll = rememberScrollState()
    val chipCorner = (cornerRadius.value * 0.4f).coerceAtLeast(4f).dp
    val bottomContentPad =
        if (actionButtons.isNotEmpty()) ImageViewDefaults.ActionButtonMinHeight + 10.dp else 0.dp

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.cardBackgroundColor, shape)
            .then(
                if (borderWidth > 0.dp) Modifier.border(borderWidth, colors.borderColor, shape)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ) { onClick() },
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FilePickerDefaults.CardPadding)
                    .padding(bottom = bottomContentPad),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = file.extension,
                    modifier = Modifier.size(FilePickerDefaults.FileIconSize),
                    tint = colors.fileIconTint,
                )
                Column {
                    Text(
                        text = file.displayName,
                        style = TextStyle(
                            fontSize = FilePickerDefaults.FileNameTextSize,
                            fontWeight = FontWeight.Medium,
                            color = colors.fileNameColor,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildMetaLine(file),
                        style = TextStyle(
                            fontSize = FilePickerDefaults.FileMetaTextSize,
                            color = colors.fileMetaColor,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FilePickerDefaults.CardPadding)
                    .padding(bottom = bottomContentPad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = file.extension,
                    modifier = Modifier.size(FilePickerDefaults.FileIconSize + 4.dp),
                    tint = colors.fileIconTint,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.displayName,
                        style = TextStyle(
                            fontSize = FilePickerDefaults.FileNameTextSize,
                            fontWeight = FontWeight.Medium,
                            color = colors.fileNameColor,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildMetaLine(file),
                        style = TextStyle(
                            fontSize = FilePickerDefaults.FileMetaTextSize,
                            color = colors.fileMetaColor,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (showDeleteButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(FilePickerDefaults.DeleteButtonMargin)
                    .size(FilePickerDefaults.DeleteButtonSize)
                    .clip(RoundedCornerShape((cornerRadius.value * 0.4f).coerceAtLeast(4f).dp))
                    .background(colors.deleteButtonBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                    ) { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clear),
                    contentDescription = "Remove file",
                    modifier = Modifier.size(
                        FilePickerDefaults.DeleteButtonSize -
                            FilePickerDefaults.DeleteButtonPadding * 2
                    ),
                    tint = colors.deleteIconTint,
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = actionButtons.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .padding(ImageViewDefaults.ActionButtonRowMargin),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(actionScroll),
                    horizontalArrangement = Arrangement.spacedBy(ImageViewDefaults.ActionButtonSpacing),
                ) {
                    actionButtons.forEach { cfg ->
                        ActionButtonChip(config = cfg, cornerRadius = chipCorner)
                    }
                }
            }
        }
    }
}

private fun resolveFileActionButtons(
    enableOpenInApp: Boolean,
    openIconTint: androidx.compose.ui.graphics.Color,
    custom: List<ActionButtonConfig>,
    onOpen: () -> Unit,
): List<ActionButtonConfig> {
    val openChip = if (enableOpenInApp) {
        listOf(
            ActionButtonConfig(
                iconResId = R.drawable.ic_open_in_new,
                text = null,
                iconTint = openIconTint.toArgb(),
                backgroundColor = 0x66000000,
                contentDescription = "Open",
                onClick = onOpen,
            )
        )
    } else {
        emptyList()
    }
    return openChip + custom
}

@Composable
private fun FileAddCard(
    modifier: Modifier,
    cornerRadius: Dp,
    borderWidth: Dp,
    colors: FilePickerColors,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.backgroundColor, shape)
            .border(borderWidth, colors.borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_add_file),
            contentDescription = "Add file",
            modifier = Modifier.size(FilePickerDefaults.AddButtonIconSize),
            tint = colors.addButtonIconColor,
        )
    }
}

@Composable
private fun FileProcessingCard(
    modifier: Modifier,
    cornerRadius: Dp,
    borderWidth: Dp,
    colors: FilePickerColors,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.backgroundColor, shape)
            .border(borderWidth, colors.borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(FilePickerDefaults.LoadingIndicatorSize),
            color = colors.loadingColor,
            strokeWidth = FilePickerDefaults.LoadingIndicatorStrokeWidth,
        )
    }
}

private fun buildMetaLine(file: PickedFile): String {
    val parts = mutableListOf(file.sizeDisplay)
    file.extension.takeIf { it.isNotBlank() }?.let { parts += it.uppercase() }
    file.pageCount?.let { parts += "$it p" }
    return parts.joinToString(" · ")
}

private fun iconForFile(file: PickedFile): Int = when {
    file.isPdf -> R.drawable.ic_file_pdf
    file.isCsv -> R.drawable.ic_file_csv
    file.isImage -> R.drawable.ic_file_image
    else -> R.drawable.ic_file
}

// endregion

/**
 * Mutable state holder for [AdvancedFilePicker].
 */
class AdvancedFilePickerState {
    var files by mutableStateOf<List<PickedFile>>(emptyList())
        private set

    var currentState by mutableStateOf(FilePickerUiState.EMPTY)
        internal set

    var isForceLoading by mutableStateOf(false)
        private set

    fun showLoading() {
        isForceLoading = true
        currentState = FilePickerUiState.LOADING
    }

    fun hideLoading() {
        isForceLoading = false
    }

    fun replaceFiles(newFiles: List<PickedFile>, deleteOrphans: Boolean = true) {
        if (deleteOrphans) {
            val keptIds = newFiles.map { it.id }.toSet()
            files.filter { it.id !in keptIds }.forEach { it.localFile?.delete() }
        }
        isForceLoading = false
        files = newFiles
        currentState = if (newFiles.isEmpty()) FilePickerUiState.EMPTY else FilePickerUiState.LOADED
    }

    fun addFiles(newFiles: List<PickedFile>, maxCount: Int = Int.MAX_VALUE) {
        val keys = files.map { it.uri.toString() }.toMutableSet()
        val merged = files + newFiles.filter { keys.add(it.uri.toString()) }
        replaceFiles(merged.take(maxCount))
    }

    fun removeAt(index: Int) {
        if (index !in files.indices) return
        val removed = files[index]
        replaceFiles(files.toMutableList().also { it.removeAt(index) })
        removed.localFile?.delete()
    }

    fun clear(deleteLocalCopies: Boolean = true) {
        if (deleteLocalCopies) {
            files.forEach { it.localFile?.delete() }
        }
        isForceLoading = false
        files = emptyList()
        currentState = FilePickerUiState.EMPTY
    }

    fun firstOrNull(): PickedFile? = files.firstOrNull()

    fun hasFiles(): Boolean = files.isNotEmpty()

    fun isLoading(): Boolean = currentState == FilePickerUiState.LOADING

    fun isError(): Boolean = currentState == FilePickerUiState.ERROR

    fun isEmpty(): Boolean = currentState == FilePickerUiState.EMPTY
}

@Composable
fun rememberAdvancedFilePickerState(
    initialFiles: List<PickedFile> = emptyList(),
): AdvancedFilePickerState {
    return remember {
        AdvancedFilePickerState().apply {
            if (initialFiles.isNotEmpty()) replaceFiles(initialFiles)
        }
    }
}
