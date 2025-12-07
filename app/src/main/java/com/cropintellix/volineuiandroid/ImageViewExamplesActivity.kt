package com.cropintellix.volineuiandroid

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cropintellix.volineui.AdvancedImageView
import com.cropintellix.volineui.PhotoCaptureConfig
import com.cropintellix.volineui.PhotoCaptureManager
import com.cropintellix.volineui.PhotoCaptureResult

/**
 * ImageViewExamplesActivity - Demonstrates AdvancedImageView usage
 *
 * Shows examples of:
 * - Basic image display with label
 * - Camera capture integration
 * - Different scale types (fit, crop)
 * - Aspect ratio configuration
 * - Delete functionality
 * - Full-screen preview on tap
 * - Loading from URL, drawable, file
 */
class ImageViewExamplesActivity : AppCompatActivity() {

    private lateinit var imageViewBasic: AdvancedImageView
    private lateinit var imageViewCamera: AdvancedImageView
    private lateinit var imageViewFit: AdvancedImageView
    private lateinit var imageViewCrop: AdvancedImageView
    private lateinit var imageViewAspect: AdvancedImageView
    private lateinit var imageViewCircle: AdvancedImageView

    private lateinit var btnLoadUrl: Button
    private lateinit var btnLoadDrawable: Button
    private lateinit var btnCapturePhoto: Button
    private lateinit var btnPickGallery: Button
    private lateinit var btnClearAll: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view_examples)

        initializeViews()
        setupImageViews()
        setupButtonListeners()
        loadSampleImages()
    }

    private fun initializeViews() {
        imageViewBasic = findViewById(R.id.imageViewBasic)
        imageViewCamera = findViewById(R.id.imageViewCamera)
        imageViewFit = findViewById(R.id.imageViewFit)
        imageViewCrop = findViewById(R.id.imageViewCrop)
        imageViewAspect = findViewById(R.id.imageViewAspect)
        imageViewCircle = findViewById(R.id.imageViewCircle)

        btnLoadUrl = findViewById(R.id.btnLoadUrl)
        btnLoadDrawable = findViewById(R.id.btnLoadDrawable)
        btnCapturePhoto = findViewById(R.id.btnCapturePhoto)
        btnPickGallery = findViewById(R.id.btnPickGallery)
        btnClearAll = findViewById(R.id.btnClearAll)
        tvStatus = findViewById(R.id.tvStatus)
    }

    private fun setupImageViews() {
        // Setup camera capture listener for imageViewCamera
        imageViewCamera.setOnCaptureClickListener { config ->
            capturePhotoFor(imageViewCamera, config)
        }

        // Setup delete listeners
        imageViewCamera.setOnDeleteClickListener {
            imageViewCamera.clearImage()
            updateStatus("Camera image cleared")
        }

        imageViewCircle.setOnDeleteClickListener {
            imageViewCircle.clearImage()
            updateStatus("Profile image cleared")
        }

        // Setup image load listeners
        listOf(imageViewBasic, imageViewCamera, imageViewFit, imageViewCrop, imageViewAspect, imageViewCircle).forEach { iv ->
            iv.setOnImageLoadListener { success ->
                if (success) {
                    updateStatus("Image loaded successfully")
                } else {
                    updateStatus("Failed to load image")
                }
            }
        }
    }

    private fun setupButtonListeners() {
        // Load from URL
        btnLoadUrl.setOnClickListener {
            val sampleUrl = "https://picsum.photos/800/600"
            imageViewBasic.loadFromUrl(sampleUrl)
            updateStatus("Loading image from URL...")
        }

        // Load from drawable
        btnLoadDrawable.setOnClickListener {
            // Use a sample drawable resource (you may need to add one)
            imageViewBasic.loadFromDrawable(android.R.drawable.ic_menu_gallery)
            updateStatus("Loaded from drawable")
        }

        // Capture photo
        btnCapturePhoto.setOnClickListener {
            val config = PhotoCaptureConfig(
                targetFileSizeKB = 200,
                saveToGallery = true
            )
            capturePhotoFor(imageViewBasic, config)
        }

        // Pick from gallery
        btnPickGallery.setOnClickListener {
            val config = PhotoCaptureConfig(
                targetFileSizeKB = 200
            )
            PhotoCaptureManager.instance.pickPhotoFromGallery(config) { result ->
                handlePhotoResult(result, imageViewBasic)
            }
        }

        // Clear all images
        btnClearAll.setOnClickListener {
            imageViewBasic.clearImage()
            imageViewCamera.clearImage()
            imageViewFit.clearImage()
            imageViewCrop.clearImage()
            imageViewAspect.clearImage()
            imageViewCircle.clearImage()
            updateStatus("All images cleared")
        }
    }

    private fun loadSampleImages() {
        // Load sample images for demonstration
        val sampleUrls = listOf(
            "https://picsum.photos/400/300?random=1",
            "https://picsum.photos/400/300?random=2",
            "https://picsum.photos/800/450?random=3",
            "https://picsum.photos/200/200?random=4"
        )

        imageViewFit.loadFromUrl(sampleUrls[0])
        imageViewCrop.loadFromUrl(sampleUrls[1])
        imageViewAspect.loadFromUrl(sampleUrls[2])
        imageViewCircle.loadFromUrl(sampleUrls[3])
    }

    private fun capturePhotoFor(imageView: AdvancedImageView, config: PhotoCaptureConfig) {
        updateStatus("Opening camera...")
        
        PhotoCaptureManager.instance.capturePhoto(config) { result ->
            handlePhotoResult(result, imageView)
        }
    }

    private fun handlePhotoResult(result: PhotoCaptureResult, targetImageView: AdvancedImageView) {
        when (result) {
            is PhotoCaptureResult.Processing -> {
                updateStatus("Processing image...")
            }

            is PhotoCaptureResult.Success -> {
                // Load the captured/picked photo into the image view
                targetImageView.loadFromFile(result.file)
                updateStatus("Photo loaded: ${result.fileSizeDisplay}")
                Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show()
            }

            is PhotoCaptureResult.Error -> {
                updateStatus("Error: ${result.message}")
                Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_LONG).show()
            }

            is PhotoCaptureResult.Cancelled -> {
                updateStatus("Cancelled")
            }
        }
    }

    private fun updateStatus(message: String) {
        tvStatus.text = "Status: $message"
    }
}
