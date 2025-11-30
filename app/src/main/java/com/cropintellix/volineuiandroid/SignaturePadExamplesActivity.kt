package com.cropintellix.volineuiandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cropintellix.volineui.SignaturePad

class SignaturePadExamplesActivity : AppCompatActivity() {

    private lateinit var signaturePad: SignaturePad

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signature_pad_examples)

        signaturePad = findViewById(R.id.signaturePad1)
        
        // Setup signature change listener
        signaturePad.setOnSignatureChangeListener { hasSignature ->
            // Can show/hide UI elements based on signature state if needed
        }
        signaturePad.setCustomColors(intArrayOf(R.color.black, R.color.white, R.color.primary_color_pink, R.color.red, R.color.maroon, R.color.green, R.color.purple, R.color.skyblue, R.color.yellow, R.color.green, R.color.orange))
    }
}