package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatissePlaceholderState

/**
 * @Author: leavesCZY
 * @Date: 2026/6/9 21:46
 * @Desc:
 */
@Composable
internal fun MatissePlaceholder(
    modifier: Modifier,
    placeholderState: MatissePlaceholderState
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier
                .size(size = 200.dp),
            painter = painterResource(id = R.drawable.ic_matisse_empty_gallery),
            contentDescription = stringResource(id = R.string.matisse_cd_empty)
        )
        Spacer(
            modifier = Modifier
                .height(height = 14.dp)
        )
        Text(
            modifier = Modifier,
            text = stringResource(id = placeholderState.titleRes),
            fontSize = 18.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            color = colorResource(id = R.color.matisse_empty_title_text_color)
        )
        Spacer(
            modifier = Modifier
                .height(height = 6.dp)
        )
        Text(
            modifier = Modifier,
            text = stringResource(id = placeholderState.messageRes),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            color = colorResource(id = R.color.matisse_empty_subtitle_text_color)
        )
    }
}
