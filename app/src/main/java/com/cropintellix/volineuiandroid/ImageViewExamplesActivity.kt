package com.cropintellix.volineuiandroid

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cropintellix.volineui.AdvancedImageView
import com.cropintellix.volineui.ImageCarousel
import com.cropintellix.volineui.PhotoCaptureConfig
import com.cropintellix.volineui.PhotoCaptureManager
import com.cropintellix.volineui.PhotoCaptureResult

class ImageViewExamplesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view_examples)

        setupImageViewExamples()
        setupCarouselExamples()
        setupCameraCapture()
    }

    private fun setupImageViewExamples() {
        // Example 2: With Label - camera capture
        val imageWithLabel = findViewById<AdvancedImageView>(R.id.imageWithLabel)
        imageWithLabel.setOnCaptureClickListener { config ->
            capturePhoto(imageWithLabel)
        }

        // Example 3: Circular image
        val circularImage = findViewById<AdvancedImageView>(R.id.circularImage)
        circularImage.setOnCaptureClickListener { capturePhoto(circularImage) }

        // Example 4: Aspect ratio
        val aspect16_9 = findViewById<AdvancedImageView>(R.id.aspect16_9)
        aspect16_9.setOnCaptureClickListener { capturePhoto(aspect16_9) }

        // Example 5: Square
        val squareNoBorder = findViewById<AdvancedImageView>(R.id.squareNoBorder)
        squareNoBorder.setOnCaptureClickListener { capturePhoto(squareNoBorder) }

        // Example 6: URL loading
        val imageFromUrl = findViewById<AdvancedImageView>(R.id.imageFromUrl)
        val btnLoadUrl = findViewById<Button>(R.id.btnLoadUrl)
        btnLoadUrl.setOnClickListener {
            imageFromUrl.loadFromUrl("https://picsum.photos/800/600")
        }

        // Example 7: Custom placeholder
        val customPlaceholder = findViewById<AdvancedImageView>(R.id.customPlaceholder)
        customPlaceholder.setOnCaptureClickListener { capturePhoto(customPlaceholder) }

        // Example 8-9: Scale types
        val scaleFit = findViewById<AdvancedImageView>(R.id.scaleFit)
        scaleFit.setOnCaptureClickListener { capturePhoto(scaleFit) }

        val scaleCenter = findViewById<AdvancedImageView>(R.id.scaleCenter)
        scaleCenter.setOnCaptureClickListener { capturePhoto(scaleCenter) }

        // Example 10: Thick border
        val thickBorder = findViewById<AdvancedImageView>(R.id.thickBorder)
        thickBorder.setOnCaptureClickListener { capturePhoto(thickBorder) }

        // Example 17-19
        val noLoading = findViewById<AdvancedImageView>(R.id.noLoading)
        noLoading.setOnCaptureClickListener { capturePhoto(noLoading) }

        val coloredBg = findViewById<AdvancedImageView>(R.id.coloredBg)
        coloredBg.setOnCaptureClickListener { capturePhoto(coloredBg) }

        val noFullscreen = findViewById<AdvancedImageView>(R.id.noFullscreen)
        noFullscreen.setOnCaptureClickListener { capturePhoto(noFullscreen) }
    }

    private fun setupCarouselExamples() {
        // Carousel 1: Basic
        val carousel1 = findViewById<ImageCarousel>(R.id.carousel1)
        val btnAdd1 = findViewById<Button>(R.id.btnAddToCarousel1)
        btnAdd1.setOnClickListener {
            carousel1.addImage("https://picsum.photos/300/300?random=${System.currentTimeMillis()}")
            carousel1.addImage("https://picsum.photos/300/300?random=${System.currentTimeMillis() + 1}")
            carousel1.addImage("https://picsum.photos/300/300?random=${System.currentTimeMillis() + 2}")
        }
        carousel1.setOnImageDeleteListener { index ->
            Toast.makeText(this, "Deleted image at index $index", Toast.LENGTH_SHORT).show()
        }

        // Carousel 2: Large items
        val carousel2 = findViewById<ImageCarousel>(R.id.carousel2)
        carousel2.addImage("https://picsum.photos/400/300?random=1")
        carousel2.addImage("https://picsum.photos/400/300?random=2")

        // Carousel 3: Thumbnails
        val carousel3 = findViewById<ImageCarousel>(R.id.carousel3)
        for (i in 1..6) {
            carousel3.addImage("https://picsum.photos/160/160?random=$i")
        }

        // Carousel 4: Circular
        val carousel4 = findViewById<ImageCarousel>(R.id.carousel4)
        carousel4.addImage("https://picsum.photos/200/200?random=10")
        carousel4.addImage("https://picsum.photos/200/200?random=11")
        carousel4.addImage("https://picsum.photos/200/200?random=12")

        // Carousel 5: No delete
        val carousel5 = findViewById<ImageCarousel>(R.id.carousel5)
        carousel5.addImage("https://picsum.photos/240/240?random=20")
        carousel5.addImage("https://picsum.photos/240/240?random=21")

        // Gallery demo
        val galleryDemo = findViewById<ImageCarousel>(R.id.galleryDemo)
        val btnAddGallery = findViewById<Button>(R.id.btnAddToGallery)
        btnAddGallery.setOnClickListener {
            PhotoCaptureManager.instance.capturePhoto(PhotoCaptureConfig()) { result ->
                when (result) {
                    is PhotoCaptureResult.Success -> {
                        galleryDemo.addImage(result.file)
                    }
                    is PhotoCaptureResult.Error -> {
                        Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
        galleryDemo.setOnImageDeleteListener { index ->
            Toast.makeText(this, "Photo $index deleted from gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCameraCapture() {
        val cameraCapture = findViewById<AdvancedImageView>(R.id.cameraCapture)
        val btnCapture = findViewById<Button>(R.id.btnCapture)
        val btnGallery = findViewById<Button>(R.id.btnPickGallery)

        cameraCapture.setOnCaptureClickListener { capturePhoto(cameraCapture) }

        btnCapture.setOnClickListener { capturePhoto(cameraCapture) }

        btnGallery.setOnClickListener {
            PhotoCaptureManager.instance.pickPhotoFromGallery(PhotoCaptureConfig()) { result ->
                when (result) {
                    is PhotoCaptureResult.Success -> {
                        cameraCapture.loadFromFile(result.file)
                    }
                    is PhotoCaptureResult.Error -> {
                        Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun capturePhoto(targetView: AdvancedImageView) {
        PhotoCaptureManager.instance.capturePhoto(PhotoCaptureConfig()) { result ->
            when (result) {
                is PhotoCaptureResult.Success -> {
                    targetView.loadFromFile(result.file)
                }
                is PhotoCaptureResult.Error -> {
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is PhotoCaptureResult.Cancelled -> {
                    // User cancelled
                }
                is PhotoCaptureResult.Processing -> {
                    // Processing
                }
            }
        }
    }
}
