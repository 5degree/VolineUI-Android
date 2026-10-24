@file:Suppress("unused")

package `in`.fivedegree.volineui.ruler

/**
 * Orientation of the ruler scale.
 */
enum class RulerOrientation {
    HORIZONTAL,
    VERTICAL,
}

/**
 * Where tick labels are drawn relative to the scale.
 *
 * For [RulerOrientation.VERTICAL], [ABOVE] maps to the leading side and
 * [BELOW] maps to the trailing side of the ruler.
 *
 * [ALTERNATE] staggers labels across two rows (horizontal) or two columns
 * (vertical) to reduce crowding.
 */
enum class RulerLabelPosition {
    ABOVE,
    BELOW,
    ALTERNATE,
    NONE,
}

/**
 * Visual style of the fixed center indicator.
 */
enum class RulerIndicatorStyle {
    /** A straight line crossing the tick area at the center. */
    LINE,

    /** A line with a small triangle pointing at the selected tick. */
    TRIANGLE,

    /** A line plus a floating value bubble anchored at the center. */
    BUBBLE,

    /** No center indicator. */
    NONE,
}

/**
 * How the currently selected value is displayed.
 */
enum class RulerValueDisplayMode {
    /** Do not render the selected value. */
    NONE,

    /** Render a floating bubble above the center indicator. */
    BUBBLE,

    /** Render the selected value as a header above the scale. */
    HEADER,
}

/**
 * Shape used to draw ticks.
 */
enum class RulerTickShape {
    LINE,
    DOT,
    ROUNDED_LINE,
}

/**
 * Haptic feedback policy while the value changes.
 */
enum class RulerHapticMode {
    NONE,
    STEP,
    MAJOR_STEP,
}
