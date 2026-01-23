package com.cropintellix.volineuiandroid.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropintellix.volineui.PhotoCaptureManager
import com.cropintellix.volineui.compose.AdvancedImageView
import com.cropintellix.volineui.compose.AdvancedImageViewFromResource
import com.cropintellix.volineui.compose.rememberAdvancedImageViewState
import com.cropintellix.volineui.imageview.ImageScaleType
import com.cropintellix.volineui.imageview.ImageSource
import com.cropintellix.volineui.imageview.ImageState
import com.cropintellix.volineui.imageview.ImageViewDefaults
import com.cropintellix.volineui.photocapturemanager.PhotoCaptureConfig
import com.cropintellix.volineui.photocapturemanager.PhotoCaptureResult
import com.cropintellix.volineuiandroid.R
import com.cropintellix.volineuiandroid.ui.theme.AppTheme
import java.io.File

/**
 * Example activity demonstrating the Compose AdvancedImageView component.
 * Shows various configurations and features of the AdvancedImageView.
 */
class ComposeImageViewExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ImageViewExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageViewExamplesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Compose ImageView Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Basic Usage
        SectionTitle("Basic Usage")
        BasicUsageExample()

        HorizontalDivider()

        // With State Management
        SectionTitle("State Management")
        StateManagementExample()

        HorizontalDivider()

        // Camera Capture Integration
        SectionTitle("Camera Capture Integration")
        CameraCaptureExample()

        HorizontalDivider()

        // File Loading with Full Features
        SectionTitle("File Loading (Full Features)")
        FileLoadingFullFeaturesExample()

        HorizontalDivider()

        // Scale Types
        SectionTitle("Scale Types")
        ScaleTypesExample()

        HorizontalDivider()

        // Corner Radius & Border
        SectionTitle("Corner Radius & Border Styles")
        CornerBorderExample()

        HorizontalDivider()

        // Aspect Ratios
        SectionTitle("Aspect Ratios")
        AspectRatioExample()

        HorizontalDivider()

        // Labels
        SectionTitle("With Labels")
        LabelsExample()

        HorizontalDivider()

        // Custom Placeholders
        SectionTitle("Custom Placeholders")
        CustomPlaceholderExample()

        HorizontalDivider()

        // Custom Colors
        SectionTitle("Custom Colors")
        CustomColorsExample()

        HorizontalDivider()

        // URL Loading
        SectionTitle("Load from URL")
        UrlLoadingExample()

        HorizontalDivider()

        // Drawable Resource
        SectionTitle("Load from Drawable Resource")
        DrawableResourceExample()

        HorizontalDivider()

        // Feature Toggles
        SectionTitle("Feature Toggles")
        FeatureTogglesExample()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF666666)
    )
}

