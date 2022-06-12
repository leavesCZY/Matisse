package github.leavesczy.matisse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import github.leavesczy.matisse.internal.MatisseActivity
import github.leavesczy.matisse.internal.SelectionSpec

/**
 * @Author: CZY
 * @Date: 2022/6/2 15:30
 * @Desc:
 */
class MatisseContract : ActivityResultContract<Matisse, List<MediaResources>>() {

    companion object {

        private const val keyResult = "keyResult"

        internal fun buildResult(selectedMediaResources: List<MediaResources>): Intent {
            val data = Intent()
            data.putParcelableArrayListExtra(
                keyResult,
                arrayListOf<Parcelable>().apply {
                    addAll(selectedMediaResources)
                }
            )
            return data
        }

    }

    override fun createIntent(context: Context, input: Matisse): Intent {
        SelectionSpec.inject(matisse = input)
        return Intent(context, MatisseActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<MediaResources> {
        return if (resultCode != Activity.RESULT_OK || intent == null) {
            emptyList()
        } else {
            intent.getParcelableArrayListExtra<MediaResources>(keyResult)!!.toList()
        }
    }

}