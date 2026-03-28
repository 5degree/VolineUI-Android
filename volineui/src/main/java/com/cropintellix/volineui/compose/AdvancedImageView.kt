@file:Suppress("unused")

package com.cropintellix.volineui.compose

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cropintellix.volineui.PhotoCaptureManager
import com.cropintellix.volineui.R
import com.cropintellix.volineui.imageview.*
import com.cropintellix.volineui.photocapturemanager.PhotoCaptureConfig
import com.cropintellix.volineui.photocapturemanager.PhotoCaptureResult
import java.io.File

/**
 * Advanced image view component for Jetpack Compose with comprehensive features.
 *
 * Features:
 * - Image loading from multiple sources (URL, File, Bitmap, Uri, Drawable, Base64)
 * - Scale types (FIT, FILL, CROP, CENTER, STRETCH)
 * - Visual styling (corner radius, border width/color, background color)
 * - Label support (text, gap, labelTextStyle)
 * - Placeholder (icon, text, colors, gap)
 * - Loading states (progress indicator)
 * - Delete button (optional, customizable tint)
 * - Full-screen preview integration
 * - State management (EMPTY, LOADING, LOADED, ERROR)
 * - Camera capture integration
 *
 * @param source Image source to load
 * @param modifier Modifier for the component
 * @param scaleType How to scale the image
 * @param aspectRatio Optional aspect ratio (width/height). If 0, uses available space
 * @param cornerRadius Corner radius for the image
 * @param borderWidth Border width
 * @param colors Color configuration for the image chrome (not the label; use [labelTextStyle] for label appearance)
 * @param label Optional label text above the image
 * @param labelGap Gap between label and image
 * @param labelTextStyle Full typography for the label (including color when you override the default)
 * @param placeholderText Text to show in placeholder
 * @param placeholderIconResId Custom placeholder icon resource ID
 * @param placeholderIconSize Placeholder icon size
 * @param placeholderTextSize Placeholder text size
 * @param placeholderGap Gap between placeholder icon and text
 * @param showDeleteButton Whether to show delete button when image is loaded
 * @param showLoadingIndicator Whether to show loading indicator
 * @param loadingGifResId Optional GIF drawable shown while loading (overrides spinner)
 * @param enableFullScreenPreview Whether to enable full-screen preview on tap
 * @param enableCameraCapture Whether to enable camera capture on placeholder tap
 * @param captureConfig Configuration passed to capture callback when placeholder is tapped
 * @param onCapturedImageClick Callback when loaded image is clicked (after fullscreen preview if enabled)
 * @param onImageClick Callback when the image container is tapped (below the label), including empty,
 *   loading, loaded, and error states. Use this instead of putting Modifier.clickable on the root when
 *   you only want the image region to be clickable.
 * @param onDeleteClick Callback when delete button is clicked
 * @param actionButtons Optional chips at bottom-right when image is loaded (horizontally scrollable)
 * @param onCaptureRequest Callback with [PhotoCaptureConfig] when placeholder is tapped
 * @param onCaptureResult Callback when image is captured and all related operations are completed
 * @param onImageLoadResult Callback with load result (success/failure)
 * @param onStateChange Callback when state changes
 */
