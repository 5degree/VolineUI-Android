package com.cropintellix.volinecore.remotelogger

/**
 * Utility for redacting sensitive HTTP header values in API log entries.
 *
 * Header names in [sensitiveHeaders] are matched case-insensitively.
 * Their values are replaced with `[REDACTED]`.
 */
internal object HeaderRedactor {

    private const val REDACTED = "[REDACTED]"

    /**
     * Returns a copy of [headers] with values of sensitive keys replaced.
     *
     * @param headers Original header map.
     * @param sensitiveHeaders Set of header names to redact (case-insensitive).
     */
    fun redact(
        headers: Map<String, String>,
        sensitiveHeaders: Set<String>
    ): Map<String, String> {
        if (headers.isEmpty() || sensitiveHeaders.isEmpty()) return headers

        val sensitiveNormalized = sensitiveHeaders.map { it.lowercase() }.toSet()
        return headers.mapValues { (key, value) ->
            if (key.lowercase() in sensitiveNormalized) REDACTED else value
        }
    }
}