@Composable
private fun BasicUsageExample() {
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Default empty state with placeholder:",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )
        
        AdvancedImageView(
            source = ImageSource.Empty,
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = 4f / 3f,
            onCaptureClick = {
                Toast.makeText(context, "Capture clicked!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun StateManagementExample() {
    val context = LocalContext.current
    val imageState = rememberAdvancedImageViewState()
    var stateText by remember { mutableStateOf("EMPTY") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Current state: $stateText",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        AdvancedImageView(
            state = imageState,
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = 16f / 9f,
            label = "Managed Image",
            onStateChange = { state ->
                stateText = state.name
            },
            onCaptureClick = {
                PhotoCaptureManager.instance.capturePhoto(PhotoCaptureConfig()) { result ->
                    when (result) {
                        is PhotoCaptureResult.Success -> imageState.loadFromFile(result.file)
                        is PhotoCaptureResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        else -> {}
                    }
                }
            },
            onDeleteClick = {
                Toast.makeText(context, "Image cleared", Toast.LENGTH_SHORT).show()
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    imageState.loadFromUrl("https://picsum.photos/800/450?random=${System.currentTimeMillis()}")
                }
            ) {
                Text("Load URL")
            }

            Button(
                onClick = { imageState.clear() }
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun CameraCaptureExample() {
    val context = LocalContext.current
    val imageState1 = rememberAdvancedImageViewState()
    val imageState2 = rememberAdvancedImageViewState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tap the placeholder to capture photo:",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdvancedImageView(
                state = imageState1,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                label = "Photo 1",
                cornerRadius = 8.dp,
                onCaptureClick = {
                    PhotoCaptureManager.instance.capturePhoto(PhotoCaptureConfig("Photo 1")) { result ->
                        when (result) {
                            is PhotoCaptureResult.Success -> imageState1.loadFromFile(result.file)
                            is PhotoCaptureResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            else -> {}
                        }
                    }
                }
            )

            AdvancedImageView(
                state = imageState2,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                label = "Photo 2",
                cornerRadius = 8.dp,
                onCaptureClick = {
                    PhotoCaptureManager.instance.capturePhoto(PhotoCaptureConfig("Photo 2")) { result ->
                        when (result) {
                            is PhotoCaptureResult.Success -> imageState2.loadFromFile(result.file)
                            is PhotoCaptureResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            else -> {}
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FileLoadingFullFeaturesExample() {
    val context = LocalContext.current
    var capturedFile by remember { mutableStateOf<File?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Load from file with custom placeholder, clickable, deletable, and full-screen preview:",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        AdvancedImageView(
            file = capturedFile,
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = 4f / 3f,
            scaleType = ImageScaleType.CROP,
            cornerRadius = 16.dp,
            borderWidth = 2.dp,
            colors = ImageViewDefaults.colors(
                // Custom placeholder colors
                backgroundColor = Color(0xFFFCE4EC),
                borderColor = Color(0xFFE91E63),
                placeholderIconColor = Color(0xFFC2185B),
                placeholderTextColor = Color(0xFFC2185B),
                // Custom delete button colors
                deleteIconTint = Color.White,
                deleteButtonBackground = Color(0xFFE91E63)
            ),
            // Label configuration
            label = "Document Photo",
            labelGap = 8.dp,
            labelTextSize = 16.sp,
            labelFontWeight = FontWeight.SemiBold,
            // Custom placeholder
            placeholderText = "Tap to capture document",
            placeholderIconSize = 48.dp,
            placeholderTextSize = 14.sp,
            placeholderGap = 12.dp,
            // Features enabled
            showDeleteButton = true,
            showLoadingIndicator = true,
            enableFullScreenPreview = true,
            enableCameraCapture = true,
            // Callbacks
            onImageClick = {
                Toast.makeText(context, "Image clicked - opening fullscreen", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = {
                capturedFile = null
                Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
            },
            onCaptureClick = {
                PhotoCaptureManager.instance.capturePhoto(
                    PhotoCaptureConfig(watermarkText = "Document")
                ) { result ->
                    when (result) {
                        is PhotoCaptureResult.Success -> {
                            capturedFile = result.file
                            Toast.makeText(context, "Photo captured: ${result.file.name}", Toast.LENGTH_SHORT).show()
                        }
                        is PhotoCaptureResult.Error -> {
                            Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                        }
                        is PhotoCaptureResult.Cancelled -> {
                            Toast.makeText(context, "Capture cancelled", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            },
            onImageLoadResult = { success ->
                if (success) {
                    Toast.makeText(context, "Image loaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Helper buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    PhotoCaptureManager.instance.capturePhoto(
                        PhotoCaptureConfig(watermarkText = "Document")
                    ) { result ->
                        when (result) {
                            is PhotoCaptureResult.Success -> capturedFile = result.file
                            is PhotoCaptureResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            else -> {}
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Capture Photo")
            }

            Button(
                onClick = {
                    PhotoCaptureManager.instance.pickPhotoFromGallery(PhotoCaptureConfig()) { result ->
                        when (result) {
                            is PhotoCaptureResult.Success -> capturedFile = result.file
                            is PhotoCaptureResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            else -> {}
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Pick from Gallery")
            }
        }

        if (capturedFile != null) {
            Text(
                text = "File: ${capturedFile?.name}",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun ScaleTypesExample() {
    val imageUrl = "https://picsum.photos/400/600"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Same image with different scale types:",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text("CROP", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    modifier = Modifier.size(100.dp),
                    scaleType = ImageScaleType.CROP,
                    cornerRadius = 8.dp
                )
            }

            Column {
                Text("FIT", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    modifier = Modifier.size(100.dp),
                    scaleType = ImageScaleType.FIT,
                    cornerRadius = 8.dp,
                    colors = ImageViewDefaults.colors(backgroundColor = Color(0xFFF0F0F0))
                )
            }

            Column {
                Text("CENTER", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    modifier = Modifier.size(100.dp),
                    scaleType = ImageScaleType.CENTER,
                    cornerRadius = 8.dp,
                    colors = ImageViewDefaults.colors(backgroundColor = Color(0xFFF0F0F0))
                )
            }

            Column {
                Text("STRETCH", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    modifier = Modifier.size(100.dp),
                    scaleType = ImageScaleType.STRETCH,
                    cornerRadius = 8.dp
                )
            }
        }
    }
}

@Composable
private fun CornerBorderExample() {
    val imageUrl = "https://picsum.photos/300/300?random=corners"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("No corners", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 0.dp,
                    borderWidth = 0.dp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("8dp corners", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 8.dp,
                    borderWidth = 0.dp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("24dp corners", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 24.dp,
                    borderWidth = 0.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Thin border", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 8.dp,
                    borderWidth = 1.dp,
                    colors = ImageViewDefaults.colors(borderColor = Color.Gray)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Thick border", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 8.dp,
                    borderWidth = 3.dp,
                    colors = ImageViewDefaults.colors(borderColor = Color(0xFF2196F3))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Colored border", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 16.dp,
                    borderWidth = 2.dp,
                    colors = ImageViewDefaults.colors(borderColor = Color(0xFFE91E63))
                )
            }
        }
    }
}

@Composable
private fun AspectRatioExample() {
    val imageUrl = "https://picsum.photos/800/600?random=aspect"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("1:1 (Square)", fontSize = 12.sp, color = Color.Gray)
        AdvancedImageView(
            url = imageUrl,
            modifier = Modifier.width(150.dp),
            aspectRatio = 1f,
            cornerRadius = 8.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("16:9 (Widescreen)", fontSize = 12.sp, color = Color.Gray)
        AdvancedImageView(
            url = imageUrl,
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = 16f / 9f,
            cornerRadius = 8.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("4:3 (Standard)", fontSize = 12.sp, color = Color.Gray)
        AdvancedImageView(
            url = imageUrl,
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = 4f / 3f,
            cornerRadius = 8.dp
        )
    }
}

@Composable
private fun LabelsExample() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdvancedImageView(
                source = ImageSource.Empty,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                label = "Profile Photo",
                labelTextSize = 14.sp,
                onCaptureClick = {
                    Toast.makeText(context, "Profile photo capture", Toast.LENGTH_SHORT).show()
                }
            )

            AdvancedImageView(
                source = ImageSource.Empty,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                label = "ID Document",
                labelTextSize = 14.sp,
                labelFontWeight = FontWeight.Bold,
                onCaptureClick = {
                    Toast.makeText(context, "ID document capture", Toast.LENGTH_SHORT).show()
                }
            )
        }

        AdvancedImageView(
            url = "https://picsum.photos/800/400?random=label",
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = 2f,
            label = "Cover Image",
            labelGap = 8.dp,
            cornerRadius = 12.dp,
            borderWidth = 0.dp
        )
    }
}

@Composable
private fun CustomPlaceholderExample() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdvancedImageView(
                source = ImageSource.Empty,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                placeholderText = "Add Photo",
                placeholderIconSize = 32.dp,
                placeholderTextSize = 11.sp,
                onCaptureClick = {
                    Toast.makeText(context, "Add photo clicked", Toast.LENGTH_SHORT).show()
                }
            )

            AdvancedImageView(
                source = ImageSource.Empty,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                placeholderText = "Upload Image",
                placeholderIconSize = 40.dp,
                placeholderGap = 12.dp,
                onCaptureClick = {
                    Toast.makeText(context, "Upload clicked", Toast.LENGTH_SHORT).show()
                }
            )

            AdvancedImageView(
                source = ImageSource.Empty,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                placeholderText = "Scan Document",
                placeholderIconResId = R.drawable.ic_launcher_foreground,
                onCaptureClick = {
                    Toast.makeText(context, "Scan clicked", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun CustomColorsExample() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdvancedImageView(
                source = ImageSource.Empty,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                placeholderText = "Blue Theme",
                colors = ImageViewDefaults.colors(
                    backgroundColor = Color(0xFFE3F2FD),
                    borderColor = Color(0xFF2196F3),
                    placeholderIconColor = Color(0xFF1976D2),
                    placeholderTextColor = Color(0xFF1976D2)
                ),
                onCaptureClick = {
                    Toast.makeText(context, "Blue theme", Toast.LENGTH_SHORT).show()
                }
            )

            AdvancedImageView(
                source = ImageSource.Empty,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                placeholderText = "Green Theme",
                colors = ImageViewDefaults.colors(
                    backgroundColor = Color(0xFFE8F5E9),
                    borderColor = Color(0xFF4CAF50),
                    placeholderIconColor = Color(0xFF388E3C),
                    placeholderTextColor = Color(0xFF388E3C)
                ),
                onCaptureClick = {
                    Toast.makeText(context, "Green theme", Toast.LENGTH_SHORT).show()
                }
            )

            AdvancedImageView(
                source = ImageSource.Empty,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                placeholderText = "Orange Theme",
                colors = ImageViewDefaults.colors(
                    backgroundColor = Color(0xFFFFF3E0),
                    borderColor = Color(0xFFFF9800),
                    placeholderIconColor = Color(0xFFF57C00),
                    placeholderTextColor = Color(0xFFF57C00)
                ),
                onCaptureClick = {
                    Toast.makeText(context, "Orange theme", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Custom delete button colors
        AdvancedImageView(
            url = "https://picsum.photos/600/400?random=colors",
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = 3f / 2f,
            cornerRadius = 12.dp,
            borderWidth = 0.dp,
            showDeleteButton = true,
            colors = ImageViewDefaults.colors(
                deleteIconTint = Color.White,
                deleteButtonBackground = Color(0xCC000000)
            ),
            onDeleteClick = {
                Toast.makeText(context, "Delete with custom colors", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun UrlLoadingExample() {
    var urlCounter by remember { mutableIntStateOf(0) }
    val imageState = rememberAdvancedImageViewState()
    var loadStateText by remember { mutableStateOf("Not loaded") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Load state: $loadStateText",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        AdvancedImageView(
            state = imageState,
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = 16f / 9f,
            cornerRadius = 12.dp,
            borderWidth = 0.dp,
            showDeleteButton = true,
            onStateChange = { state ->
                loadStateText = when (state) {
                    ImageState.EMPTY -> "Empty"
                    ImageState.LOADING -> "Loading..."
                    ImageState.LOADED -> "Loaded successfully!"
                    ImageState.ERROR -> "Failed to load"
                }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    urlCounter++
                    imageState.loadFromUrl("https://picsum.photos/1200/675?random=$urlCounter")
                }
            ) {
                Text("Load Random Image")
            }

            Button(
                onClick = {
                    imageState.loadFromUrl("https://invalid-url-that-will-fail.xyz/image.jpg")
                }
            ) {
                Text("Load Invalid URL")
            }
        }
    }
}

@Composable
private fun DrawableResourceExample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Images loaded from drawable resources:",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdvancedImageViewFromResource(
                drawableResId = R.drawable.ic_launcher_foreground,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                cornerRadius = 8.dp,
                colors = ImageViewDefaults.colors(backgroundColor = Color(0xFFF5F5F5))
            )

            AdvancedImageViewFromResource(
                drawableResId = R.drawable.ic_launcher_background,
                modifier = Modifier.weight(1f),
                aspectRatio = 1f,
                cornerRadius = 8.dp
            )
        }
    }
}

@Composable
private fun FeatureTogglesExample() {
    val context = LocalContext.current
    val imageUrl = "https://picsum.photos/400/400?random=features"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Delete button ON", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 8.dp,
                    borderWidth = 0.dp,
                    showDeleteButton = true,
                    onDeleteClick = {
                        Toast.makeText(context, "Delete clicked", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Delete button OFF", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 8.dp,
                    borderWidth = 0.dp,
                    showDeleteButton = false
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Fullscreen ON", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 8.dp,
                    borderWidth = 0.dp,
                    showDeleteButton = false,
                    enableFullScreenPreview = true,
                    onImageClick = {
                        Toast.makeText(context, "Opening fullscreen...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Fullscreen OFF", fontSize = 12.sp, color = Color.Gray)
                AdvancedImageView(
                    url = imageUrl,
                    aspectRatio = 1f,
                    cornerRadius = 8.dp,
                    borderWidth = 0.dp,
                    showDeleteButton = false,
                    enableFullScreenPreview = false,
                    onImageClick = {
                        Toast.makeText(context, "Clicked (no fullscreen)", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
