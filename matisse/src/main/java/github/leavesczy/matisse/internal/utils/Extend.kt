package github.leavesczy.matisse.internal.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MimeType

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
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    }

internal val MediaResource.isVideo: Boolean
    get() = mimeType.startsWith(prefix = "video/")

internal val MimeType.isImage: Boolean
    get() = type.startsWith(prefix = "image/")

internal val MimeType.isVideo: Boolean
    get() = type.startsWith(prefix = "video/")