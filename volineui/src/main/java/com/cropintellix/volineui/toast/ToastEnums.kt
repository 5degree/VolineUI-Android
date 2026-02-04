package com.cropintellix.volineui.toast

/**
 * Toast type enumeration for different visual styles.
 */
enum class ToastType {
    /** Default toast with primary color border */
    DEFAULT,
    /** Success toast with green accent */
    SUCCESS,
    /** Error toast with red accent */
    ERROR,
    /** Warning toast with orange/amber accent */
    WARNING,
    /** Info toast with blue accent */
    INFO
}

/**
 * Toast position on screen.
 */
enum class ToastPosition {
    TOP,
    CENTER,
    BOTTOM
}

/**
 * Toast duration presets.
 */
enum class ToastDuration(val millis: Long) {
    SHORT(2000L),
    MEDIUM(3500L),
    LONG(5000L)
}
