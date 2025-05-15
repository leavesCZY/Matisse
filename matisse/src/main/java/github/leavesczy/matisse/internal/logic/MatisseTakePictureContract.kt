package github.leavesczy.matisse.internal.logic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

/**
 * @Author: leavesCZY
 * @Date: 2023/6/28 17:40
 * @Desc:
 */
internal class MatisseTakePictureContract :
    ActivityResultContract<MatisseTakePictureContract.MatisseTakePictureContractParams, Boolean>() {

    data class MatisseTakePictureContractParams(
        val uri: Uri,
        val extra: Bundle
    )

    override fun createIntent(context: Context, input: MatisseTakePictureContractParams): Intent {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val extra = input.extra
        if (!extra.isEmpty) {
            intent.putExtras(extra)
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, input.uri)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }

}