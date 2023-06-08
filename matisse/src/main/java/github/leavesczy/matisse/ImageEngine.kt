package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * @Author: leavesCZY
 * @Desc:
 */
interface ImageEngine : Parcelable {

    @Composable
    fun Image(
        modifier: Modifier,
        model: Uri,
        contentScale: ContentScale,
        contentDescription: String?
    )

}