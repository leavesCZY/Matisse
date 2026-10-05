package github.leavesczy.matisse.internal.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R

@Composable
internal fun MatisseNoPermissionPlaceholder(
    modifier: Modifier,
    includesImage: Boolean,
    includesVideo: Boolean
) {
    MatissePlaceholder(
        modifier = modifier,
        titleRes = mediaTypeStringRes(
            includesImage = includesImage,
            includesVideo = includesVideo,
            imageRes = R.string.matisse_empty_permission_image_title,
            videoRes = R.string.matisse_empty_permission_video_title,
            mediaRes = R.string.matisse_empty_permission_media_title
        ),
        subtitleRes = mediaTypeStringRes(
            includesImage = includesImage,
            includesVideo = includesVideo,
            imageRes = R.string.matisse_error_permission_image,
            videoRes = R.string.matisse_error_permission_video,
            mediaRes = R.string.matisse_error_permission_media
        )
    )
}

@Composable
internal fun MatisseEmptyPlaceholder(
    modifier: Modifier,
    includesImage: Boolean,
    includesVideo: Boolean
) {
    MatissePlaceholder(
        modifier = modifier,
        titleRes = mediaTypeStringRes(
            includesImage = includesImage,
            includesVideo = includesVideo,
            imageRes = R.string.matisse_empty_image_title,
            videoRes = R.string.matisse_empty_video_title,
            mediaRes = R.string.matisse_empty_media_title
        )
    )
}

@Composable
internal fun MatisseLoadErrorPlaceholder(
    modifier: Modifier,
    onRetry: () -> Unit
) {
    MatissePlaceholder(
        modifier = modifier,
        titleRes = R.string.matisse_empty_error_title,
        subtitleRes = R.string.matisse_empty_error_subtitle,
        onRetry = onRetry
    )
}

@Composable
private fun MatissePlaceholder(
    modifier: Modifier,
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier
                .size(size = 200.dp),
            painter = painterResource(id = R.drawable.ic_matisse_placeholder),
            contentDescription = null
        )
        val titleOnly = subtitleRes == null
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 14.dp,
                    bottom = if (titleOnly) 0.dp else 6.dp
                ),
            text = stringResource(id = titleRes),
            fontSize = if (titleOnly) 16.sp else 18.sp,
            lineHeight = if (titleOnly) 20.sp else 22.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            color = colorResource(
                id = if (titleOnly) {
                    R.color.matisse_empty_subtitle_text_color
                } else {
                    R.color.matisse_empty_title_text_color
                }
            )
        )
        if (subtitleRes != null) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(id = subtitleRes),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.matisse_empty_subtitle_text_color)
            )
        }
        if (onRetry != null) {
            Text(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(shape = CircleShape)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                text = stringResource(id = R.string.matisse_action_retry),
                fontSize = 16.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = colorResource(id = R.color.matisse_empty_retry_text_color)
            )
        }
    }
}

private fun mediaTypeStringRes(
    includesImage: Boolean,
    includesVideo: Boolean,
    imageRes: Int,
    videoRes: Int,
    mediaRes: Int
): Int {
    return if (includesImage && !includesVideo) {
        imageRes
    } else if (!includesImage && includesVideo) {
        videoRes
    } else {
        mediaRes
    }
}