@Composable
fun AdvancedImageView(
    source: ImageSource = ImageSource.Empty,
    modifier: Modifier = Modifier,
    scaleType: ImageScaleType = ImageScaleType.CROP,
    imageAlignment: Alignment = Alignment.Center,
    aspectRatio: Float = 0f,
    cornerRadius: Dp = ImageViewDefaults.CornerRadius,
    borderWidth: Dp = ImageViewDefaults.BorderWidth,
    colors: ImageViewColors = ImageViewDefaults.colors(),
    // Label
    label: String = "",
    labelGap: Dp = ImageViewDefaults.LabelGap,
    labelTextStyle: TextStyle = ImageViewDefaults.LabelTextStyle,
    // Placeholder
    placeholderText: String = ImageViewDefaults.PlaceholderText,
    placeholderIconResId: Int = 0,
    placeholderIconSize: Dp = ImageViewDefaults.PlaceholderIconSize,
    placeholderTextSize: TextUnit = ImageViewDefaults.PlaceholderTextSize,
    placeholderGap: Dp = ImageViewDefaults.PlaceholderGap,
    // Features
    showDeleteButton: Boolean = true,
    showLoadingIndicator: Boolean = true,
    loadingGifResId: Int = 0,
    enableFullScreenPreview: Boolean = true,
    enableCameraCapture: Boolean = true,
    captureConfig: PhotoCaptureConfig = PhotoCaptureConfig(),
    // Callbacks
    onCapturedImageClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    actionButtons: List<ActionButtonConfig> = emptyList(),
    onCaptureRequest: ((PhotoCaptureConfig) -> Unit)? = null,
    onCaptureResult: ((ImageSource) -> Unit)? = null,
    onImageLoadResult: ((Boolean) -> Unit)? = null,
    onStateChange: ((ImageState) -> Unit)? = null,
    // Force loading state (used during photo processing)
    isLoading: Boolean = false,
) {
    val context = LocalContext.current
    var currentState by remember { mutableStateOf(ImageState.EMPTY) }
    var internalLoading by remember { mutableStateOf(false) }

    // Update state and notify
    fun updateState(newState: ImageState) {
        if (currentState != newState) {
            currentState = newState
            onStateChange?.invoke(newState)
        }
    }

    // Handle force loading state from external or internal (capture processing) control
    LaunchedEffect(isLoading, internalLoading) {
        if (isLoading || internalLoading) {
            updateState(ImageState.LOADING)
        }
    }

    // Determine initial state based on source.
    // Guard against overwriting LOADED – async loaders (Coil) may resolve from
    // memory cache during composition, before this effect runs.
    LaunchedEffect(source) {
        if (!isLoading && !internalLoading) {
            when (source) {
                is ImageSource.Empty -> updateState(ImageState.EMPTY)
                is ImageSource.DrawableRes -> updateState(ImageState.LOADED)
                else -> {
                    if (currentState != ImageState.LOADED) {
                        updateState(ImageState.LOADING)
                    }
                }
            }
        }
    }

    val shape = RoundedCornerShape(cornerRadius)
    val contentScale = when (scaleType) {
        ImageScaleType.FIT -> ContentScale.Fit
        ImageScaleType.FILL, ImageScaleType.CROP -> ContentScale.Crop
        ImageScaleType.CENTER -> ContentScale.Inside
        ImageScaleType.STRETCH -> ContentScale.FillBounds
    }

    // Animation for fade in
    val imageAlpha by animateFloatAsState(
        targetValue = if (currentState == ImageState.LOADED) 1f else 0f,
        animationSpec = tween(durationMillis = ImageViewDefaults.FadeAnimationDuration),
        label = "imageAlpha"
    )

    // Apply sensible default sizing when no aspect ratio is provided,
    // but let caller-specified size modifiers (height, fillMaxHeight, etc.) take precedence.
    val effectiveModifier = if (aspectRatio <= 0) {
        modifier.then(
            Modifier.heightIn(min = ImageViewDefaults.DefaultHeight)
        )
    } else {
        modifier
    }

    Column(modifier = effectiveModifier) {
        // Label
        if (label.isNotEmpty()) {
            Text(text = label, style = labelTextStyle)
            Spacer(modifier = Modifier.height(labelGap))
        }

        // Image container - fills remaining space in Column
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (aspectRatio > 0) {
                        Modifier.aspectRatio(aspectRatio, matchHeightConstraintsFirst = false)
                    } else {
                        Modifier.weight(1f)
                    }
                )
                .clip(shape)
                .background(colors.backgroundColor, shape)
                .then(
                    if (borderWidth > 0.dp) {
                        Modifier.border(borderWidth, colors.borderColor(currentState), shape)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = if (enableFullScreenPreview) ripple() else null,
                    enabled = currentState == ImageState.LOADED ||
                            currentState == ImageState.EMPTY ||
                            currentState == ImageState.ERROR ||
                            (currentState == ImageState.LOADING && onImageClick != null)
                ) {
                    onImageClick?.invoke()
                    when (currentState) {
                        ImageState.LOADED -> {
                            if (enableFullScreenPreview) {
                                showFullScreenPreview(context, source)
                            }
                            onCapturedImageClick?.invoke()
                        }

                        ImageState.EMPTY, ImageState.ERROR -> {
                            if (enableCameraCapture) {
                                if (onCaptureRequest != null) {
                                    onCaptureRequest.invoke(captureConfig)
                                } else if (onImageClick == null) {
                                    PhotoCaptureManager.instance.capturePhoto(captureConfig) { result ->
                                        when (result) {
                                            is PhotoCaptureResult.Processing -> {
                                                internalLoading = true
                                            }
                                            is PhotoCaptureResult.Success -> {
                                                internalLoading = false
                                                onCaptureResult?.invoke(ImageSource.File(result.file))
                                            }
                                            is PhotoCaptureResult.Error -> {
                                                internalLoading = false
                                                updateState(ImageState.ERROR)
                                            }
                                            is PhotoCaptureResult.Cancelled -> {
                                                internalLoading = false
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                },
            contentAlignment = imageAlignment
        ) {
            // Content based on state
            when {
                source is ImageSource.Empty -> {
                    // Placeholder
                    if (currentState != ImageState.LOADING) {
                        PlaceholderContent(
                            text = placeholderText,
                            iconResId = placeholderIconResId,
                            iconSize = placeholderIconSize,
                            textSize = placeholderTextSize,
                            gap = placeholderGap,
                            iconColor = colors.placeholderIconColor(currentState),
                            textColor = colors.placeholderTextColor(currentState),
                            isError = currentState == ImageState.ERROR
                        )
                    }
                }

                else -> {
                    // Image loading
                    ImageContent(
                        source = source,
                        contentScale = contentScale,
                        cornerRadius = cornerRadius,
                        alignment = imageAlignment,
                        imageAlpha = imageAlpha,
                        onLoading = { updateState(ImageState.LOADING) },
                        onSuccess = { resolvedSource ->
                            updateState(ImageState.LOADED)
                            onCaptureResult?.invoke(resolvedSource)
                            onImageLoadResult?.invoke(true)
                        },
                        onError = {
                            updateState(ImageState.ERROR)
                            onImageLoadResult?.invoke(false)
                        }
                    )

                    // Error placeholder
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentState == ImageState.ERROR,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        PlaceholderContent(
                            text = "Failed to load",
                            iconResId = 0,
                            iconSize = placeholderIconSize,
                            textSize = placeholderTextSize,
                            gap = placeholderGap,
                            iconColor = colors.errorColor,
                            textColor = colors.errorColor,
                            isError = true
                        )
                    }
                }
            }

            // Loading indicator
            androidx.compose.animation.AnimatedVisibility(
                visible = currentState == ImageState.LOADING && showLoadingIndicator,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (loadingGifResId != 0) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(loadingGifResId)
                            .build(),
                        contentDescription = "Loading",
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Inside
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ImageViewDefaults.LoadingIndicatorSize),
                        color = colors.loadingColor,
                        strokeWidth = ImageViewDefaults.LoadingIndicatorStrokeWidth
                    )
                }
            }

            // Delete button
            androidx.compose.animation.AnimatedVisibility(
                visible = currentState == ImageState.LOADED && showDeleteButton,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(ImageViewDefaults.DeleteButtonMargin)
            ) {
                DeleteButton(
                    onClick = {
                        onDeleteClick?.invoke()
                    },
                    iconTint = colors.deleteIconTint,
                    backgroundColor = colors.deleteButtonBackground,
                    cornerRadius = (cornerRadius.value * 0.4f).coerceAtLeast(4f).dp
                )
            }

            val actionScroll = rememberScrollState()
            val chipCorner = (cornerRadius.value * 0.4f).coerceAtLeast(4f).dp
            androidx.compose.animation.AnimatedVisibility(
                visible = currentState == ImageState.LOADED && actionButtons.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd)
                    .padding(ImageViewDefaults.ActionButtonRowMargin)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
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
}

/**
 * Placeholder content when no image is loaded.
 */
@Composable
private fun PlaceholderContent(
    text: String,
    iconResId: Int,
    iconSize: Dp,
    textSize: TextUnit,
    gap: Dp,
    iconColor: Color,
    textColor: Color,
    isError: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (iconResId != 0) iconResId
                else if (isError) R.drawable.ic_clear
                else R.drawable.ic_add_photo
            ),
            contentDescription = if (isError) "Error" else "Add photo",
            modifier = Modifier.size(iconSize),
            tint = iconColor
        )

        Spacer(modifier = Modifier.height(gap))

        Text(
            text = text,
            style = TextStyle(
                fontSize = textSize,
                color = textColor
            )
        )
    }
}

/**
 * Delete button composable.
 */
@Composable
private fun DeleteButton(
    onClick: () -> Unit,
    iconTint: Color,
    backgroundColor: Color,
    cornerRadius: Dp,
) {
    Box(
        modifier = Modifier
            .size(ImageViewDefaults.DeleteButtonSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_clear),
            contentDescription = "Delete",
            modifier = Modifier
                .size(ImageViewDefaults.DeleteButtonSize - ImageViewDefaults.DeleteButtonPadding * 2),
            tint = iconTint
        )
    }
}

