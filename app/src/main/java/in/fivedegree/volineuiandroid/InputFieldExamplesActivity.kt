package `in`.fivedegree.volineuiandroid

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import `in`.fivedegree.volineuiandroid.databinding.ActivityInputFieldExamplesBinding

class InputFieldExamplesActivity : AppCompatActivity() {

    private var binding: ActivityInputFieldExamplesBinding? = null
    private val b get() = binding!!

    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInputFieldExamplesBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupDemo()
    }

    private fun setupDemo() {
        b.btnShowError.setOnClickListener {
            b.emailInput.showError("Invalid email format")
        }

        // Show success button
        b.btnShowSuccess.setOnClickListener {
            b.emailInput.setSuccess()
        }

        // Toggle loading button
        b.btnShowLoading.setOnClickListener {
            isLoading = !isLoading
            b.basicInput.setLoading(isLoading)
            b.btnShowLoading.text = if (isLoading) "Stop Loading" else "Toggle Loading"
        }
    }
}