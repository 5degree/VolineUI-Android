package `in`.fivedegree.volineuiandroid

import android.os.Bundle
import android.widget.Toast
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

        setupClickableAccessories()
    }

    private fun setupClickableAccessories() {
        b.clickableIconsInput.onPrefixIconClickListener {
            toast("Prefix icon clicked")
        }
        b.clickableIconsInput.onSuffixIconClickListener {
            toast("Suffix icon clicked")
        }

        b.clickableTrailingTextInput.onTrailingTextClickListener {
            toast("Trailing text clicked")
        }
        b.clickableTrailingTextInput.onSuffixIconClickListener {
            toast("Suffix icon clicked")
        }
        // Leading prefix text mirrors the trailing slot (prefix icon -> text -> divider -> input)
        b.clickableTrailingTextInput.setLeadingText("$")
        b.clickableTrailingTextInput.onLeadingTextClickListener {
            toast("Leading text clicked")
        }

        // Also wire existing fertilizer field so suffix + unit are clickable
        b.fertilizerInput.onTrailingTextClickListener {
            toast("Fertilizer unit clicked")
        }
        b.fertilizerInput.onSuffixIconClickListener {
            toast("Fertilizer suffix icon clicked")
        }

        b.disabledClickableInput.onPrefixIconClickListener {
            toast("Prefix icon clicked (disabled field)")
        }
        b.disabledClickableInput.onSuffixIconClickListener {
            toast("Suffix icon clicked (disabled field)")
        }
        b.disabledClickableInput.onTrailingTextClickListener {
            toast("Trailing text clicked (disabled field)")
        }
        b.disabledClickableInput.setLeadingText("$")
        b.disabledClickableInput.onLeadingTextClickListener {
            toast("Leading text clicked (disabled field)")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}