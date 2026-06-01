package `in`.fivedegree.volineui.inputfield

import android.text.Editable
import android.text.TextWatcher

/**
 * Utility class for applying input masks to format user input
 * Supports common patterns like phone numbers and credit cards
 */
class InputMask(private val mask: String, private val maskChar: Char = '#') : TextWatcher {
    
    private var isUpdating = false
    private var oldText = ""
    
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        if (!isUpdating) {
            oldText = s?.toString() ?: ""
        }
    }
    
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Not used
    }
    
    override fun afterTextChanged(s: Editable?) {
        if (isUpdating || s == null) return
        
        isUpdating = true
        
        val unmasked = unmask(s.toString())
        val masked = applyMask(unmasked)
        
        if (s.toString() != masked) {
            s.replace(0, s.length, masked)
        }
        
        isUpdating = false
    }
    
    /**
     * Remove mask characters from input
     */
    private fun unmask(text: String): String {
        return text.filter { it.isLetterOrDigit() }
    }
    
    /**
     * Apply mask pattern to unmasked input
     */
    private fun applyMask(unmasked: String): String {
        if (mask.isEmpty()) return unmasked
        
        val result = StringBuilder()
        var unmaskedIndex = 0
        
        for (maskCharacter in mask) {
            if (unmaskedIndex >= unmasked.length) break
            
            if (maskCharacter == maskChar) {
                result.append(unmasked[unmaskedIndex])
                unmaskedIndex++
            } else {
                result.append(maskCharacter)
            }
        }
        
        return result.toString()
    }
    
    companion object {
        /** Common mask patterns */
        const val PHONE_MASK = "(###) ###-####"
        const val CREDIT_CARD_MASK = "#### #### #### ####"
        const val DATE_MASK = "##/##/####"
        const val TIME_MASK = "##:##"
        const val SSN_MASK = "###-##-####"
        
        /**
         * Get maximum unmasked length for a given mask
         */
        fun getMaxLength(mask: String, maskChar: Char = '#'): Int {
            return mask.count { it == maskChar }
        }
    }
}
