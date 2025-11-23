package com.cropintellix.volineuiandroid

import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cropintellix.volineui.Dropdown
import com.cropintellix.volineui.DropdownOption
import com.cropintellix.volineuiandroid.databinding.ActivityDropdownExamplesBinding
import org.xmlpull.v1.XmlPullParser

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

        val countries = listOf("India", "USA", "Germany", "France", "Japan", "China", "Brazil", "Russia", "Australia", "Canada", "South Korea", "Netherlands", "Sweden", "Norway", "Denmark", "Finland", "Ireland")
        val options = countries.map {
            DropdownOption(
                text = it,
                children = if (it == "Russia") listOf(DropdownOption("Russia A"), DropdownOption("Russia B"), DropdownOption("Russia C")) else null,
                groupId = null,
                customData = null
            )
        }
        b.dropdown2.setOptions(options)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}