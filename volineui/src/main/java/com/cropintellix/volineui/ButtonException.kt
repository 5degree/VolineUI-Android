package com.cropintellix.volineui

/**
 * Exception thrown for button-related errors
 */
class ButtonException(message: String) : Exception(message) {
    companion object {
        fun invalidState(currentState: String, requestedAction: String): ButtonException {
            return ButtonException("Cannot perform '$requestedAction' while button is in '$currentState' state")
        }
        
        fun invalidConfiguration(reason: String): ButtonException {
            return ButtonException("Invalid button configuration: $reason")
        }
    }
}
