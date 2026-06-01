@file:Suppress("unused")

package `in`.fivedegree.volineui.compose

import android.annotation.SuppressLint
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.fivedegree.volineui.R
import `in`.fivedegree.volineui.PhotoCaptureManager
import `in`.fivedegree.volineui.imageview.ActionButtonConfig
import `in`.fivedegree.volineui.imageview.FullScreenImageViewer
import `in`.fivedegree.volineui.imageview.ImageCarouselColors
import `in`.fivedegree.volineui.imageview.ImageCarouselDefaults
import `in`.fivedegree.volineui.imageview.ImageSource
import `in`.fivedegree.volineui.imageview.ImageViewDefaults
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureConfig
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureResult
import java.io.File

/**
 * Image Carousel component for Jetpack Compose with horizontal scrolling images.
 *
 * Features:
 * - Horizontal scrolling image carousel
 * - Add button to add new images (disabled while processing)
 * - Delete button on each image
 * - Optional bottom-right action chips per slide (horizontally scrollable)
 * - Image indicators (dots)
 * - Full-screen preview on tap
 * - Processing state with loading placeholder
 * - Customizable appearance (size, colors, border, corner radius)
 * - Label support
 * - Max image count limit
 * - Images displayed with CROP scale type for consistent appearance
 *
 * Usage (simplified with captureConfig + onCaptureResult):
 * ```kotlin
 * var files by remember { mutableStateOf<List<File>>(emptyList()) }
 *
 * ImageCarousel(
 *     files = files,
 *     label = "Photos",
 *     maxImageCount = 5,
 *     captureConfig = PhotoCaptureConfig("Watermark Text"),
 *     onCaptureResult = { file -> files = files + file },
 *     onImageDelete = { index ->
 *         files = files.toMutableList().apply { removeAt(index) }
 *     },
 * )
 * ```
 *
 * The carousel internally manages the loading placeholder and add button
 * while [PhotoCaptureManager] is processing. If you need full manual control
 * use [onAddClick] instead (it takes precedence over the built-in capture).
 *
 * @param files List of image files to display
 * @param label Optional label text above the carousel
 * @param labelGap Gap between label and carousel
 * @param labelTextStyle Full typography for the label (including color when you override the default)
 * @param carouselHeight Height of the carousel container (default 200dp)
 * @param itemWidth Width of each carousel item
 * @param itemSpacing Spacing between items
 * @param cornerRadius Corner radius for items
 * @param borderWidth Border width for items
 * @param colors Color configuration for the carousel chrome (not the label; use [labelTextStyle] for label appearance)
 * @param showIndicators Whether to show indicators
 * @param showDeleteIcon Whether to show the delete icon for the item at each index
 * @param enableFullScreen Whether to enable full-screen preview on tap
 * @param maxImageCount Maximum number of images allowed
 * @param isProcessing Whether an image is currently being processed (shows loading placeholder, disables add button)
 * @param onImageClick Callback when an image is clicked
 * @param onImageDelete Callback when an image is deleted (receives the index)
 * @param onAddClick Callback when add button is clicked (takes precedence over built-in capture)
 * @param captureConfig Configuration for built-in photo capture (used when [onAddClick] is null)
 * @param onCaptureResult Callback with captured [File] after a successful capture
 * @param onCaptureError Optional callback when capture fails
 * @param actionButtons Factory for bottom-right action chips per item index (shown when image is loaded)
 * @param modifier Modifier for the component (width fills max by default)
 */
