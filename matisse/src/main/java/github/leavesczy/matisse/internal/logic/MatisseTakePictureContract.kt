package github.leavesczy.matisse.internal.logic

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

internal class MatisseTakePictureContract :
    ActivityResultContract<MatisseTakePictureContract.Params, Boolean>() {

    data class Params(
        val uri: Uri,
        val extra: Bundle
    )

    override fun createIntent(context: Context, input: Params): Intent {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val extra = input.extra
        if (!extra.isEmpty) {
            intent.putExtras(extra)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.clipData = ClipData.newUri(context.contentResolver, "Photo", input.uri)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, input.uri)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }

}