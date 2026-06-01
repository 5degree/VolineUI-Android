package `in`.fivedegree.volineuiandroid

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import `in`.fivedegree.volineui.Radio

class RadioExamplesActivity : AppCompatActivity() {
    
    private lateinit var roleRadio: Radio
    private lateinit var timePeriodRadio: Radio
    private lateinit var sizeRadio: Radio
    private lateinit var planRadio: Radio
    private lateinit var difficultyRadio: Radio
    private lateinit var customRadio: Radio
    
    private lateinit var tvSelectedValue: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_radio_examples)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize Radios
        roleRadio = findViewById(R.id.roleRadio)
        timePeriodRadio = findViewById(R.id.timePeriodRadio)
        sizeRadio = findViewById(R.id.sizeRadio)
        planRadio = findViewById(R.id.planRadio)
        difficultyRadio = findViewById(R.id.difficultyRadio)
        customRadio = findViewById(R.id.customRadio)
        
        // Initialize TextView
        tvSelectedValue = findViewById(R.id.tvSelectedValue)
        
        // Setup listeners for each Radio
//        setupListeners()
        
        // Setup demo buttons
        setupDemoButtons()
    }
    
    private fun setupListeners() {
        roleRadio.setOnValueChangeListener { value, index ->
            showToast("Role: $value (index: $index)")
        }
        
        timePeriodRadio.setOnValueChangeListener { value, index ->
            showToast("Time Period: $value")
        }
        
        sizeRadio.setOnValueChangeListener { value, index ->
            showToast("Size: $value")
        }
        
        planRadio.setOnValueChangeListener { value, index ->
            showToast("Plan: $value")
        }
        
        difficultyRadio.setOnValueChangeListener { value, index ->
            showToast("Difficulty: $value")
        }
        
        customRadio.setOnValueChangeListener { value, index ->
            showToast("Custom: $value")
        }
    }
    
    private fun setupDemoButtons() {
        val btnSelectFirst: Button = findViewById(R.id.btnSelectFirst)
        val btnSelectLast: Button = findViewById(R.id.btnSelectLast)
        val btnGetValue: Button = findViewById(R.id.btnGetValue)
        
        btnSelectFirst.setOnClickListener {
            timePeriodRadio.setSelectedIndex(2, animated = true)
        }
        
        btnSelectLast.setOnClickListener {
            // Get the last index of the roleRadio options
            val lastIndex = 2 // Since we know it has 3 options (Admin, Editor, Viewer)
            roleRadio.setSelectedIndex(lastIndex, animated = true)
        }
        
        btnGetValue.setOnClickListener {
            val value = roleRadio.getSelectedValue()
            val index = roleRadio.getSelectedIndex()
            tvSelectedValue.text = "Selected: $value (Index: $index)"
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}