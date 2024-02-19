package github.leavesczy.matisse.internal.utils

import android.os.SystemClock
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

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
): Modifier {
    return then(other = Modifier.composed {
        var lastClickTime by remember {
            mutableLongStateOf(value = 0L)
        }
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
    })
}

internal inline fun Modifier.clickableLimit(
    minDuration: Long = 300L,
    crossinline onClick: () -> Unit
): Modifier = composed {
    clickableLimit(
        indication = LocalIndication.current,
        interactionSource = remember { MutableInteractionSource() },
        minDuration = minDuration,
        onClick = onClick
    )
}

@Composable
internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return then(
        other = Modifier.clickable(
            onClickLabel = null,
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    )
}