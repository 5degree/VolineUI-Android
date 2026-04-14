package com.cropintellix.volineuiandroid

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cropintellix.volineui.AdvancedButton

class ButtonExamplesActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    
    // Button references
    private lateinit var btnFilled: AdvancedButton
    private lateinit var btnOutlined: AdvancedButton
    private lateinit var btnText: AdvancedButton
    private lateinit var btnElevated: AdvancedButton
    private lateinit var btnTonal: AdvancedButton
    private lateinit var btnChip: AdvancedButton
    private lateinit var btnTrailingIcon: AdvancedButton
    private lateinit var btnLeadingIcon: AdvancedButton
    private lateinit var btnBothIcons: AdvancedButton
    private lateinit var btnIconStandard: AdvancedButton
    private lateinit var btnIconFilled: AdvancedButton
    private lateinit var btnIconTonal: AdvancedButton
    private lateinit var btnIconOutlined: AdvancedButton
    private lateinit var btnIconElevated: AdvancedButton
    private lateinit var btnIconTrailingOnly: AdvancedButton
    private lateinit var btnIconXs: AdvancedButton
    private lateinit var btnIconS: AdvancedButton
    private lateinit var btnIconM: AdvancedButton
    private lateinit var btnIconL: AdvancedButton
    private lateinit var btnIconSharp: AdvancedButton
    private lateinit var btnIconRounded: AdvancedButton
    private lateinit var btnIconPill: AdvancedButton
    private lateinit var btnIconDisabled: AdvancedButton
    private lateinit var btnIconLoading: AdvancedButton
    private lateinit var btnIconSuccessError: AdvancedButton
    private lateinit var btnIconInteraction: AdvancedButton
    private lateinit var btnIconGradient: AdvancedButton
    private lateinit var btnLoading: AdvancedButton
    private lateinit var btnSuccessError: AdvancedButton
    private lateinit var btnGradient: AdvancedButton
    private lateinit var btnLoadingSpinner: AdvancedButton
    private lateinit var btnLoadingDots: AdvancedButton
    private lateinit var btnLoadingShimmer: AdvancedButton
    private lateinit var btnInteractions: AdvancedButton
    private lateinit var btnDisabled: AdvancedButton
    
    private var successToggle = true
    private var iconSuccessToggle = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_button_examples)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        
        btnFilled = findViewById(R.id.btnFilled)
        btnOutlined = findViewById(R.id.btnOutlined)
        btnText = findViewById(R.id.btnText)
        btnElevated = findViewById(R.id.btnElevated)
        btnTonal = findViewById(R.id.btnTonal)
        btnChip = findViewById(R.id.btnChip)
        btnTrailingIcon = findViewById(R.id.btnTrailingIcon)
        btnLeadingIcon = findViewById(R.id.btnLeadingIcon)
        btnBothIcons = findViewById(R.id.btnBothIcons)
        btnIconStandard = findViewById(R.id.btnIconStandard)
        btnIconFilled = findViewById(R.id.btnIconFilled)
        btnIconTonal = findViewById(R.id.btnIconTonal)
        btnIconOutlined = findViewById(R.id.btnIconOutlined)
        btnIconElevated = findViewById(R.id.btnIconElevated)
        btnIconTrailingOnly = findViewById(R.id.btnIconTrailingOnly)
        btnIconXs = findViewById(R.id.btnIconXs)
        btnIconS = findViewById(R.id.btnIconS)
        btnIconM = findViewById(R.id.btnIconM)
        btnIconL = findViewById(R.id.btnIconL)
        btnIconSharp = findViewById(R.id.btnIconSharp)
        btnIconRounded = findViewById(R.id.btnIconRounded)
        btnIconPill = findViewById(R.id.btnIconPill)
        btnIconDisabled = findViewById(R.id.btnIconDisabled)
        btnIconLoading = findViewById(R.id.btnIconLoading)
        btnIconSuccessError = findViewById(R.id.btnIconSuccessError)
        btnIconInteraction = findViewById(R.id.btnIconInteraction)
        btnIconGradient = findViewById(R.id.btnIconGradient)
        btnLoading = findViewById(R.id.btnLoading)
        btnSuccessError = findViewById(R.id.btnSuccessError)
        btnGradient = findViewById(R.id.btnGradient)
        btnLoadingSpinner = findViewById(R.id.btnLoadingSpinner)
        btnLoadingDots = findViewById(R.id.btnLoadingDots)
        btnLoadingShimmer = findViewById(R.id.btnLoadingShimmer)
        btnInteractions = findViewById(R.id.btnInteractions)
        btnDisabled = findViewById(R.id.btnDisabled)
    }
    
    private fun setupListeners() {
        // Basic style buttons
        btnFilled.onClickListener {
            updateStatus("Filled button clicked")
        }
        
        btnOutlined.onClickListener {
            updateStatus("Outlined button clicked")
        }
        
        btnText.onClickListener {
            updateStatus("Text button clicked")
        }
        
        btnElevated.onClickListener {
            updateStatus("Elevated button clicked")
        }
        
        btnTonal.onClickListener {
            updateStatus("Tonal button clicked")
        }
        
        btnChip.onClickListener {
            updateStatus("Chip button clicked")
        }
        
        // Icon buttons
        btnLeadingIcon.onClickListener {
            updateStatus("Leading icon button clicked")
        }
        
        // Trailing icon with separate click listener
        btnTrailingIcon.onClickListener {
            updateStatus("Send Message button clicked")
        }
        btnTrailingIcon.onTrailingIconClickListener {
            updateStatus("Trailing icon (send) clicked!")
            showToast("Trailing icon clicked separately!")
        }
        
        // Both icons button
        btnBothIcons.onClickListener {
            updateStatus("Both icons button clicked")
        }
        btnBothIcons.onLeadingIconClickListener {
            updateStatus("Leading icon (edit) clicked!")
            showToast("Edit icon clicked!")
        }
        btnBothIcons.onTrailingIconClickListener {
            updateStatus("Trailing icon (more) clicked!")
            showToast("More icon clicked!")
        }

        btnIconStandard.onClickListener {
            updateStatus("Standard icon button clicked")
        }

        btnIconFilled.onClickListener {
            updateStatus("Filled icon button clicked")
        }

        btnIconTonal.onClickListener {
            updateStatus("Tonal icon button clicked")
        }

        btnIconOutlined.onClickListener {
            updateStatus("Outlined icon button clicked")
        }

        btnIconElevated.onClickListener {
            updateStatus("Elevated icon button clicked")
        }

        listOf(
            btnIconXs to "XS icon button clicked",
            btnIconS to "Small icon button clicked",
            btnIconM to "Medium icon button clicked",
            btnIconL to "Large icon button clicked",
            btnIconSharp to "Sharp icon button clicked",
            btnIconRounded to "Rounded icon button clicked",
            btnIconPill to "Pill icon button clicked",
            btnIconGradient to "Gradient icon button clicked"
        ).forEach { (button, message) ->
            button.onClickListener {
                updateStatus(message)
            }
        }

        btnIconTrailingOnly.onClickListener {
            updateStatus("Trailing-only icon button clicked")
        }
        btnIconTrailingOnly.onTrailingIconClickListener {
            updateStatus("Trailing icon-only glyph clicked")
            showToast("Trailing-only icon callback fired")
        }

        btnIconDisabled.onDisabledClickListener {
            updateStatus("Tried to click disabled icon button")
            showToast("Icon button is disabled")
        }

        btnIconLoading.onClickListener {
            updateStatus("Icon loading started...")
            btnIconLoading.setLoading(true)
            btnIconLoading.postDelayed({
                btnIconLoading.setLoading(false)
                updateStatus("Icon loading completed")
            }, 1800)
        }

        btnIconSuccessError.onClickListener {
            if (iconSuccessToggle) {
                updateStatus("Icon success state")
                btnIconSuccessError.showSuccessState()
            } else {
                updateStatus("Icon error state")
                btnIconSuccessError.showErrorState()
            }
            iconSuccessToggle = !iconSuccessToggle
            btnIconSuccessError.postDelayed({
                btnIconSuccessError.setButtonState(AdvancedButton.ButtonState.NORMAL)
            }, 1600)
        }
        btnIconSuccessError.onSuccessListener {
            updateStatus("Icon success animation completed")
        }
        btnIconSuccessError.onErrorListener {
            updateStatus("Icon error animation completed")
        }

        btnIconInteraction.onClickListener {
            updateStatus("Icon single click detected")
        }
        btnIconInteraction.onDoubleClickListener {
            updateStatus("Icon double click detected")
            showToast("Icon double click!")
        }
        btnIconInteraction.onLongClickListener {
            updateStatus("Icon long press detected")
            showToast("Icon long press!")
            true
        }
        
        // Loading button
        btnLoading.onClickListener {
            updateStatus("Loading started...")
            btnLoading.setLoading(true)
            
            // Simulate async operation
            btnLoading.postDelayed({
                btnLoading.setLoading(false)
                updateStatus("Loading completed!")
            }, 2000)
        }
        
        // Success/Error button
        btnSuccessError.onClickListener {
            if (successToggle) {
                updateStatus("Showing success state...")
                btnSuccessError.showSuccessState()
            } else {
                updateStatus("Showing error state...")
                btnSuccessError.showErrorState()
            }
            successToggle = !successToggle
            
            // Update button text after animation
            btnSuccessError.postDelayed({
                btnSuccessError.setText(if (successToggle) "Click for Success" else "Click for Error")
            }, 2000)
        }
        btnSuccessError.onSuccessListener {
            updateStatus("Success animation completed!")
        }
        btnSuccessError.onErrorListener {
            updateStatus("Error animation completed!")
        }
        
        // Gradient button
        btnGradient.onClickListener {
            updateStatus("Gradient button clicked")
        }
        
        // Loading type buttons
        setupLoadingTypeButtons()
        
        // Interaction demo button
        setupInteractionButton()
        
        // Disabled button click listener
        btnDisabled.onDisabledClickListener {
            showToast("Button is disabled!")
            updateStatus("Tried to click disabled button")
        }
    }
    
    private fun setupLoadingTypeButtons() {
        btnLoadingSpinner.onClickListener {
            btnLoadingSpinner.setLoading(true)
            btnLoadingSpinner.postDelayed({
                btnLoadingSpinner.setLoading(false)
            }, 2000)
        }
        
        btnLoadingDots.onClickListener {
            btnLoadingDots.setLoading(true)
            btnLoadingDots.postDelayed({
                btnLoadingDots.setLoading(false)
            }, 2000)
        }
        
        btnLoadingShimmer.onClickListener {
            btnLoadingShimmer.setLoading(true)
            btnLoadingShimmer.postDelayed({
                btnLoadingShimmer.setLoading(false)
            }, 2000)
        }
    }
    
    private fun setupInteractionButton() {
        btnInteractions.onClickListener {
            updateStatus("Single click detected")
        }
        
        btnInteractions.onDoubleClickListener {
            updateStatus("Double click detected!")
            showToast("Double click!")
        }
        
        btnInteractions.onLongClickListener {
            updateStatus("Long press detected!")
            showToast("Long press!")
            true
        }
    }
    
    private fun updateStatus(message: String) {
        tvStatus.text = "Status: $message"
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
