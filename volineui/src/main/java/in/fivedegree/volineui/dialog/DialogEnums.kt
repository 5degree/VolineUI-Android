package `in`.fivedegree.volineui.dialog

/**
 * Dialog type enumeration for different visual styles.
 */
enum class DialogType {
    /** Default dialog with primary color accent */
    DEFAULT,
    /** Success dialog with green accent */
    SUCCESS,
    /** Error dialog with red accent */
    ERROR,
    /** Warning dialog with orange/amber accent */
    WARNING,
    /** Info dialog with blue accent */
    INFO,
    /** Confirmation dialog */
    CONFIRMATION,
    /** Destructive action dialog */
    DESTRUCTIVE
}