package `in`.fivedegree.volineuiandroid

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import `in`.fivedegree.volineui.PhotoCaptureManager
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureConfig
import `in`.fivedegree.volineui.photocapturemanager.PhotoCaptureResult
import java.io.File

/**
 * PhotoCaptureExamplesActivity - Demonstrates PhotoCaptureManager usage
 *
 * Shows examples of:
 * - Capturing photos with camera
 * - Picking photos from gallery
 * - Applying watermarks with cached location
 * - Applying watermarks with fresh location
 * - Previewing captured photos
 */
class PhotoCaptureExamplesActivity : AppCompatActivity() {

    private lateinit var btnCaptureNoWatermark: Button
    private lateinit var btnCaptureCachedLocation: Button
    private lateinit var btnCaptureFreshLocation: Button
    private lateinit var btnPickFromGallery: Button

    private lateinit var ivPhoto: ImageView
    private lateinit var tvMetadata: TextView

    private var currentPhotoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_capture_examples)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        btnCaptureNoWatermark = findViewById(R.id.btnCaptureNoWatermark)
        btnCaptureCachedLocation = findViewById(R.id.btnCaptureCachedLocation)
        btnCaptureFreshLocation = findViewById(R.id.btnCaptureFreshLocation)
        btnPickFromGallery = findViewById(R.id.btnPickFromGallery)

        ivPhoto = findViewById(R.id.ivPhoto)
        tvMetadata = findViewById(R.id.tvMetadata)

    }

    private fun setupClickListeners() {
        // Capture without watermark
        btnCaptureNoWatermark.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Capture with watermark (cached location)
        btnCaptureCachedLocation.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = "Farm Survey - Cached Location",
                printFreshLatLng = false,
                targetFileSizeKB = 200,
                watermarkPosition = PhotoCaptureConfig.WatermarkPosition.BOTTOM_LEFT
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Capture with watermark (fresh location)
        btnCaptureFreshLocation.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = "Farm Survey - Fresh Location",
                printFreshLatLng = true,
                targetFileSizeKB = 200,
                watermarkPosition = PhotoCaptureConfig.WatermarkPosition.BOTTOM_LEFT
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Pick from gallery with watermark
        btnPickFromGallery.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = "Gallery Photo - Watermarked",
                printFreshLatLng = false,
                targetFileSizeKB = 200,
                watermarkPosition = PhotoCaptureConfig.WatermarkPosition.BOTTOM_RIGHT
            )
            PhotoCaptureManager.instance.pickPhotoFromGallery(config) { result ->
                handlePhotoResult(result)
            }
        }
    }

    private fun handlePhotoResult(result: PhotoCaptureResult) {
        when (result) {
            is PhotoCaptureResult.Processing -> {
                // Show loading indicator
                ivPhoto.setImageResource(R.drawable.loading_gif)
                tvMetadata.text = "⏳ Processing image...\nApplying watermark and fetching location..."
            }
            
            is PhotoCaptureResult.Success -> {
                currentPhotoFile = result.file

                // Display photo
                ivPhoto.setImageBitmap(result.bitmap)

                // Display metadata
                val metadata = buildString {
                    appendLine("✓ Photo captured successfully!")
                    appendLine()
                    appendLine("File: ${result.file.name}")
                    appendLine("Size: ${result.fileSizeDisplay}")
                    appendLine("Dimensions: ${result.dimensionsDisplay}")
                    appendLine("Watermark: ${if (result.hasWatermark) "Yes" else "No"}")

                    result.location?.let { location ->
                        appendLine()
                        appendLine("Location:")
                        appendLine("  Lat: ${String.format("%.5f", location.latitude)}")
                        appendLine("  Lng: ${String.format("%.5f", location.longitude)}")
                        appendLine("  Cached: ${location.isFromCache}")
                    }
                }

                tvMetadata.text = metadata

                Toast.makeText(this, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
            }

            is PhotoCaptureResult.Error -> {
                ivPhoto.setImageDrawable(null)
                tvMetadata.text = "❌ Error: ${result.message}"
                Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_LONG).show()
            }

            is PhotoCaptureResult.Cancelled -> {
                tvMetadata.text = "Operation cancelled by user"
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
