package github.leavesczy.matisse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import github.leavesczy.matisse.internal.MatisseActivity

/**
 * @Author: CZY
 * @Date: 2022/6/2 15:30
 * @Desc:
 */
class MatisseContract : ActivityResultContract<Matisse, List<MediaResource>?>() {

    companion object {

        private const val keyRequest = "keyRequest"

        private const val keyResult = "keyResult"

        internal fun getRequest(intent: Intent): Matisse {
            return intent.getParcelableExtra(keyRequest)!!
        }

        internal fun buildResult(selectedMediaResources: List<MediaResource>): Intent {
            val intent = Intent()
            val resources = arrayListOf<Parcelable>().apply {
                addAll(selectedMediaResources)
            }
            intent.putParcelableArrayListExtra(keyResult, resources)
            return intent
        }

    }

    override fun createIntent(context: Context, input: Matisse): Intent {
        val intent = Intent(context, MatisseActivity::class.java)
        intent.putExtra(keyRequest, input)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<MediaResource>? {
        val result = if (resultCode == Activity.RESULT_OK && intent != null) {
            intent.getParcelableArrayListExtra<MediaResource>(keyResult)
        } else {
            null
        }
        return if (result.isNullOrEmpty()) {
            null
        } else {
            result
        }
    }

}