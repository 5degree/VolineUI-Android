package `in`.fivedegree.volineuiandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import `in`.fivedegree.volineuiandroid.compose.ComposeButtonExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.ComposeDateTimePickerExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.ComposeDialogExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.ComposeDropdownExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.ComposeImageCarouselExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.ComposeImageViewExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.ComposeInputFieldExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.ComposeRadioExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.ComposeToastExamplesActivity
import `in`.fivedegree.volineuiandroid.compose.RemoteLoggerTestActivity
import `in`.fivedegree.volineuiandroid.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private val b get() = binding!!
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        b.btnOpenInputFieldExamples.setOnClickListener {
            startActivity(Intent(this, InputFieldExamplesActivity::class.java))
        }
        b.btnOpenComposeInputFieldExamples.setOnClickListener {
            startActivity(Intent(this, ComposeInputFieldExamplesActivity::class.java))
        }
        b.btnOpenRadioExamples.setOnClickListener {
            startActivity(Intent(this, RadioExamplesActivity::class.java))
        }
        b.btnOpenComposeRadioExamples.setOnClickListener {
            startActivity(Intent(this, ComposeRadioExamplesActivity::class.java))
        }
        b.btnOpenDropdownExamples.setOnClickListener {
            startActivity(Intent(this, DropdownExamplesActivity::class.java))
        }
        b.btnOpenComposeDropdownExamples.setOnClickListener {
            startActivity(Intent(this, ComposeDropdownExamplesActivity::class.java))
        }
        b.btnOpenSignaturePadExamples.setOnClickListener {
            startActivity(Intent(this, SignaturePadExamplesActivity::class.java))
        }
        b.btnOpenPermissionExamples.setOnClickListener {
            startActivity(Intent(this, PermissionExamplesActivity::class.java))
        }
        b.btnOpenLocationExamples.setOnClickListener {
            startActivity(Intent(this, LocationExamplesActivity::class.java))
        }
        b.btnOpenPhotoCaptureExamples.setOnClickListener {
            startActivity(Intent(this, PhotoCaptureExamplesActivity::class.java))
        }
        b.btnOpenImageViewExamples.setOnClickListener {
            startActivity(Intent(this, ImageViewExamplesActivity::class.java))
        }
        b.btnOpenComposeImageViewExamples.setOnClickListener {
            startActivity(Intent(this, ComposeImageViewExamplesActivity::class.java))
        }
        b.btnOpenButtonExamples.setOnClickListener {
            startActivity(Intent(this, ButtonExamplesActivity::class.java))
        }
        b.btnOpenComposeButtonExamples.setOnClickListener {
            startActivity(Intent(this, ComposeButtonExamplesActivity::class.java))
        }
        b.btnOpenComposeImageCarouselExamples.setOnClickListener {
            startActivity(Intent(this, ComposeImageCarouselExamplesActivity::class.java))
        }
        b.btnOpenToastExamples.setOnClickListener {
            startActivity(Intent(this, ToastExamplesActivity::class.java))
        }
        b.btnOpenComposeToastExamples.setOnClickListener {
            startActivity(Intent(this, ComposeToastExamplesActivity::class.java))
        }
        b.btnOpenDialogExamples.setOnClickListener {
            startActivity(Intent(this, DialogExamplesActivity::class.java))
        }
        b.btnOpenComposeDialogExamples.setOnClickListener {
            startActivity(Intent(this, ComposeDialogExamplesActivity::class.java))
        }
        b.btnOpenComposeDateTimePickerExamples.setOnClickListener {
            startActivity(Intent(this, ComposeDateTimePickerExamplesActivity::class.java))
        }
        b.btnOpenRemoteLoggerTest.setOnClickListener {
            startActivity(Intent(this, RemoteLoggerTestActivity::class.java))
        }
    }
}