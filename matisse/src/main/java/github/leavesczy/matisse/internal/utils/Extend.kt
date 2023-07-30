package github.leavesczy.matisse.internal.utils

import android.os.SystemClock
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MimeType

/**
 * @Author: CZY
 * @Date: 2023/2/19 18:56
 * @Desc:
 */
private inline fun Modifier.clickableLimit(
    indication: Indication?,
    interactionSource: MutableInteractionSource,
    minDuration: Long,
    crossinline onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableStateOf(value = 0L) }
    clickable(
        indication = indication,
        interactionSource = interactionSource
    ) {
        val currentTimeMillis = SystemClock.elapsedRealtime()
        if (currentTimeMillis - lastClickTime > minDuration) {
            lastClickTime = currentTimeMillis
            onClick()
        }
    }
}

private const val MIN_DURATION = 300L

internal inline fun Modifier.clickableLimit(
    minDuration: Long = MIN_DURATION,
    crossinline onClick: () -> Unit
): Modifier = composed {
    clickableLimit(
        indication = LocalIndication.current,
        interactionSource = remember { MutableInteractionSource() },
        minDuration = minDuration,
        onClick = onClick
    )
}

internal fun Modifier.clickableNoRippleLimit(
    minDuration: Long = MIN_DURATION,
    onClick: () -> Unit
): Modifier = composed {
    clickableLimit(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        minDuration = minDuration,
        onClick = onClick
    )
}

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