package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import github.leavesczy.matisse.internal.logic.MediaBucket
import github.leavesczy.matisse.internal.theme.LocalMatisseTheme

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
    val topAppBarTheme = LocalMatisseTheme.current.topAppBarTheme
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
            .shadow(elevation = 1.dp)
            .background(color = topAppBarTheme.backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier) {
            IconButton(
                content = {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIos,
                        tint = topAppBarTheme.contentColor,
                        contentDescription = "Back",
                    )
                },
                onClick = onClickBackMenu,
            )
            BucketDropdownMenu(
                allBucket = allBucket,
                menuExpanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                },
                onSelectBucket = onSelectBucket,
            )
        }
        Row(
            modifier = Modifier
                .padding(end = 30.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    menuExpanded = true
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f, fill = false),
                text = selectedBucket.bucketDisplayName,
                color = topAppBarTheme.contentColor,
                fontSize = topAppBarTheme.fontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                tint = topAppBarTheme.contentColor,
                contentDescription = selectedBucket.bucketDisplayName,
            )
        }
    }
}

@Composable
private fun BucketDropdownMenu(
    allBucket: List<MediaBucket>,
    menuExpanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelectBucket: (MediaBucket) -> Unit,
) {
    val dropdownMenuTheme = LocalMatisseTheme.current.dropdownMenuTheme
    DropdownMenu(
        modifier = Modifier
            .wrapContentSize(align = Alignment.TopStart)
            .background(color = dropdownMenuTheme.backgroundColor)
            .widthIn(min = 200.dp)
            .heightIn(max = 400.dp),
        offset = DpOffset(x = 10.dp, y = 0.dp),
        expanded = menuExpanded,
        onDismissRequest = onDismissRequest
    ) {
        allBucket.forEach { bucket ->
            DropdownMenuItem(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 10.dp,
                    vertical = 4.dp
                ),
                content = {
                    AsyncImage(
                        modifier = Modifier
                            .size(size = 54.dp)
                            .clip(shape = RoundedCornerShape(size = 4.dp))
                            .background(color = LocalMatisseTheme.current.imageBackgroundColor),
                        model = bucket.bucketDisplayIcon,
                        contentScale = ContentScale.Crop,
                        contentDescription = bucket.bucketDisplayName,
                    )
                    Text(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(start = 6.dp),
                        text = bucket.bucketDisplayName,
                        style = dropdownMenuTheme.textStyle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                        text = "(${bucket.resources.size})",
                        style = dropdownMenuTheme.textStyle,
                        maxLines = 1,
                    )
                }, onClick = {
                    onDismissRequest()
                    onSelectBucket(bucket)
                })
        }
    }
}