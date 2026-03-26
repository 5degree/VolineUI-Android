package com.cropintellix.volineuiandroid.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropintellix.volineui.R as VolineR
import com.cropintellix.volineui.compose.ImageCarousel
import com.cropintellix.volineui.imageview.ActionButtonConfig
import com.cropintellix.volineui.imageview.ImageCarouselDefaults
import com.cropintellix.volineui.photocapturemanager.PhotoCaptureConfig
import com.cropintellix.volineuiandroid.R
import com.cropintellix.volineuiandroid.ui.theme.AppTheme
import java.io.File

/**
 * Example activity demonstrating the Compose ImageCarousel component.
 */
class ComposeImageCarouselExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ImageCarouselExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageCarouselExamplesScreen(modifier: Modifier = Modifier) {
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
            text = "Compose ImageCarousel Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Basic Usage with Files
        SectionTitle("File-based Carousel with Camera")
        FileCarouselExample()

        HorizontalDivider()

        // URL-based Carousel
        SectionTitle("URL-based Carousel")
        UrlCarouselExample()

        HorizontalDivider()

        // Action buttons on each slide
        SectionTitle("Action buttons per slide")
        ActionButtonsCarouselExample()

        HorizontalDivider()

        // Custom Styling
        SectionTitle("Custom Styling")
        CustomStylingExample()

        HorizontalDivider()

        // Read-only URL Carousel
        SectionTitle("Read-only (Display Only)")
        ReadOnlyExample()

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
private fun FileCarouselExample() {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ImageCarousel(
            files = files,
            label = "Captured Photos",
            carouselHeight = 250.dp,
            itemWidth = 180.dp,
            maxImageCount = 5,
            captureConfig = PhotoCaptureConfig("Demo Watermark ${files.size + 1}", true),
            onCaptureResult = { file ->
                files = files + file
                Toast.makeText(context, "Photo added", Toast.LENGTH_SHORT).show()
            },
            onCaptureError = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onImageDelete = { index ->
                files = files.toMutableList().apply { removeAt(index) }
            },
        )

        Text(
            text = "Tap + to capture. Loading shown while watermark is applied.",
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA)
        )
    }
}

@Composable
private fun UrlCarouselExample() {
    val context = LocalContext.current
    var urls by remember {
        mutableStateOf(
            listOf(
                "https://picsum.photos/400/400?random=1",
                "https://picsum.photos/400/400?random=2",
                "https://picsum.photos/400/400?random=3"
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Images from URLs: ${urls.size}",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        ImageCarousel(
            urls = urls,
            onUrlsChange = { newUrls ->
                urls = newUrls
            },
            label = "Gallery",
            onImageClick = { index ->
                Toast.makeText(context, "Clicked image $index", Toast.LENGTH_SHORT).show()
            },
            onImageDelete = { index ->
                Toast.makeText(context, "Deleted image $index", Toast.LENGTH_SHORT).show()
            },
            onAddClick = {
                // Add a random URL
                urls = urls + "https://picsum.photos/400/400?random=${System.currentTimeMillis()}"
            }
        )
    }
}

@Composable
private fun ActionButtonsCarouselExample() {
    val context = LocalContext.current
    var urls by remember {
        mutableStateOf(
            listOf(
                "https://picsum.photos/400/400?random=carouselAct1",
                "https://picsum.photos/400/400?random=carouselAct2",
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Each loaded slide shows scrollable chips (e.g. Upload). Delete stays top-right.",
            fontSize = 12.sp,
            color = Color(0xFF888888)
        )

        ImageCarousel(
            urls = urls,
            onUrlsChange = { urls = it },
            label = "Carousel with actions",
            carouselHeight = 220.dp,
            itemWidth = 200.dp,
            actionButtons = { index ->
                listOf(
                    ActionButtonConfig(
                        iconResId = R.drawable.ic_cloud_upload,
                        text = "Upload",
                        iconTint = 0xFFFFFFFF.toInt(),
                        backgroundColor = 0xCC1976D2.toInt(),
                        textColor = 0xFFFFFFFF.toInt(),
                        onClick = {
                            Toast.makeText(
                                context,
                                "Upload slide $index",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    ),
                    ActionButtonConfig(
                        iconResId = VolineR.drawable.ic_info_filled,
                        text = "Info",
                        iconTint = 0xFFFFFFFF.toInt(),
                        backgroundColor = 0xCC00796B.toInt(),
                        textColor = 0xFFFFFFFF.toInt(),
                        onClick = {
                            Toast.makeText(
                                context,
                                "Info for slide $index",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    ),
                )
            },
        )
    }
}

@Composable
private fun CustomStylingExample() {
    val urls = listOf(
        "https://picsum.photos/400/400?random=style1",
        "https://picsum.photos/400/400?random=style2",
        "https://picsum.photos/400/400?random=style3"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Blue theme:",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        ImageCarousel(
            urls = urls,
            cornerRadius = 16.dp,
            borderWidth = 2.dp,
            colors = ImageCarouselDefaults.colors(
                borderColor = Color(0xFF2196F3),
                backgroundColor = Color(0xFFE3F2FD),
                indicatorActiveColor = Color(0xFF2196F3),
                indicatorInactiveColor = Color(0x402196F3)
            ),
            showDeleteIcon = false
        )

        Text(
            text = "Green theme with larger items:",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        ImageCarousel(
            urls = urls,
            carouselHeight = 180.dp,
            itemWidth = 200.dp,
            cornerRadius = 12.dp,
            colors = ImageCarouselDefaults.colors(
                borderColor = Color(0xFF4CAF50),
                backgroundColor = Color(0xFFE8F5E9),
                indicatorActiveColor = Color(0xFF4CAF50),
                indicatorInactiveColor = Color(0x404CAF50)
            ),
            showDeleteIcon = false
        )
    }
}

@Composable
private fun ReadOnlyExample() {
    val urls = listOf(
        "https://picsum.photos/400/400?random=readonly1",
        "https://picsum.photos/400/400?random=readonly2",
        "https://picsum.photos/400/400?random=readonly3",
        "https://picsum.photos/400/400?random=readonly4"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Display-only mode (no edit, tap for fullscreen):",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        ImageCarousel(
            urls = urls,
            // No onUrlsChange = read-only, no delete button
            label = "Photo Gallery",
            showDeleteIcon = false,
            enableFullScreen = true
        )
    }
}
