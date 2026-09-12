package github.leavesczy.matisse

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import github.leavesczy.matisse.internal.MatisseCaptureActivity

/**
 * 使用 [MatisseCapture] 配置启动独立拍照流程的 [ActivityResultContract]。
 *
 * 启动后进入拍照流程（可能先按需申请存储写入或相机权限），然后打开系统相机，不显示媒体选择界面。
 * 拍照并成功读取结果时返回 [MediaResource]；用户取消、相机不可用、权限被拒绝或结果无效时返回 null。
 * 此流程不请求媒体读取权限；宿主声明 `CAMERA` 后会按需申请，存储权限和照片存储位置由
 * [MatisseCapture.captureStrategy] 决定。
 */
class MatisseCaptureContract : ActivityResultContract<MatisseCapture, MediaResource?>() {

    override fun createIntent(context: Context, input: MatisseCapture): Intent {
        val intent = Intent(context, MatisseCaptureActivity::class.java)
        intent.putExtra(MatisseCapture::class.java.name, input)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): MediaResource? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            IntentCompat.getParcelableExtra(
                intent,
                MediaResource::class.java.name,
                MediaResource::class.java
            )
        } else {
            null
        }
    }

}