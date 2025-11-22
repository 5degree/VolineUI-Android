package com.cropintellix.volineuiandroid

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cropintellix.volineui.Dropdown
import com.cropintellix.volineui.DropdownOption
import com.cropintellix.volineuiandroid.databinding.ActivityDropdownExamplesBinding

class DropdownExamplesActivity : AppCompatActivity() {

    private var binding: ActivityDropdownExamplesBinding? = null
    private val b get() = binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDropdownExamplesBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}