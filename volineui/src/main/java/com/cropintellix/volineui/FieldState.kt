package com.cropintellix.volineui

/**
 * Enum representing the different states of an InputField
 */
enum class FieldState {
    /** Normal state - default appearance */
    NORMAL,
    
    /** Focused state - user is interacting with the field */
    FOCUSED,
    
    /** Error state - validation failed or error shown */
    ERROR,
    
    /** Success state - validation passed */
    SUCCESS,
    
    /** Disabled state - field cannot be interacted with */
    DISABLED,
    
    /** Loading state - async validation in progress */
    LOADING,
    
    /** Read-only state - field can be focused but not edited */
    READ_ONLY
}
