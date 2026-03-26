package com.cropintellix.volineui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cropintellix.volineui.imageview.ActionButtonConfig
import com.cropintellix.volineui.imageview.ImageViewDefaults

@Composable
internal fun ActionButtonChip(
    config: ActionButtonConfig,
    cornerRadius: Dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Row(
        modifier = Modifier
            .heightIn(min = ImageViewDefaults.ActionButtonMinHeight)
            .clip(shape)
            .background(Color(config.backgroundColor))
            .clickable(
                enabled = config.enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { config.onClick() }
            .padding(
                horizontal = ImageViewDefaults.ActionButtonHorizontalPadding,
                vertical = 4.dp
            )
            .alpha(if (config.enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = config.iconResId),
            contentDescription = config.resolvedContentDescription(),
            modifier = Modifier.size(ImageViewDefaults.ActionButtonIconSize),
            tint = Color(config.iconTint)
        )
        if (!config.text.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(ImageViewDefaults.ActionButtonIconTextGap))
            Text(
                text = config.text,
                style = TextStyle(
                    fontSize = ImageViewDefaults.ActionButtonTextSize,
                    color = Color(config.textColor)
                )
            )
        }
    }
}
