@file:Suppress("unused")

package com.cropintellix.volineui

/**
 * Base exception class for Dropdown-related errors.
 */
open class DropdownException(message: String) : Exception(message)

/**
 * Exception thrown when attempting to use a dropdown with no options.
 */
class EmptyOptionsException : DropdownException(
    "Dropdown must have at least one option. Please provide options before displaying the dropdown."
)

/**
 * Exception thrown when attempting to select an invalid option.
 * 
 * @param optionId The ID of the option that was attempted to be selected
 */
class InvalidSelectionException(optionId: String) : DropdownException(
    "Cannot select option with ID '$optionId'. Option does not exist or is disabled."
)

/**
 * Exception thrown when max selection limit is exceeded in multi-select mode.
 * 
 * @param maxSelections The maximum number of selections allowed
 * @param attempted The number of selections that was attempted
 */
class MaxSelectionExceededException(
    maxSelections: Int,
    attempted: Int
) : DropdownException(
    "Maximum selection limit exceeded. Maximum allowed: $maxSelections, attempted: $attempted."
)

/**
 * Exception thrown when nesting depth exceeds the maximum allowed.
 * 
 * @param maxDepth The maximum nesting depth allowed
 * @param actual The actual depth found
 */
class ExcessiveNestingException(
    maxDepth: Int,
    actual: Int
) : DropdownException(
    "Option nesting exceeds maximum depth. Maximum allowed: $maxDepth, found: $actual levels."
)
