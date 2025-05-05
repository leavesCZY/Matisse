package github.leavesczy.matisse.samples.logic

import android.content.Context
import android.os.Bundle
import android.os.Environment
import github.leavesczy.matisse.FileProviderCaptureStrategy
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * @Author: CZY
 * @Date: 2025/5/4 18:30
 * @Desc:
 */
@Parcelize
data class CustomFileProviderCaptureStrategy(
    private val authority: String,
    private val extra: Bundle
) : FileProviderCaptureStrategy(
    authority = authority,
    extra = extra
) {

    override fun getAuthorityDirectory(context: Context): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    }

}