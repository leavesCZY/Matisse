package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MediaBucket

/**
 * @Author: leavesCZY
 * @Date: 2021/6/24 16:44
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
@Composable
internal fun MatisseTopBar(
    allBucket: List<MediaBucket>,
    selectedBucket: MediaBucket,
    onClickBackMenu: () -> Unit,
    onSelectBucket: (MediaBucket) -> Unit
) {
    var menuExpanded by remember {
        mutableStateOf(value = false)
    }
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
                modifier = Modifier.padding(start = 6.dp, end = 2.dp), content = {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIos,
                        tint = colorResource(id = R.color.matisse_top_bar_icon_color),
                        contentDescription = "Back",
                    )
                }, onClick = onClickBackMenu
            )
            BucketDropdownMenu(
                allBucket = allBucket, menuExpanded = menuExpanded, onDismissRequest = {
                    menuExpanded = false
                }, onSelectBucket = onSelectBucket
            )
        }
        Row(modifier = Modifier
            .padding(end = 30.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                menuExpanded = true
            }, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f, fill = false),
                text = selectedBucket.bucketDisplayName,
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
                contentDescription = selectedBucket.bucketDisplayName
            )
        }
    }
}

@Composable
private fun BucketDropdownMenu(
    allBucket: List<MediaBucket>,
    menuExpanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelectBucket: (MediaBucket) -> Unit
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
        allBucket.forEach { bucket ->
            DropdownMenuItem(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(
                horizontal = 10.dp, vertical = 4.dp
            ), text = {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .size(size = 54.dp)
                            .clip(shape = RoundedCornerShape(size = 4.dp))
                            .background(color = colorResource(id = R.color.matisse_image_item_background_color)),
                        model = bucket.bucketDisplayIcon,
                        contentScale = ContentScale.Crop,
                        contentDescription = bucket.bucketDisplayName
                    )
                    val textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.matisse_dropdown_menu_text_color)
                    )
                    Text(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(start = 6.dp),
                        text = bucket.bucketDisplayName,
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
                onSelectBucket(bucket)
            })
        }
    }
}