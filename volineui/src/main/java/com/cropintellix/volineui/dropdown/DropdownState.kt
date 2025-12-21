@file:Suppress("unused")

package com.cropintellix.volineui.dropdown

/**
 * Enum representing the various states a Dropdown component can be in.
 * 
 * States affect the visual appearance and behavior of the dropdown:
 * - NORMAL: Default idle state
 * - FOCUSED: When trigger receives focus
 * - EXPANDED: When dropdown menu is open
 * - ERROR: When validation fails or error is set
 * - SUCCESS: When validation succeeds
 * - DISABLED: When component is disabled
 * - LOADING: When async options are loading
 * - READ_ONLY: When component can show selection but not change it
 */
enum class DropdownState {
    NORMAL,
    FOCUSED,
    ERROR,
    SUCCESS,
    DISABLED,
    LOADING,
    READ_ONLY
}