/**
 * Image content composable that handles different source types.
 */
@Composable
private fun ImageContent(
    source: ImageSource,
    contentScale: ContentScale,
    cornerRadius: Dp,
    alignment: Alignment,
    imageAlpha: Float,
    onLoading: () -> Unit,
    onSuccess: (ImageSource) -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)

    when (source) {
        is ImageSource.Bitmap -> {
            // Direct bitmap display
            LaunchedEffect(source) { onSuccess(source) }
            Image(
                bitmap = source.bitmap.asImageBitmap(),
                contentDescription = "Image",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlpha)
                    .clip(shape),
                contentScale = contentScale
            )
        }

        is ImageSource.Base64 -> {
            // Decode Base64 and display
            val bitmap = remember(source.base64String) {
                try {
                    val bytes = Base64.decode(source.base64String, Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            }

            LaunchedEffect(bitmap) {
                if (bitmap != null) onSuccess(source) else onError()
            }

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(imageAlpha)
                        .clip(shape),
                    contentScale = contentScale
                )
            }
        }

        else -> {
            // Use Coil for URL, File, Uri, FilePath, Drawable
            val model = remember(source) {
                when (source) {
                    is ImageSource.Url -> source.url
                    is ImageSource.File -> source.file
                    is ImageSource.FilePath -> File(source.path)
                    is ImageSource.Uri -> source.uri
                    is ImageSource.DrawableRes -> source.resId
                    else -> null
                }
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = "Image",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlpha)
                    .clip(shape),
                contentScale = contentScale,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Loading -> onLoading()
                        is AsyncImagePainter.State.Success -> onSuccess(source)
                        is AsyncImagePainter.State.Error -> onError()
                        else -> {}
                    }
                }
            )
        }
    }
}

