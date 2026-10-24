package `in`.fivedegree.volineui.ruler

/**
 * Exception thrown when [RulerValuePicker] receives an invalid scale configuration.
 */
class RulerValuePickerException(message: String) : IllegalArgumentException(message) {

    companion object {
        /** Creates an exception for an invalid min/max range. */
        fun invalidRange(minValue: Double, maxValue: Double): RulerValuePickerException =
            RulerValuePickerException(
                "RulerValuePicker requires minValue < maxValue, but received " +
                    "minValue=$minValue and maxValue=$maxValue."
            )

        /** Creates an exception for a non-positive or non-finite step. */
        fun invalidStep(step: Double): RulerValuePickerException =
            RulerValuePickerException(
                "RulerValuePicker requires a finite step > 0, but received step=$step."
            )

        /** Creates an exception for a non-positive or non-finite major step. */
        fun invalidMajorStep(majorStep: Double): RulerValuePickerException =
            RulerValuePickerException(
                "RulerValuePicker requires a finite majorStep > 0, but received " +
                    "majorStep=$majorStep."
            )

        /** Creates an exception when [majorStep] is not an exact multiple of [step]. */
        fun majorStepNotMultiple(majorStep: Double, step: Double): RulerValuePickerException =
            RulerValuePickerException(
                "RulerValuePicker requires majorStep to be an exact multiple of step, but " +
                    "received majorStep=$majorStep and step=$step."
            )

        /** Creates an exception for an unsupported decimal count. */
        fun invalidDecimals(decimals: Int): RulerValuePickerException =
            RulerValuePickerException(
                "RulerValuePicker decimals must be between 0 and 6, or -1 for automatic " +
                    "formatting, but received decimals=$decimals."
            )

        /** Creates an exception when the requested range produces too many ticks. */
        fun tooManySteps(stepCount: Double): RulerValuePickerException =
            RulerValuePickerException(
                "RulerValuePicker range produces $stepCount steps, which exceeds the " +
                    "supported maximum. Increase step or reduce the range."
            )

        /** Creates an exception for an invalid tick spacing. */
        fun invalidTickSpacing(spacing: Float): RulerValuePickerException =
            RulerValuePickerException(
                "RulerValuePicker requires tickSpacing > 0, but received $spacing."
            )
    }
}
