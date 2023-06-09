package github.leavesczy.matisse.samples.engine

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import github.leavesczy.matisse.ImageEngine
import kotlinx.parcelize.Parcelize

@Parcelize
class GlideImageEngine : ImageEngine {

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    override fun Image(
        modifier: Modifier,
        model: Uri,
        contentScale: ContentScale,
        contentDescription: String?
    ) {
        GlideImage(
            modifier = modifier,
            model = model,
            contentScale = contentScale,
            contentDescription = contentDescription
        )
    }

}