/**
 * Shows full-screen preview of the image.
 */
private fun showFullScreenPreview(context: android.content.Context, source: ImageSource) {
    val builder = FullScreenImageViewer.Builder(context)

    when (source) {
        is ImageSource.File -> builder.setImageFile(source.file)
        is ImageSource.FilePath -> builder.setImageFile(File(source.path))
        is ImageSource.Url -> builder.setImageUrl(source.url)
        is ImageSource.Uri -> builder.setImageUri(source.uri)
        is ImageSource.DrawableRes -> builder.setImageDrawableRes(source.resId)
        is ImageSource.Bitmap -> builder.setImageBitmap(source.bitmap)
        is ImageSource.Base64 -> {
            try {
                val bytes = Base64.decode(source.base64String, Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) builder.setImageBitmap(bitmap)
            } catch (e: Exception) {
                return
            }
        }

        ImageSource.Empty -> return
    }

    builder.show()
}

/**
 * Convenience composable for displaying an image from a URL.
 */
@Composable
fun AdvancedImageView(
    url: String,
    modifier: Modifier = Modifier,
    scaleType: ImageScaleType = ImageScaleType.CROP,
    aspectRatio: Float = 0f,
    cornerRadius: Dp = ImageViewDefaults.CornerRadius,
    borderWidth: Dp = 0.dp,
    colors: ImageViewColors = ImageViewDefaults.displayOnlyColors(),
    label: String = "",
    labelGap: Dp = ImageViewDefaults.LabelGap,
    labelTextStyle: TextStyle = ImageViewDefaults.LabelTextStyle,
    showDeleteButton: Boolean = false,
    showLoadingIndicator: Boolean = true,
    loadingGifResId: Int = 0,
    enableFullScreenPreview: Boolean = true,
    enableCameraCapture: Boolean = false,
    captureConfig: PhotoCaptureConfig = PhotoCaptureConfig(),
    onCapturedImageClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    actionButtons: List<ActionButtonConfig> = emptyList(),
    onCaptureRequest: ((PhotoCaptureConfig) -> Unit)? = null,
    onCaptureResult: ((ImageSource) -> Unit)? = null,
    onImageLoadResult: ((Boolean) -> Unit)? = null,
) {
    AdvancedImageView(
        source = if (url.isNotBlank()) ImageSource.Url(url) else ImageSource.Empty,
        modifier = modifier,
        scaleType = scaleType,
        aspectRatio = aspectRatio,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        colors = colors,
        label = label,
        labelGap = labelGap,
        labelTextStyle = labelTextStyle,
        showDeleteButton = showDeleteButton,
        showLoadingIndicator = showLoadingIndicator,
        loadingGifResId = loadingGifResId,
        enableFullScreenPreview = enableFullScreenPreview,
        enableCameraCapture = enableCameraCapture,
        captureConfig = captureConfig,
        onCapturedImageClick = onCapturedImageClick,
        onImageClick = onImageClick,
        onDeleteClick = onDeleteClick,
        actionButtons = actionButtons,
        onCaptureRequest = onCaptureRequest,
        onCaptureResult = onCaptureResult,
        onImageLoadResult = onImageLoadResult,
    )
}