@Composable
fun ImageCarousel(
	files: List<File>,
    // Label
	label: String = "",
	labelGap: Dp = ImageCarouselDefaults.LabelGap,
	labelTextStyle: TextStyle = ImageCarouselDefaults.LabelTextStyle,
    // Carousel dimensions
	carouselHeight: Dp = ImageCarouselDefaults.CarouselHeight,
    // Item dimensions
	itemWidth: Dp = ImageCarouselDefaults.ItemWidth,
	itemSpacing: Dp = ImageCarouselDefaults.ItemSpacing,
	cornerRadius: Dp = ImageCarouselDefaults.CornerRadius,
	borderWidth: Dp = ImageCarouselDefaults.BorderWidth,
    // Colors
	colors: ImageCarouselColors = ImageCarouselDefaults.colors(),
    // Features
	showIndicators: Boolean = true,
	showDeleteIcon: (Int) -> Boolean = { true },
	enableFullScreen: Boolean = true,
	maxImageCount: Int = ImageCarouselDefaults.DefaultMaxImageCount,
    // Processing state - shows loading placeholder and disables add button
	isProcessing: Boolean = false,
    // Callbacks
	onImageClick: ((Int) -> Unit)? = null,
	onImageDelete: ((Int) -> Unit)? = null,
	onAddClick: (() -> Unit)? = null,
	captureConfig: PhotoCaptureConfig = PhotoCaptureConfig(),
	onCaptureResult: ((File) -> Unit)? = null,
	onCaptureError: ((String) -> Unit)? = null,
	actionButtons: (Int) -> List<ActionButtonConfig> = { emptyList() },
	@SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    var internalProcessing by remember { mutableStateOf(false) }
    val effectiveProcessing = isProcessing || internalProcessing

    // Convert files to ImageSource
    val imageSources = remember(files) {
        files.map { ImageSource.File(it) }
    }

    // Calculate current index based on scroll position
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val itemSpacingPx = with(density) { itemSpacing.toPx() }
    val currentIndex by remember(files.size) {
        derivedStateOf {
            if (files.isEmpty()) 0
            else {
                val scrollX = scrollState.value
                ((scrollX + itemWidthPx / 2) / (itemWidthPx + itemSpacingPx)).toInt()
                    .coerceIn(0, files.size - 1)
            }
        }
    }

    val processingCount = if (effectiveProcessing) 1 else 0
    val canAddMore = !effectiveProcessing && files.size < maxImageCount
    val totalItemCount = files.size + processingCount

    @Suppress("AssignedValueIsNeverRead") val handleAddClick: () -> Unit = {
        if (onAddClick != null) {
            onAddClick()
        } else if (onCaptureResult != null) {
            PhotoCaptureManager.instance.capturePhoto(captureConfig) { result ->
                when (result) {
                    is PhotoCaptureResult.Processing -> {
                        internalProcessing = true
                    }
                    is PhotoCaptureResult.Success -> {
                        internalProcessing = false
                        onCaptureResult(result.file)
                    }
                    is PhotoCaptureResult.Error -> {
                        internalProcessing = false
                        onCaptureError?.invoke(result.message)
                    }
                    is PhotoCaptureResult.Cancelled -> {
                        internalProcessing = false
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        if (label.isNotEmpty()) {
            Text(text = label, style = labelTextStyle)
            Spacer(modifier = Modifier.height(labelGap))
        }

        // Carousel container with indicators overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight)
        ) {
            // Horizontal scroll with images
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image items
                files.forEachIndexed { index, file ->
                    key(file.absolutePath) {
                        CarouselImageItem(
                            source = ImageSource.File(file),
                            modifier = Modifier.size(itemWidth, carouselHeight),
                            cornerRadius = cornerRadius,
                            borderWidth = borderWidth,
                            colors = colors,
                            showDeleteIcon = showDeleteIcon(index),
                            actionButtons = actionButtons(index),
                            onImageClick = {
                                if (enableFullScreen) {
                                    FullScreenImageViewer.showCarouselCompose(
                                        context,
                                        imageSources,
                                        index
                                    )
                                }
                                onImageClick?.invoke(index)
                            },
                            onDeleteClick = {
                                onImageDelete?.invoke(index)
                            }
                        )
                    }
                }

                // Processing placeholder (loading indicator)
                if (effectiveProcessing) {
                    ProcessingPlaceholder(
                        modifier = Modifier.size(itemWidth, carouselHeight),
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        colors = colors
                    )
                }

                // Add button
                if (canAddMore) {
                    AddButton(
                        modifier = Modifier.size(itemWidth, carouselHeight),
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        colors = colors,
                        onClick = handleAddClick
                    )
                }
            }

            // Indicators overlay (bottom center)
            if (showIndicators && totalItemCount > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ImageCarouselDefaults.IndicatorBottomMargin),
                    horizontalArrangement = Arrangement.spacedBy(ImageCarouselDefaults.IndicatorSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicators for loaded images
                    files.forEachIndexed { index, _ ->
                        IndicatorDot(
                            isActive = index == currentIndex,
                            activeColor = colors.indicatorActiveColor,
                            inactiveColor = colors.indicatorInactiveColor
                        )
                    }
                    // Indicator for processing item
                    if (effectiveProcessing) {
                        IndicatorDot(
                            isActive = false,
                            activeColor = colors.indicatorActiveColor,
                            inactiveColor = colors.loadingColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Image Carousel for displaying URL-based images.
 *
 * @param urls List of image URLs to display
 * @param onUrlsChange Callback when URLs list changes (after delete), null for read-only
 * @param label Optional label text above the carousel
 * @param labelGap Gap between label and carousel
 * @param labelTextStyle Full typography for the label (including color when you override the default)
 * @param carouselHeight Height of the carousel container (default 200dp)
 * @param itemWidth Width of each carousel item
 * @param itemSpacing Spacing between items
 * @param cornerRadius Corner radius for items
 * @param borderWidth Border width for items
 * @param colors Color configuration for the carousel chrome (not the label; use [labelTextStyle] for label appearance)
 * @param showIndicators Whether to show indicators
 * @param showDeleteIcon Whether to show the delete icon for the item at each index (ignored when read-only)
 * @param enableFullScreen Whether to enable full-screen preview on tap
 * @param maxImageCount Maximum number of images allowed
 * @param onImageClick Callback when an image is clicked
 * @param onImageDelete Callback when an image is deleted
 * @param onAddClick Callback when add button is clicked
 * @param actionButtons Factory for bottom-right action chips per item index (shown when image is loaded)
 * @param modifier Modifier for the component (width fills max by default)
 */
@Composable
fun ImageCarousel(
    urls: List<String>,
    onUrlsChange: ((List<String>) -> Unit)? = null,
    // Label
    label: String = "",
    labelGap: Dp = ImageCarouselDefaults.LabelGap,
    labelTextStyle: TextStyle = ImageCarouselDefaults.LabelTextStyle,
    // Carousel dimensions
    carouselHeight: Dp = ImageCarouselDefaults.CarouselHeight,
    // Item dimensions
    itemWidth: Dp = ImageCarouselDefaults.ItemWidth,
    itemSpacing: Dp = ImageCarouselDefaults.ItemSpacing,
    cornerRadius: Dp = ImageCarouselDefaults.CornerRadius,
    borderWidth: Dp = ImageCarouselDefaults.BorderWidth,
    // Colors
    colors: ImageCarouselColors = ImageCarouselDefaults.colors(),
    // Features
    showIndicators: Boolean = true,
    showDeleteIcon: (Int) -> Boolean = { onUrlsChange != null },
    enableFullScreen: Boolean = true,
    maxImageCount: Int = ImageCarouselDefaults.DefaultMaxImageCount,
    // Callbacks
    onImageClick: ((Int) -> Unit)? = null,
    onImageDelete: ((Int) -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
    actionButtons: (Int) -> List<ActionButtonConfig> = { emptyList() },
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val canDelete = onUrlsChange != null

    // Convert URLs to ImageSource
    val imageSources = remember(urls) {
        urls.map { ImageSource.Url(it) }
    }

    // Calculate current index based on scroll position
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val itemSpacingPx = with(density) { itemSpacing.toPx() }
    val currentIndex by remember(urls.size) {
        derivedStateOf {
            if (urls.isEmpty()) 0
            else {
                val scrollX = scrollState.value
                ((scrollX + itemWidthPx / 2) / (itemWidthPx + itemSpacingPx)).toInt()
                    .coerceIn(0, urls.size - 1)
            }
        }
    }

    val canAddMore = urls.size < maxImageCount

    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        if (label.isNotEmpty()) {
            Text(text = label, style = labelTextStyle)
            Spacer(modifier = Modifier.height(labelGap))
        }

        // Carousel container with indicators overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight)
        ) {
            // Horizontal scroll with images
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image items
                urls.forEachIndexed { index, url ->
                    key(url, index) {
                        CarouselImageItem(
                            source = ImageSource.Url(url),
                            modifier = Modifier.size(itemWidth, carouselHeight),
                            cornerRadius = cornerRadius,
                            borderWidth = borderWidth,
                            colors = colors,
                            showDeleteIcon = canDelete && showDeleteIcon(index),
                            actionButtons = actionButtons(index),
                            onImageClick = {
                                if (enableFullScreen) {
                                    FullScreenImageViewer.showCarouselCompose(
                                        context,
                                        imageSources,
                                        index
                                    )
                                }
                                onImageClick?.invoke(index)
                            },
                            onDeleteClick = {
                                val newUrls = urls.toMutableList().apply { removeAt(index) }
                                onUrlsChange?.invoke(newUrls)
                                onImageDelete?.invoke(index)
                            }
                        )
                    }
                }

                // Add button
                if (canAddMore && onAddClick != null) {
                    AddButton(
                        modifier = Modifier.size(itemWidth, carouselHeight),
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        colors = colors,
                        onClick = { onAddClick() }
                    )
                }
            }

            // Indicators overlay (bottom center)
            if (showIndicators && urls.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ImageCarouselDefaults.IndicatorBottomMargin),
                    horizontalArrangement = Arrangement.spacedBy(ImageCarouselDefaults.IndicatorSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    urls.forEachIndexed { index, _ ->
                        IndicatorDot(
                            isActive = index == currentIndex,
                            activeColor = colors.indicatorActiveColor,
                            inactiveColor = colors.indicatorInactiveColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single image item in the carousel.
 */
@Composable
private fun CarouselImageItem(
    source: ImageSource,
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    borderWidth: Dp,
    colors: ImageCarouselColors,
    showDeleteIcon: Boolean,
    actionButtons: List<ActionButtonConfig> = emptyList(),
    onImageClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)
    val itemActionScroll = rememberScrollState()
    var isLoading by remember(source) { mutableStateOf(true) }
    var isLoaded by remember(source) { mutableStateOf(false) }

    val imageAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = ImageCarouselDefaults.FadeAnimationDuration),
        label = "imageAlpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.backgroundColor, shape)
            .border(borderWidth, colors.borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onImageClick() },
        contentAlignment = Alignment.Center
    ) {
        // Image content
        when (source) {
            is ImageSource.Bitmap -> {
                androidx.compose.runtime.LaunchedEffect(source) {
                    isLoading = false
                    isLoaded = true
                }
                Image(
                    bitmap = source.bitmap.asImageBitmap(),
                    contentDescription = "Carousel image",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(imageAlpha)
                        .clip(shape),
                    contentScale = ContentScale.Crop
                )
            }

            is ImageSource.DrawableRes -> {
                androidx.compose.runtime.LaunchedEffect(source) {
                    isLoading = false
                    isLoaded = true
                }
                Image(
                    painter = painterResource(id = source.resId),
                    contentDescription = "Carousel image",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(imageAlpha)
                        .clip(shape),
                    contentScale = ContentScale.Crop
                )
            }

            is ImageSource.Base64 -> {
                val bitmap = remember(source.base64String) {
                    try {
                        val bytes = Base64.decode(source.base64String, Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }

                androidx.compose.runtime.LaunchedEffect(bitmap) {
                    isLoading = false
                    isLoaded = bitmap != null
                }

                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Carousel image",
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(imageAlpha)
                            .clip(shape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            else -> {
                // Use Coil for URL, File, FilePath, Uri
                val model = remember(source) {
                    when (source) {
                        is ImageSource.Url -> source.url
                        is ImageSource.File -> source.file
                        is ImageSource.FilePath -> File(source.path)
                        is ImageSource.Uri -> source.uri
                        else -> null
                    }
                }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(model)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Carousel image",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(imageAlpha)
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Loading -> {
                                isLoading = true
                                isLoaded = false
                            }
                            is AsyncImagePainter.State.Success -> {
                                isLoading = false
                                isLoaded = true
                            }
                            is AsyncImagePainter.State.Error -> {
                                isLoading = false
                                isLoaded = false
                            }
                            else -> {}
                        }
                    }
                )
            }
        }

        // Loading indicator
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(ImageCarouselDefaults.LoadingIndicatorSize),
                color = colors.loadingColor,
                strokeWidth = ImageCarouselDefaults.LoadingIndicatorStrokeWidth
            )
        }

        // Delete button
        if (showDeleteIcon) {
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(ImageCarouselDefaults.DeleteButtonMargin)
            ) {
                Box(
                    modifier = Modifier
                        .size(ImageCarouselDefaults.DeleteButtonSize)
                        .clip(RoundedCornerShape((cornerRadius.value * 0.5f).coerceAtLeast(4f).dp))
                        .background(colors.deleteButtonBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clear),
                        contentDescription = "Delete",
                        modifier = Modifier.size(
                            ImageCarouselDefaults.DeleteButtonSize -
                                    ImageCarouselDefaults.DeleteButtonPadding * 2
                        ),
                        tint = colors.deleteIconTint
                    )
                }
            }
        }

        val actionChipCorner = (cornerRadius.value * 0.4f).coerceAtLeast(4f).dp
        AnimatedVisibility(
            visible = isLoaded && actionButtons.isNotEmpty(),
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
                    modifier = Modifier.horizontalScroll(itemActionScroll),
                    horizontalArrangement = Arrangement.spacedBy(ImageViewDefaults.ActionButtonSpacing),
                ) {
                    actionButtons.forEach { cfg ->
                        ActionButtonChip(config = cfg, cornerRadius = actionChipCorner)
                    }
                }
            }
        }
    }
}

/**
 * Add button for the carousel.
 */
@Composable
private fun AddButton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    borderWidth: Dp,
    colors: ImageCarouselColors,
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
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_add_photo),
            contentDescription = "Add image",
            modifier = Modifier
                .size(ImageCarouselDefaults.AddButtonIconSize)
                .alpha(0.6f),
            tint = colors.addButtonIconColor
        )
    }
}

/**
 * Processing placeholder showing loading indicator while image is being processed.
 */
@Composable
private fun ProcessingPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    borderWidth: Dp,
    colors: ImageCarouselColors,
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.backgroundColor, shape)
            .border(borderWidth, colors.borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(ImageCarouselDefaults.LoadingIndicatorSize),
            color = colors.loadingColor,
            strokeWidth = ImageCarouselDefaults.LoadingIndicatorStrokeWidth
        )
    }
}

/**
 * Indicator dot for the carousel.
 */
@Composable
private fun IndicatorDot(
    isActive: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .size(ImageCarouselDefaults.IndicatorSize)
            .clip(CircleShape)
            .background(if (isActive) activeColor else inactiveColor)
    )
}
