package github.leavesczy.matisse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import github.leavesczy.matisse.internal.logic.SelectionSpec
import github.leavesczy.matisse.internal.ui.MatisseActivity

/**
 * @Author: CZY
 * @Date: 2022/6/2 15:30
 * @Desc:
 */
class MatisseContract : ActivityResultContract<Matisse, List<MediaResource>>() {

    companion object {

        private const val keyResult = "keyResult"

        internal fun buildResult(selectedMediaResources: List<MediaResource>): Intent {
            val data = Intent()
            val resources = arrayListOf<Parcelable>().apply {
                addAll(selectedMediaResources)
            }
            data.putParcelableArrayListExtra(keyResult, resources)
            return data
        }

    }

    override fun createIntent(context: Context, input: Matisse): Intent {
        SelectionSpec.inject(matisse = input)
        return Intent(context, MatisseActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<MediaResource> {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(keyResult, MediaResource::class.java)
            } else {
                intent.getParcelableArrayListExtra(keyResult)
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

}