/**
 * Convenience composable for displaying an image from a File.
 */
@Composable
fun AdvancedImageView(
    file: File?,
    modifier: Modifier = Modifier,
    scaleType: ImageScaleType = ImageScaleType.CROP,
    aspectRatio: Float = 0f,
    cornerRadius: Dp = ImageViewDefaults.CornerRadius,
    borderWidth: Dp = ImageViewDefaults.BorderWidth,
    colors: ImageViewColors = ImageViewDefaults.colors(),
    label: String = "",
    labelGap: Dp = ImageViewDefaults.LabelGap,
    labelTextStyle: TextStyle = ImageViewDefaults.LabelTextStyle,
    placeholderText: String = ImageViewDefaults.PlaceholderText,
    placeholderIconResId: Int = 0,
    placeholderIconSize: Dp = ImageViewDefaults.PlaceholderIconSize,
    placeholderTextSize: TextUnit = ImageViewDefaults.PlaceholderTextSize,
    placeholderGap: Dp = ImageViewDefaults.PlaceholderGap,
    showDeleteButton: Boolean = true,
    showLoadingIndicator: Boolean = true,
    loadingGifResId: Int = 0,
    enableFullScreenPreview: Boolean = true,
    enableCameraCapture: Boolean = true,
    captureConfig: PhotoCaptureConfig = PhotoCaptureConfig(),
    isLoading: Boolean = false,
    onCapturedImageClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    actionButtons: List<ActionButtonConfig> = emptyList(),
    onCaptureRequest: ((PhotoCaptureConfig) -> Unit)? = null,
    onCaptureResult: ((ImageSource) -> Unit)? = null,
    onImageLoadResult: ((Boolean) -> Unit)? = null,
) {
    AdvancedImageView(
        source = file?.let { ImageSource.File(it) } ?: ImageSource.Empty,
        modifier = modifier,
        scaleType = scaleType,
        aspectRatio = aspectRatio,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        colors = colors,
        label = label,
        labelGap = labelGap,
        labelTextStyle = labelTextStyle,
        placeholderText = placeholderText,
        placeholderIconResId = placeholderIconResId,
        placeholderIconSize = placeholderIconSize,
        placeholderTextSize = placeholderTextSize,
        placeholderGap = placeholderGap,
        showDeleteButton = showDeleteButton,
        showLoadingIndicator = showLoadingIndicator,
        loadingGifResId = loadingGifResId,
        enableFullScreenPreview = enableFullScreenPreview,
        enableCameraCapture = enableCameraCapture,
        captureConfig = captureConfig,
        isLoading = isLoading,
        onCapturedImageClick = onCapturedImageClick,
        onImageClick = onImageClick,
        onDeleteClick = onDeleteClick,
        actionButtons = actionButtons,
        onCaptureRequest = onCaptureRequest,
        onCaptureResult = onCaptureResult,
        onImageLoadResult = onImageLoadResult,
    )
}

/**
 * Convenience composable for displaying an image from a Uri.
 */
