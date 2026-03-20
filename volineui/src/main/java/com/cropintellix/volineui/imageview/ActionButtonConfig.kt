package com.cropintellix.volineui.imageview

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Configuration for a single overlay action chip on [com.cropintellix.volineui.AdvancedImageView]
 * or [com.cropintellix.volineui.compose.AdvancedImageView] (bottom-right row).
 */
data class ActionButtonConfig(
    val iconResId: Int,
    val text: String? = null,
    @param:ColorInt val iconTint: Int = Color.WHITE,
    @param:ColorInt val backgroundColor: Int = 0x66000000,
    @param:ColorInt val textColor: Int = Color.WHITE,
    val contentDescription: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
) {
    fun resolvedContentDescription(): String =
        contentDescription ?: text ?: "Action"
}
