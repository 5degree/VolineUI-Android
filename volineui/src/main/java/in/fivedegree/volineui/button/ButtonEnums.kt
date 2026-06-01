@file:Suppress("unused")

package `in`.fivedegree.volineui.button

/**
 * Button style variants matching the View-based AdvancedButton.
 */
enum class ButtonStyle(val value: Int) {
    FILLED(0),
    OUTLINED(1),
    TEXT(2),
    ELEVATED(3),
    TONAL(4),
    ICON(5),
    FAB(6),
    EXTENDED_FAB(7),
    CHIP(8);
    
    companion object {
        fun fromValue(value: Int): ButtonStyle = entries.find { it.value == value } ?: FILLED
    }
}

/**
 * Visual appearance variants for icon-only buttons.
 */
enum class IconButtonAppearance(val value: Int) {
    STANDARD(0),
    FILLED(1),
    TONAL(2),
    OUTLINED(3),
    ELEVATED(4);

    companion object {
        fun fromValue(value: Int): IconButtonAppearance =
            entries.find { it.value == value } ?: STANDARD
    }
}

/**
 * Button size presets.
 */
enum class ButtonSize(val value: Int) {
    XS(0),
    S(1),
    M(2),
    L(3),
    XL(4);
    
    companion object {
        fun fromValue(value: Int): ButtonSize = entries.find { it.value == value } ?: M
    }
}

/**
 * Button visual/interaction states.
 */
enum class ButtonState {
    NORMAL,
    PRESSED,
    DISABLED,
    LOADING,
    SUCCESS,
    ERROR
}

/**
 * Loading animation type.
 */
enum class LoadingType(val value: Int) {
    SPINNER(0),
    DOTS(1),
    SHIMMER(2),
    PROGRESS(3);
    
    companion object {
        fun fromValue(value: Int): LoadingType = entries.find { it.value == value } ?: SPINNER
    }
}

/**
 * Haptic feedback intensity levels.
 */
enum class HapticIntensity(val value: Int) {
    NONE(0),
    LIGHT(1),
    MEDIUM(2),
    HEAVY(3);
    
    companion object {
        fun fromValue(value: Int): HapticIntensity = entries.find { it.value == value } ?: MEDIUM
    }
}

/**
 * Text transformation options.
 */
enum class TextTransform(val value: Int) {
    NONE(0),
    UPPERCASE(1),
    LOWERCASE(2),
    CAPITALIZE(3);
    
    companion object {
        fun fromValue(value: Int): TextTransform = entries.find { it.value == value } ?: NONE
    }
}

/**
 * Corner shape types.
 */
enum class CornerType(val value: Int) {
    SHARP(0),
    ROUNDED(1),
    PILL(2);
    
    companion object {
        fun fromValue(value: Int): CornerType = entries.find { it.value == value } ?: ROUNDED
    }
}