@Composable
fun AdvancedImageView(
    uri: Uri?,
    modifier: Modifier = Modifier,
    scaleType: ImageScaleType = ImageScaleType.CROP,
    aspectRatio: Float = 0f,
    cornerRadius: Dp = ImageViewDefaults.CornerRadius,
    borderWidth: Dp = 0.dp,
    colors: ImageViewColors = ImageViewDefaults.displayOnlyColors(),
    showDeleteButton: Boolean = false,
    showLoadingIndicator: Boolean = true,
    loadingGifResId: Int = 0,
    enableFullScreenPreview: Boolean = true,
    enableCameraCapture: Boolean = false,
    captureConfig: PhotoCaptureConfig = PhotoCaptureConfig(),
    onCapturedImageClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    actionButtons: List<ActionButtonConfig> = emptyList(),
    onCaptureRequest: ((PhotoCaptureConfig) -> Unit)? = null,
    onCaptureResult: ((ImageSource) -> Unit)? = null,
    onImageLoadResult: ((Boolean) -> Unit)? = null,
) {
    AdvancedImageView(
        source = uri?.let { ImageSource.Uri(it) } ?: ImageSource.Empty,
        modifier = modifier,
        scaleType = scaleType,
        aspectRatio = aspectRatio,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        colors = colors,
        showDeleteButton = showDeleteButton,
        showLoadingIndicator = showLoadingIndicator,
        loadingGifResId = loadingGifResId,
        enableFullScreenPreview = enableFullScreenPreview,
        enableCameraCapture = enableCameraCapture,
        captureConfig = captureConfig,
        onCapturedImageClick = onCapturedImageClick,
        onImageClick = onImageClick,
        onDeleteClick = onDeleteClick,
        actionButtons = actionButtons,
        onCaptureRequest = onCaptureRequest,
        onCaptureResult = onCaptureResult,
        onImageLoadResult = onImageLoadResult,
    )
}

/**
 * Convenience composable for displaying an image from a Bitmap.
 */
@Composable
fun AdvancedImageView(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    scaleType: ImageScaleType = ImageScaleType.CROP,
    aspectRatio: Float = 0f,
    cornerRadius: Dp = ImageViewDefaults.CornerRadius,
    borderWidth: Dp = 0.dp,
    colors: ImageViewColors = ImageViewDefaults.displayOnlyColors(),
    showDeleteButton: Boolean = false,
    showLoadingIndicator: Boolean = true,
    loadingGifResId: Int = 0,
    enableFullScreenPreview: Boolean = true,
    enableCameraCapture: Boolean = false,
    captureConfig: PhotoCaptureConfig = PhotoCaptureConfig(),
    onCapturedImageClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    actionButtons: List<ActionButtonConfig> = emptyList(),
    onCaptureRequest: ((PhotoCaptureConfig) -> Unit)? = null,
    onCaptureResult: ((ImageSource) -> Unit)? = null,
    onImageLoadResult: ((Boolean) -> Unit)? = null,
) {
    AdvancedImageView(
        source = bitmap?.let { ImageSource.Bitmap(it) } ?: ImageSource.Empty,
        modifier = modifier,
        scaleType = scaleType,
        aspectRatio = aspectRatio,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        colors = colors,
        showDeleteButton = showDeleteButton,
        showLoadingIndicator = showLoadingIndicator,
        loadingGifResId = loadingGifResId,
        enableFullScreenPreview = enableFullScreenPreview,
        enableCameraCapture = enableCameraCapture,
        captureConfig = captureConfig,
        onCapturedImageClick = onCapturedImageClick,
        onImageClick = onImageClick,
        onDeleteClick = onDeleteClick,
        actionButtons = actionButtons,
        onCaptureRequest = onCaptureRequest,
        onCaptureResult = onCaptureResult,
        onImageLoadResult = onImageLoadResult,
    )
}

/**
 * Convenience composable for displaying an image from a drawable resource.
 */
