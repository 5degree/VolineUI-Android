package com.cropintellix.volineui

import android.util.Patterns

/**
 * Utility class for validating input field values
 */
object InputValidator {
    
    /**
     * Validates an email address
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Validates a phone number
     * Accepts formats like: (123) 456-7890, 123-456-7890, 1234567890, +1 123 456 7890
     */
    fun isValidPhone(phone: String): Boolean {
        if (phone.isEmpty()) return false
        // Remove common phone number characters for validation
        val cleaned = phone.replace(Regex("[\\s()\\-+]"), "")
        return cleaned.matches(Regex("^\\d{10,15}$"))
    }
    
    /**
     * Validates a URL
     */
    fun isValidUrl(url: String): Boolean {
        return url.isNotEmpty() && Patterns.WEB_URL.matcher(url).matches()
    }
    
    /**
     * Validates against a custom regex pattern
     */
    fun isValidCustomPattern(text: String, pattern: String): Boolean {
        return try {
            text.matches(Regex(pattern))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get default error message for validation type
     */
    fun getDefaultErrorMessage(validationType: ValidationType): String {
        return when (validationType) {
            ValidationType.EMAIL -> "Please enter a valid email address"
            ValidationType.PHONE -> "Please enter a valid phone number"
            ValidationType.URL -> "Please enter a valid URL"
            ValidationType.CUSTOM -> "Invalid input format"
            ValidationType.NONE -> ""
        }
    }
}
