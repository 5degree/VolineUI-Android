package co.kcagroforestry.app.utils

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import co.kcagroforestry.app.BuildConfig
import co.kcagroforestry.app.R
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoCaptureHelper(
    private val activity: Activity,
    private val onPhotoProcessed: (File, Bitmap) -> Unit
) {
    private var tempFile: File? = null
    private var watermarkLabel: String = ""

    companion object {
        const val CAMERA_REQUEST_CODE = 1001

        private val REQUIRED_PERMISSIONS: Array<String> = buildList {
            add(Manifest.permission.CAMERA)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // WRITE_EXTERNAL_STORAGE and READ_EXTERNAL_STORAGE are needed below Android 10
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Only READ_EXTERNAL_STORAGE is needed for Android 10 to 12
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                // Android 13+ uses new media permissions
                add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }.toTypedArray()
    }

    fun capturePhoto(watermarkLabel: String) {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, CAMERA_REQUEST_CODE)
            return
        }
        this.watermarkLabel = watermarkLabel
        dispatchTakePictureIntent()
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(
                activity,
                "Camera and storage permissions are required.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            handleCapturedImage()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
            tempFile = createImageFile(activity)
            tempFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    activity,
                    BuildConfig.APPLICATION_ID + ".provider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                activity.startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        }
    }

    private fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun handleCapturedImage() {
        val filePath = tempFile?.absolutePath ?: return
        var bitmap = decodeAndResizeBitmap(filePath, 600, 800) ?: return
        bitmap = getRotatedBitmap(filePath, bitmap)

        val timeStamp = "[ ${getCurrentDate()} ${getCurrentFormattedTime()} ]"
        val watermark = "$watermarkLabel\n$timeStamp"
        val finalFile = getCompressedWatermarkedFile(watermark, bitmap, activity)
        if (finalFile != null) {
            onPhotoProcessed(finalFile, bitmap)
        } else {
            Toast.makeText(activity, "Failed to process image.", Toast.LENGTH_SHORT).show()
        }
        tempFile?.delete()
    }

    private fun decodeAndResizeBitmap(filePath: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, options)
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > maxHeight || width > maxWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                inSampleSize *= 2
            }
        }
        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun getRotatedBitmap(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = androidx.exifinterface.media.ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            val rotation = when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotation != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotation)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun getCompressedWatermarkedFile(
        watermark: String,
        bitmap: Bitmap,
        context: Context
    ): File? {
        var bitmapWithWatermark =
            CommonData().drawTextToBitmap(context.applicationContext, bitmap, watermark)!!
        bitmapWithWatermark = resizeBitmapKeepingRatio(bitmapWithWatermark, 720, 960)
        val filename = "IMG_${System.currentTimeMillis()}"
        val savedUri = saveImageToGallery(context, bitmapWithWatermark, filename)
        return if (savedUri != null) {
            copyImageFromUri(context, savedUri)
        } else {
            null
        }
    }

    private fun saveImageToGallery(context: Context, bitmap: Bitmap, filename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/Agroforestry"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        return imageUri
    }

    private fun copyImageFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file =
                File(context.getExternalFilesDir(null), "copied_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeBitmapKeepingRatio(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (ratioMax > ratioBitmap) {
            finalWidth = ((maxHeight.toFloat() * ratioBitmap).toInt())
        } else {
            finalHeight = ((maxWidth.toFloat() / ratioBitmap).toInt())
        }
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getCurrentFormattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun setPhotoToImageView(
        context: Context,
        photo: File?,
        imageView: ImageView,
        placeholder: Int = R.drawable.img_placeholder
    ) {
        Glide.with(context)
            .load(photo)
            .placeholder(placeholder)
            .into(imageView)
    }

    fun previewPhoto(context: Context, path: String) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_image_preview)
        dialog.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val btnDismiss = dialog.findViewById<Button>(R.id.btnDismiss)
        val iv = dialog.findViewById<ImageView>(R.id.ivPreview)
        iv.setImageBitmap(BitmapFactory.decodeFile(path))
        btnDismiss.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