@Composable
fun AdvancedImageView(
    drawableResId: Int,
    modifier: Modifier = Modifier,
    scaleType: ImageScaleType = ImageScaleType.CROP,
    aspectRatio: Float = 0f,
    imageAlignment: Alignment = Alignment.Center,
    cornerRadius: Dp = ImageViewDefaults.CornerRadius,
    borderWidth: Dp = 0.dp,
    colors: ImageViewColors = ImageViewDefaults.displayOnlyColors(),
    showDeleteButton: Boolean = false,
    showLoadingIndicator: Boolean = true,
    loadingGifResId: Int = 0,
    enableFullScreenPreview: Boolean = true,
    enableCameraCapture: Boolean = false,
    captureConfig: PhotoCaptureConfig = PhotoCaptureConfig(),
    onCapturedImageClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    actionButtons: List<ActionButtonConfig> = emptyList(),
    onCaptureRequest: ((PhotoCaptureConfig) -> Unit)? = null,
    onCaptureResult: ((ImageSource) -> Unit)? = null,
) {
    AdvancedImageView(
        source = if (drawableResId != 0) ImageSource.DrawableRes(drawableResId) else ImageSource.Empty,
        modifier = modifier,
        scaleType = scaleType,
        aspectRatio = aspectRatio,
        imageAlignment = imageAlignment,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        colors = colors,
        showDeleteButton = showDeleteButton,
        showLoadingIndicator = showLoadingIndicator,
        loadingGifResId = loadingGifResId,
        enableFullScreenPreview = enableFullScreenPreview,
        enableCameraCapture = enableCameraCapture,
        captureConfig = captureConfig,
        onCapturedImageClick = onCapturedImageClick,
        onImageClick = onImageClick,
        actionButtons = actionButtons,
        onCaptureRequest = onCaptureRequest,
        onCaptureResult = onCaptureResult,
    )
}

/**
 * State holder for AdvancedImageView with mutable source.
 *
 * Usage:
 * ```kotlin
 * val imageState = rememberAdvancedImageViewState()
 *
 * AdvancedImageView(
 *     state = imageState,
 *     ...
 * )
 *
 * // Load image
 * imageState.loadFromUrl("https://...")
 *
 * // Clear image
 * imageState.clear()
 * ```
 */
class AdvancedImageViewState {
    var source by mutableStateOf<ImageSource>(ImageSource.Empty)
        private set

    var currentState by mutableStateOf(ImageState.EMPTY)
        internal set

    // Force loading state (for showing loading during processing)
    var isForceLoading by mutableStateOf(false)
        private set

    /**
     * Show loading state (e.g., during photo processing).
     * Call this when PhotoCaptureResult.Processing is received.
     */
    fun showLoading() {
        isForceLoading = true
        currentState = ImageState.LOADING
    }

    /**
     * Hide the forced loading state.
     * Usually called automatically when loadFromFile/loadFromUrl is called.
     */
    fun hideLoading() {
        isForceLoading = false
    }

    fun loadFromUrl(url: String) {
        isForceLoading = false
        source = if (url.isNotBlank()) ImageSource.Url(url) else ImageSource.Empty
    }

    fun loadFromFile(file: File) {
        isForceLoading = false
        source = if (file.exists()) ImageSource.File(file) else ImageSource.Empty
    }

    fun loadFromFilePath(path: String) {
        isForceLoading = false
        source = ImageSource.FilePath(path)
    }

    fun loadFromUri(uri: Uri) {
        isForceLoading = false
        source = ImageSource.Uri(uri)
    }

    fun loadFromBitmap(bitmap: Bitmap) {
        isForceLoading = false
        source = ImageSource.Bitmap(bitmap)
    }

    fun loadFromDrawableRes(resId: Int) {
        isForceLoading = false
        source = if (resId != 0) ImageSource.DrawableRes(resId) else ImageSource.Empty
    }

    fun loadFromBase64(base64String: String) {
        isForceLoading = false
        source =
            if (base64String.isNotBlank()) ImageSource.Base64(base64String) else ImageSource.Empty
    }

    fun clear() {
        isForceLoading = false
        source = ImageSource.Empty
        currentState = ImageState.EMPTY
    }

    fun getImageFile(): File? = when (val current = source) {
        is ImageSource.File -> current.file
        is ImageSource.FilePath -> File(current.path)
        else -> null
    }

    fun getImageBitmap(): Bitmap? = when (val current = source) {
        is ImageSource.Bitmap -> current.bitmap
        else -> null
    }

    fun getImageUri(): Uri? = when (val current = source) {
        is ImageSource.Uri -> current.uri
        else -> null
    }

    fun getImageUrl(): String? = when (val current = source) {
        is ImageSource.Url -> current.url
        else -> null
    }

    fun hasImage(): Boolean = currentState == ImageState.LOADED

