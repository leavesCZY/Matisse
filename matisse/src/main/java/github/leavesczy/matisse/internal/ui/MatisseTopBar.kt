package github.leavesczy.matisse.internal.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseTopBarViewState

/**
 * @Author: leavesCZY
 * @Date: 2021/6/24 16:44
 * @Desc:
 */
@Composable
internal fun MatisseTopBar(
    modifier: Modifier,
    topBarViewState: MatisseTopBarViewState
) {
    Row(
        modifier = modifier
            .shadow(elevation = 4.dp)
            .background(color = colorResource(id = R.color.matisse_status_bar_color))
            .windowInsetsPadding(insets = WindowInsets.statusBarsIgnoringVisibility)
            .fillMaxWidth()
            .height(height = 56.dp)
            .background(color = colorResource(id = R.color.matisse_top_bar_background_color)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var menuExpanded by remember {
            mutableStateOf(value = false)
        }
        Row(
            modifier = Modifier
                .padding(end = 30.dp)
                .clickableNoRipple {
                    menuExpanded = true
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            val localActivity = LocalActivity.current
            Icon(
                modifier = Modifier
                    .clickableNoRipple {
                        localActivity?.finish()
                    }
                    .padding(start = 18.dp, end = 12.dp)
                    .fillMaxHeight()
                    .size(size = 24.dp),
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                tint = colorResource(id = R.color.matisse_top_bar_icon_color),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .weight(weight = 1f, fill = false),
                text = topBarViewState.title,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colorResource(id = R.color.matisse_top_bar_text_color)
            )
            Icon(
                modifier = Modifier
                    .size(size = 32.dp),
                imageVector = Icons.Filled.ArrowDropDown,
                tint = colorResource(id = R.color.matisse_top_bar_icon_color),
                contentDescription = null
            )
        }
        BucketDropdownMenu(
            modifier = Modifier,
            topBarViewState = topBarViewState,
            menuExpanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
            }
        )
    }
}

@Composable
private fun BucketDropdownMenu(
    modifier: Modifier,
    topBarViewState: MatisseTopBarViewState,
    menuExpanded: Boolean,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(
        modifier = modifier
            .background(color = colorResource(id = R.color.matisse_dropdown_menu_background_color))
            .widthIn(min = 200.dp)
            .heightIn(max = 400.dp),
        expanded = menuExpanded,
        offset = DpOffset(x = 10.dp, y = (-15).dp),
        onDismissRequest = onDismissRequest
    ) {
        for (bucket in topBarViewState.mediaBuckets) {
            DropdownMenuItem(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                text = {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(size = 52.dp)
                                .clip(shape = RoundedCornerShape(size = 4.dp))
                                .background(color = colorResource(id = R.color.matisse_media_item_background_color)),
                            contentAlignment = Alignment.Center
                        ) {
                            val firstResource = bucket.resources.firstOrNull()
                            if (firstResource != null) {
                                topBarViewState.imageEngine.Thumbnail(mediaResource = firstResource)
                            }
                        }
                        Text(
                            modifier = Modifier
                                .weight(weight = 1f, fill = false)
                                .padding(start = 10.dp),
                            text = bucket.name,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Normal,
                            color = colorResource(id = R.color.matisse_dropdown_menu_text_color)
                        )
                        Text(
                            modifier = Modifier
                                .padding(start = 6.dp, end = 6.dp),
                            text = "(${bucket.resources.size})",
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Normal,
                            color = colorResource(id = R.color.matisse_dropdown_menu_text_color)
                        )
                    }
                },
                onClick = {
                    onDismissRequest()
                    topBarViewState.onClickBucket(bucket)
                }
            )
        }
    }
}