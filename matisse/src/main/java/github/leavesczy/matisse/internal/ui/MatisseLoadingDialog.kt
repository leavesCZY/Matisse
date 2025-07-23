package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.R

/**
 * @Author: leavesCZY
 * @Desc:
 */
@Composable
internal fun MatisseLoadingDialog(
    modifier: Modifier,
    visible: Boolean
) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickableNoRipple {},
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(size = 42.dp),
                strokeWidth = 3.dp,
                color = colorResource(id = R.color.matisse_circular_loading_color),
                trackColor = Color.Transparent,
                strokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap
            )
        }
    }
}
