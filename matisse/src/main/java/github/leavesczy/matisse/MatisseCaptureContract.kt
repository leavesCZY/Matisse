package github.leavesczy.matisse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import github.leavesczy.matisse.internal.MatisseCaptureActivity

/**
 * @Author: CZY
 * @Date: 2023/4/11 16:38
 * @Desc:
 */
class MatisseCaptureContract : ActivityResultContract<CaptureStrategy, MediaResource?>() {

    companion object {

        private const val keyRequest = "keyRequest"

        private const val keyResult = "keyResult"

        internal fun getRequest(intent: Intent): CaptureStrategy {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(keyRequest, CaptureStrategy::class.java)
            } else {
                intent.getParcelableExtra(keyRequest)
            }!!
        }

        internal fun buildResult(mediaResource: MediaResource): Intent {
            val intent = Intent()
            intent.putExtra(keyResult, mediaResource)
            return intent
        }

    }

    override fun getSynchronousResult(
        context: Context,
        input: CaptureStrategy
    ): SynchronousResult<MediaResource?>? {
        if (input.isEnabled()) {
            return super.getSynchronousResult(context, input)
        }
        return SynchronousResult(value = null)
    }

    override fun createIntent(context: Context, input: CaptureStrategy): Intent {
        val intent = Intent(context, MatisseCaptureActivity::class.java)
        intent.putExtra(keyRequest, input)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): MediaResource? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(keyResult, MediaResource::class.java)
            } else {
                intent.getParcelableExtra(keyResult)
            }
        } else {
            null
        }
    }

}