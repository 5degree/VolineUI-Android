package com.cropintellix.volineuiandroid

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cropintellix.volineui.InputField

class MainActivity : AppCompatActivity() {
    
    private var isLoading = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupDemo()
    }
    
    private fun setupDemo() {
        val emailInput = findViewById<InputField>(R.id.emailInput)
        val basicInput = findViewById<InputField>(R.id.basicInput)
        
        val btnShowError = findViewById<Button>(R.id.btnShowError)
        val btnShowSuccess = findViewById<Button>(R.id.btnShowSuccess)
        val btnShowLoading = findViewById<Button>(R.id.btnShowLoading)

        // Show error button
        btnShowError.setOnClickListener {
            emailInput.showError("Invalid email format")
        }
        
        // Show success button
        btnShowSuccess.setOnClickListener {
            emailInput.setSuccess()
        }
        
        // Toggle loading button
        btnShowLoading.setOnClickListener {
            isLoading = !isLoading
            basicInput.setLoading(isLoading)
            btnShowLoading.text = if (isLoading) "Stop Loading" else "Toggle Loading"
        }
    }
}