package `in`.fivedegree.volineuiandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import `in`.fivedegree.volineui.SignaturePad

class SignaturePadExamplesActivity : AppCompatActivity() {

    private lateinit var signaturePad: SignaturePad

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signature_pad_examples)

        signaturePad = findViewById(R.id.signaturePad1)

    }
}