    fun isLoading(): Boolean = currentState == ImageState.LOADING

    fun isError(): Boolean = currentState == ImageState.ERROR

    fun isEmpty(): Boolean = currentState == ImageState.EMPTY
}

/**
 * Remember and create an AdvancedImageViewState.
 */
@Composable
fun rememberAdvancedImageViewState(
    initialSource: ImageSource = ImageSource.Empty,
): AdvancedImageViewState {
    return remember {
        AdvancedImageViewState().apply {
            if (initialSource != ImageSource.Empty) {
                when (initialSource) {
                    is ImageSource.Url -> loadFromUrl(initialSource.url)
                    is ImageSource.File -> loadFromFile(initialSource.file)
                    is ImageSource.FilePath -> loadFromFilePath(initialSource.path)
                    is ImageSource.Uri -> loadFromUri(initialSource.uri)
                    is ImageSource.Bitmap -> loadFromBitmap(initialSource.bitmap)
                    is ImageSource.DrawableRes -> loadFromDrawableRes(initialSource.resId)
                    is ImageSource.Base64 -> loadFromBase64(initialSource.base64String)
                    ImageSource.Empty -> {}
                }
            }
        }
    }
}

/**
 * AdvancedImageView with state holder.
 */
@Composable
fun AdvancedImageView(
    state: AdvancedImageViewState,
    modifier: Modifier = Modifier,
    scaleType: ImageScaleType = ImageScaleType.CROP,
    aspectRatio: Float = 0f,
    cornerRadius: Dp = ImageViewDefaults.CornerRadius,
    borderWidth: Dp = ImageViewDefaults.BorderWidth,
    colors: ImageViewColors = ImageViewDefaults.colors(),
    label: String = "",
    labelGap: Dp = ImageViewDefaults.LabelGap,
    labelTextStyle: TextStyle = ImageViewDefaults.LabelTextStyle,
    placeholderText: String = ImageViewDefaults.PlaceholderText,
    placeholderIconResId: Int = 0,
    placeholderIconSize: Dp = ImageViewDefaults.PlaceholderIconSize,
    placeholderTextSize: TextUnit = ImageViewDefaults.PlaceholderTextSize,
    placeholderGap: Dp = ImageViewDefaults.PlaceholderGap,
    showDeleteButton: Boolean = true,
    showLoadingIndicator: Boolean = true,
    loadingGifResId: Int = 0,
    enableFullScreenPreview: Boolean = true,
    enableCameraCapture: Boolean = true,
    captureConfig: PhotoCaptureConfig = PhotoCaptureConfig(),
    onCapturedImageClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    actionButtons: List<ActionButtonConfig> = emptyList(),
    onCaptureRequest: ((PhotoCaptureConfig) -> Unit)? = null,
    onCaptureResult: ((ImageSource) -> Unit)? = null,
    onImageLoadResult: ((Boolean) -> Unit)? = null,
    onStateChange: ((ImageState) -> Unit)? = null,
) {
    AdvancedImageView(
        source = state.source,
        modifier = modifier,
        scaleType = scaleType,
        aspectRatio = aspectRatio,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        colors = colors,
        label = label,
        labelGap = labelGap,
        labelTextStyle = labelTextStyle,
        placeholderText = placeholderText,
        placeholderIconResId = placeholderIconResId,
        placeholderIconSize = placeholderIconSize,
        placeholderTextSize = placeholderTextSize,
        placeholderGap = placeholderGap,
        showDeleteButton = showDeleteButton,
        showLoadingIndicator = showLoadingIndicator,
        loadingGifResId = loadingGifResId,
        enableFullScreenPreview = enableFullScreenPreview,
        enableCameraCapture = enableCameraCapture,
        captureConfig = captureConfig,
        onCapturedImageClick = onCapturedImageClick,
        onImageClick = onImageClick,
        onDeleteClick = {
            state.clear()
            onDeleteClick?.invoke()
        },
        actionButtons = actionButtons,
        onCaptureRequest = onCaptureRequest,
        onCaptureResult = onCaptureResult,
        onImageLoadResult = { success ->
            if (success) state.hideLoading()
            onImageLoadResult?.invoke(success)
        },
        onStateChange = { newState ->
            state.currentState = newState
            onStateChange?.invoke(newState)
        },
        isLoading = state.isForceLoading,
    )
}
