package github.leavesczy.matisse.internal.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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

@Composable
internal fun MatisseNoPermissionPlaceholder(modifier: Modifier) {
    MatissePlaceholder(
        modifier = modifier,
        titleRes = R.string.matisse_empty_no_permission_title,
        subtitleRes = R.string.matisse_empty_no_permission_subtitle
    )
}

@Composable
internal fun MatisseEmptyPlaceholder(
    modifier: Modifier,
    requestImage: Boolean,
    requestVideo: Boolean
) {
    val titleRes: Int
    val subtitleRes: Int
    if (requestImage && requestVideo) {
        titleRes = R.string.matisse_empty_no_media_title
        subtitleRes = R.string.matisse_empty_no_media_subtitle
    } else if (requestVideo) {
        titleRes = R.string.matisse_empty_no_video_title
        subtitleRes = R.string.matisse_empty_no_video_subtitle
    } else {
        titleRes = R.string.matisse_empty_no_image_title
        subtitleRes = R.string.matisse_empty_no_image_subtitle
    }
    MatissePlaceholder(
        modifier = modifier,
        titleRes = titleRes,
        subtitleRes = subtitleRes
    )
}

@Composable
private fun MatissePlaceholder(
    modifier: Modifier,
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int
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
        Text(
            modifier = Modifier
                .padding(top = 14.dp, bottom = 6.dp),
            text = stringResource(id = titleRes),
            fontSize = 18.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            color = colorResource(id = R.color.matisse_empty_title_text_color)
        )
        Text(
            modifier = Modifier,
            text = stringResource(id = subtitleRes),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            color = colorResource(id = R.color.matisse_empty_subtitle_text_color)
        )
    }
}
