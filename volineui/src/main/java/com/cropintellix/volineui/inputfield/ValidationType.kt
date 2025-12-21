package com.cropintellix.volineui.inputfield

/**
 * Enum representing different validation types supported by InputField
 */
enum class ValidationType {
    /** No validation */
    NONE,

    /** Email address validation */
    EMAIL,

    /** Phone number validation */
    PHONE,

    /** URL validation */
    URL,

    /** Custom regex pattern validation */
    CUSTOM
}