package github.leavesczy.matisse.internal.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseTopBarViewState
import github.leavesczy.matisse.internal.utils.clickableNoRipple

/**
 * @Author: leavesCZY
 * @Date: 2021/6/24 16:44
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
@Composable
internal fun MatisseTopBar(matisse: Matisse, topBarViewState: MatisseTopBarViewState) {
    var menuExpanded by remember {
        mutableStateOf(value = false)
    }
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .shadow(elevation = 4.dp)
            .background(color = colorResource(id = R.color.matisse_status_bar_color))
            .statusBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
            .background(color = colorResource(id = R.color.matisse_top_bar_background_color)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(
                modifier = Modifier.padding(start = 6.dp, end = 2.dp),
                content = {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        tint = colorResource(id = R.color.matisse_top_bar_icon_color),
                        contentDescription = null,
                    )
                },
                onClick = {
                    (context as Activity).finish()
                }
            )
            BucketDropdownMenu(
                matisse = matisse,
                topBarViewState = topBarViewState,
                menuExpanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                }
            )
        }
        Row(
            modifier = Modifier
                .padding(end = 30.dp)
                .clickableNoRipple {
                    menuExpanded = true
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = topBarViewState.title
            Text(
                modifier = Modifier.weight(weight = 1f, fill = false),
                text = title,
                style = TextStyle(
                    color = colorResource(id = R.color.matisse_top_bar_text_color),
                    fontSize = 19.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                tint = colorResource(id = R.color.matisse_top_bar_icon_color),
                contentDescription = title
            )
        }
    }
}

@Composable
private fun BucketDropdownMenu(
    matisse: Matisse,
    topBarViewState: MatisseTopBarViewState,
    menuExpanded: Boolean,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(
        modifier = Modifier
            .wrapContentSize(align = Alignment.TopStart)
            .background(color = colorResource(id = R.color.matisse_dropdown_menu_background_color))
            .widthIn(min = 200.dp)
            .heightIn(max = 400.dp),
        offset = DpOffset(x = 10.dp, y = 0.dp),
        expanded = menuExpanded,
        onDismissRequest = onDismissRequest
    ) {
        for (bucket in topBarViewState.mediaBuckets) {
            DropdownMenuItem(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 10.dp, vertical = 4.dp
                ),
                text = {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        matisse.imageEngine.Image(
                            modifier = Modifier
                                .size(size = 54.dp)
                                .clip(shape = RoundedCornerShape(size = 4.dp))
                                .background(color = colorResource(id = R.color.matisse_image_item_background_color)),
                            model = bucket.displayIcon,
                            alignment = Alignment.Center,
                            contentScale = ContentScale.Crop,
                            contentDescription = bucket.displayName
                        )
                        val textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.matisse_dropdown_menu_text_color)
                        )
                        Text(
                            modifier = Modifier
                                .weight(weight = 1f, fill = false)
                                .padding(start = 6.dp),
                            text = bucket.displayName,
                            style = textStyle,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                            text = "(${bucket.resources.size})",
                            style = textStyle,
                            maxLines = 1
                        )
                    }
                }, onClick = {
                    onDismissRequest()
                    topBarViewState.onSelectBucket(bucket)
                })
        }
    }
}