package `in`.fivedegree.volineuiandroid

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import `in`.fivedegree.volineui.PhotoCaptureManager
import `in`.fivedegree.volineui.photocapturemanager.CaptureGuide
import `in`.fivedegree.volineui.photocapturemanager.FaceMeshResult
import `in`.fivedegree.volineui.photocapturemanager.OcrField
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
    private lateinit var btnCaptureOcr: Button
    private lateinit var btnPickOcrFromGallery: Button
    private lateinit var btnCaptureLocked169: Button
    private lateinit var btnCaptureFreeAspect: Button
    private lateinit var btnCaptureFrontAllowed: Button
    private lateinit var btnCapturePersonGuide: Button
    private lateinit var btnCaptureDocumentScanner: Button
    private lateinit var btnCaptureDocumentScannerOcr: Button

    private lateinit var ivPhoto: ImageView
    private lateinit var tvMetadata: TextView

    private var currentPhotoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Diagnostic: the scanner flow finishes RESULT_OK but this
        // screen stays on its initial text. Comparing this instance
        // hash with the one in handlePhotoResult shows whether the
        // result lambda ran on this visible instance or a dead one.
        Log.d("DemoPhoto", "PhotoCaptureExamplesActivity onCreate instance=${this.hashCode()}")
        setContentView(R.layout.activity_photo_capture_examples)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        btnCaptureNoWatermark = findViewById(R.id.btnCaptureNoWatermark)
        btnCaptureCachedLocation = findViewById(R.id.btnCaptureCachedLocation)
        btnCaptureFreshLocation = findViewById(R.id.btnCaptureFreshLocation)
        btnPickFromGallery = findViewById(R.id.btnPickFromGallery)
        btnCaptureOcr = findViewById(R.id.btnCaptureOcr)
        btnPickOcrFromGallery = findViewById(R.id.btnPickOcrFromGallery)
        btnCaptureLocked169 = findViewById(R.id.btnCaptureLocked169)
        btnCaptureFreeAspect = findViewById(R.id.btnCaptureFreeAspect)
        btnCaptureFrontAllowed = findViewById(R.id.btnCaptureFrontAllowed)
        btnCapturePersonGuide = findViewById(R.id.btnCapturePersonGuide)
        btnCaptureDocumentScanner = findViewById(R.id.btnCaptureDocumentScanner)
        btnCaptureDocumentScannerOcr = findViewById(R.id.btnCaptureDocumentScannerOcr)

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

        // Capture a document photo and collect fields with OCR: after
        // confirming the photo, the user taps words on it for each field.
        btnCaptureOcr.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200,
                ocrFields = listOf(
                    OcrField("personName", "Person Name", required = true),
                    OcrField("dob", "Date of Birth", required = true),
                    OcrField("gender", "Gender", required = false),
                    OcrField("documentNumber", "Document Number", required = true)
                )
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Same OCR flow, starting from a gallery photo instead of the camera.
        btnPickOcrFromGallery.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200,
                ocrFields = listOf(
                    OcrField("personName", "Person Name", required = true),
                    OcrField("dob", "Date of Birth", required = true),
                    OcrField("gender", "Gender", required = false),
                    OcrField("documentNumber", "Document Number", required = true)
                )
            )
            PhotoCaptureManager.instance.pickPhotoFromGallery(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Capture locked to 16:9: the aspect pill is hidden and the
        // user cannot change the ratio for this capture.
        btnCaptureLocked169.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200,
                aspectRatio = PhotoCaptureConfig.AspectRatio.RATIO_16_9
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Capture with no aspect lock: the pill is shown and the user
        // may switch 4:3 / 16:9 for this capture only.
        btnCaptureFreeAspect.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200,
                aspectRatio = PhotoCaptureConfig.AspectRatio.NONE
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Capture with the front camera allowed: the switch-camera
        // button appears (on devices that have a front camera). The
        // camera still opens on the back camera.
        btnCaptureFrontAllowed.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200,
                allowFrontCamera = true
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Capture a person with the live guide: the person outline
        // is drawn over the preview and the shutter stays disabled
        // until a person is framed inside it. The result carries a
        // face mesh (468 points + box) in the delivered photo's
        // pixel space, drawn over the photo and listed in the
        // metadata below, so the host can verify the mesh data
        // arrives aligned and usable.
        btnCapturePersonGuide.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200,
                captureGuide = CaptureGuide.PERSON
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Capture a document through Google's scanner UI: the
        // cleaned page comes back into the confirmation screen.
        btnCaptureDocumentScanner.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200,
                captureGuide = CaptureGuide.DOCUMENT
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }

        // Same scanner flow, with the OCR step on the cleaned page
        // after confirmation.
        btnCaptureDocumentScannerOcr.setOnClickListener {
            val config = PhotoCaptureConfig(
                watermarkText = null,
                targetFileSizeKB = 200,
                captureGuide = CaptureGuide.DOCUMENT,
                ocrFields = listOf(
                    OcrField("personName", "Person Name", required = true),
                    OcrField("dob", "Date of Birth", required = true),
                    OcrField("gender", "Gender", required = false),
                    OcrField("documentNumber", "Document Number", required = true)
                )
            )
            PhotoCaptureManager.instance.capturePhoto(config) { result ->
                handlePhotoResult(result)
            }
        }
    }

    private fun handlePhotoResult(result: PhotoCaptureResult) {
        Log.d(
            "DemoPhoto",
            "handlePhotoResult ${result::class.simpleName} instance=${this.hashCode()}"
        )
        when (result) {
            is PhotoCaptureResult.Processing -> {
                // Show loading indicator
                ivPhoto.setImageResource(R.drawable.loading_gif)
                tvMetadata.text = "⏳ Processing image...\nApplying watermark and fetching location..."
            }
            
            is PhotoCaptureResult.Success -> {
                currentPhotoFile = result.file

                // Display photo. When a face mesh was returned, draw
                // it over a copy of the photo: the mesh points are
                // in this bitmap's pixel space, so they land exactly
                // on the face with no view-scale mapping, and dots
                // sitting on eyes, nose and lips prove the host
                // received the data aligned and usable.
                val mesh = result.faceMesh
                val photo = result.bitmap
                ivPhoto.setImageBitmap(
                    if (mesh != null && mesh.points.isNotEmpty() && photo != null) {
                        drawFaceMeshOverlay(photo, mesh)
                    } else {
                        photo
                    }
                )

                // Display metadata
                val metadata = buildString {
                    appendLine("✓ Photo captured successfully!")
                    appendLine()
                    appendLine("File: ${result.file.name}")
                    appendLine("Size: ${result.fileSizeDisplay}")
                    appendLine("Dimensions: ${result.dimensionsDisplay}")
                    appendLine("Watermark: ${if (result.hasWatermark) "Yes" else "No"}")

                    if (result.ocrData.isNotEmpty()) {
                        appendLine()
                        appendLine("OCR data:")
                        result.ocrData.forEach { (key, value) ->
                            appendLine("  $key: $value")
                        }
                    }

                    result.faceMesh?.let { mesh ->
                        appendLine()
                        if (mesh.points.isEmpty()) {
                            appendLine("Face mesh: no face found")
                        } else {
                            appendLine("Face mesh: ${mesh.points.size} points")
                            mesh.boundingBox?.let { box ->
                                appendLine("  Box: ${box.left.toInt()}, ${box.top.toInt()} - ${box.right.toInt()}, ${box.bottom.toInt()}")
                            }
                            appendLine("  Overlay: drawn on the photo above")
                        }
                    }

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

    /**
     * Draws a face mesh over a copy of [photo] and returns it; the
     * original bitmap is left untouched. Points become white dots
     * and the bounding box a green rectangle, both in the photo's
     * own pixel space, so no view-scale mapping is needed. Dot and
     * stroke sizes scale with the photo so the mesh stays visible
     * at any resolution. Note the library payload carries points
     * and a box only, no triangle indices, so this draws dots
     * rather than the triangulated wireframe.
     */
    private fun drawFaceMeshOverlay(photo: Bitmap, mesh: FaceMeshResult): Bitmap {
        val overlay = photo.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(overlay)

        mesh.boundingBox?.let { box ->
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GREEN
                style = Paint.Style.STROKE
                strokeWidth = maxOf(4f, photo.width / 300f)
            }
            canvas.drawRect(box.left, box.top, box.right, box.bottom, boxPaint)
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val dotRadius = maxOf(3f, photo.width / 400f)
        for (point in mesh.points) {
            canvas.drawCircle(point.x, point.y, dotRadius, dotPaint)
        }
        return overlay
    }
}
