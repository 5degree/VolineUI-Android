package com.cropintellix.volineui.signaturepad

/**
 * Exception thrown by SignaturePad for validation and export errors
 */
class SignaturePadException(message: String) : Exception(message) {
    companion object {
        const val ERROR_EMPTY_SIGNATURE = "Signature is empty"
        const val ERROR_INSUFFICIENT_STROKES = "Signature does not meet minimum stroke requirement"
        const val ERROR_INVALID_FORMAT = "Invalid export format specified"
        const val ERROR_EXPORT_FAILED = "Failed to export signature"
    }
}