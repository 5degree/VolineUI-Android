package com.cropintellix.volineui.radio

/**
 * Exception thrown when RadioButton component receives invalid options configuration.
 *
 * Common scenarios:
 * - Less than 2 options provided (minimum required)
 * - Null or empty options array
 */
class RadioOptionsException(message: String) : IllegalArgumentException(message) {

    companion object {
        /**
         * Creates exception for insufficient options
         */
        fun insufficientOptions(count: Int): RadioOptionsException {
            return RadioOptionsException(
                "RadioButton requires at least 2 options, but $count were provided. " +
                "Please provide a valid string array with 2 or more options."
            )
        }

        /**
         * Creates exception for null/empty options
         */
        fun emptyOptions(): RadioOptionsException {
            return RadioOptionsException(
                "RadioButton options cannot be null or empty. " +
                "Please provide a valid string array with at least 2 options."
            )
        }
    }
}