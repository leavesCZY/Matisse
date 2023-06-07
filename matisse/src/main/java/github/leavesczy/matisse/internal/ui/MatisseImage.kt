package github.leavesczy.matisse.internal.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
internal fun MatisseImage(
    modifier: Modifier,
    model: Uri,
    size: Int = 0,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val request = ImageRequest
        .Builder(context = context)
        .crossfade(enable = false)
        .data(data = model)
        .apply {
            if (size != 0) {
                size(size = size)
            }
        }
        .build()
    AsyncImage(
        modifier = modifier,
        model = request,
        contentScale = contentScale,
        alignment = alignment,
        filterQuality = FilterQuality.None,
        contentDescription = contentDescription
    )
}