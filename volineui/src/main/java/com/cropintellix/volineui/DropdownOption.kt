@file:Suppress("unused")

package com.cropintellix.volineui

import android.graphics.drawable.Drawable

/**
 * Data class representing a single option in a dropdown menu.
 * 
 * @property text Display text for the option
 * @property value Optional value associated with the option (can be any type)
 * @property description Optional subtitle/description shown below main text
 * @property leadingIcon Optional icon displayed before the text
 * @property trailingIcon Optional icon displayed after the text
 * @property badge Optional badge text/count displayed on the right
 * @property isEnabled Whether this option can be selected
 * @property isHeader Whether this is a section header (non-selectable)
 * @property isDivider Whether this is a visual divider (non-selectable)
 * @property children Optional nested options for hierarchical dropdowns
 * @property groupId Optional group identifier for categorization
 * @property customData Optional map for storing custom metadata
 */
data class DropdownOption(
    val text: String,
    val value: Any? = null,
    val description: String? = null,
    val leadingIcon: Drawable? = null,
    val trailingIcon: Drawable? = null,
    val badge: String? = null,
    val isEnabled: Boolean = true,
    val isHeader: Boolean = false,
    val isDivider: Boolean = false,
    val children: List<DropdownOption>? = null,
    val groupId: String? = null,
    val customData: Map<String, Any>? = null
) {
    /**
     * Returns a flattened list of all options including nested children.
     * Useful for search and filtering operations.
     */
    fun flatten(): List<DropdownOption> {
        val result = mutableListOf<DropdownOption>()
        if (!isHeader && !isDivider) {
            result.add(this)
        }
        children?.forEach { child ->
            result.addAll(child.flatten())
        }
        return result
    }
    
    /**
     * Returns the depth level of this option in the hierarchy (0-based).
     */
    fun getDepth(currentDepth: Int = 0): Int {
        return if (children.isNullOrEmpty()) {
            currentDepth
        } else {
            children.maxOf { it.getDepth(currentDepth + 1) }
        }
    }
    
    companion object {
        /**
         * Creates a simple text-only option.
         */
        @JvmStatic
        fun simple(text: String, value: Any? = null): DropdownOption {
            return DropdownOption(text = text, value = value)
        }
        
        /**
         * Creates a header option for grouping.
         */
        @JvmStatic
        fun header(text: String): DropdownOption {
            return DropdownOption(
                text = text,
                isHeader = true,
                isEnabled = false
            )
        }
        
        /**
         * Creates a divider option.
         */
        @JvmStatic
        fun divider(id: String = "divider_${System.currentTimeMillis()}"): DropdownOption {
            return DropdownOption(
                text = "",
                isDivider = true,
                isEnabled = false
            )
        }
    }
}
