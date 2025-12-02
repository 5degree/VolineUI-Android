package com.cropintellix.volineuiandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cropintellix.volineuiandroid.databinding.ActivityMainBinding

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
        b.btnOpenRadioExamples.setOnClickListener {
            startActivity(Intent(this, RadioExamplesActivity::class.java))
        }
        b.btnOpenDropdownExamples.setOnClickListener {
            startActivity(Intent(this, DropdownExamplesActivity::class.java))
        }
        b.btnOpenSignaturePadExamples.setOnClickListener {
            startActivity(Intent(this, SignaturePadExamplesActivity::class.java))
        }
        b.btnOpenPermissionExamples.setOnClickListener {
            startActivity(Intent(this, PermissionExamplesActivity::class.java))
        }
    }
}