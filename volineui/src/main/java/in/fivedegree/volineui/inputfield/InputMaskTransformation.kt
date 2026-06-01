package `in`.fivedegree.volineui.inputfield

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A [VisualTransformation] that applies an input mask pattern to format user input.
 * Supports common patterns like phone numbers and credit cards.
 *
 * @param mask The mask pattern where [maskChar] represents user input positions
 * @param maskChar The character in the mask that represents input positions (default '#')
 *
 * Example:
 * ```
 * InputMaskTransformation("(###) ###-####") // Phone number
 * InputMaskTransformation("#### #### #### ####") // Credit card
 * ```
 */
class InputMaskTransformation(
    private val mask: String,
    private val maskChar: Char = '#'
) : VisualTransformation {
    
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(mask.count { it == maskChar })
        val maskedText = applyMask(trimmed)
        
        return TransformedText(
            text = AnnotatedString(maskedText),
            offsetMapping = InputMaskOffsetMapping(mask, maskChar, trimmed.length)
        )
    }
    
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
    
    /**
     * Offset mapping for cursor position in masked text.
     */
    private class InputMaskOffsetMapping(
        private val mask: String,
        private val maskChar: Char,
        private val inputLength: Int
    ) : OffsetMapping {
        
        override fun originalToTransformed(offset: Int): Int {
            var transformedOffset = 0
            var originalCount = 0
            
            for (i in mask.indices) {
                if (originalCount >= offset) break
                transformedOffset++
                if (mask[i] == maskChar) {
                    originalCount++
                }
            }
            
            // Add any mask characters that come after the current position
            while (transformedOffset < mask.length && mask[transformedOffset] != maskChar) {
                if (originalCount < inputLength) {
                    transformedOffset++
                } else {
                    break
                }
            }
            
            return transformedOffset.coerceAtMost(mask.length)
        }
        
        override fun transformedToOriginal(offset: Int): Int {
            var originalOffset = 0
            
            for (i in 0 until offset.coerceAtMost(mask.length)) {
                if (mask[i] == maskChar) {
                    originalOffset++
                }
            }
            
            return originalOffset.coerceAtMost(inputLength)
        }
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

/**
 * A [VisualTransformation] that immediately masks password characters
 * without showing them briefly like the default implementation.
 */
class ImmediatePasswordTransformation : VisualTransformation {
    
    companion object {
        private const val MASK_CHAR = '\u2022' // Bullet character •
    }
    
    override fun filter(text: AnnotatedString): TransformedText {
        val maskedText = CharArray(text.length) { MASK_CHAR }.concatToString()
        return TransformedText(
            text = AnnotatedString(maskedText),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
