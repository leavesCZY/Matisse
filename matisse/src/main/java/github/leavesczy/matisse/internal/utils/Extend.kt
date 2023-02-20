package github.leavesczy.matisse.internal.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * @Author: CZY
 * @Date: 2023/2/19 18:56
 * @Desc:
 */
internal fun Modifier.clickableNoRipple(
    onClick: () -> Unit
): Modifier =
    composed {
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }) {
            onClick()
        }
    }