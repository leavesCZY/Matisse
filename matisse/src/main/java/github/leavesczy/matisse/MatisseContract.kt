package github.leavesczy.matisse

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import github.leavesczy.matisse.internal.MatisseActivity

/**
 * 使用 [Matisse] 配置启动图片和视频选择器的 [ActivityResultContract]。
 *
 * 选择完成时返回非空的 [MediaResource] 列表；Activity 未以成功结果结束、结果 Intent 缺失或
 * 结果列表为空时返回 null。权限被拒或媒体加载失败不会自动结束选择器，用户返回后结果为 null。
 * 宿主应用需要提前在 Manifest 中声明 [Matisse.mediaType] 对应的媒体读取权限，权限申请由选择器完成；
 * 具体权限规则参见 [Matisse]。选择器界面固定为竖屏。
 */
class MatisseContract : ActivityResultContract<Matisse, List<MediaResource>?>() {

    override fun createIntent(context: Context, input: Matisse): Intent {
        val intent = Intent(context, MatisseActivity::class.java)
        intent.putExtra(Matisse::class.java.name, input)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<MediaResource>? {
        val result = if (resultCode == Activity.RESULT_OK && intent != null) {
            IntentCompat.getParcelableArrayListExtra(
                intent,
                MediaResource::class.java.name,
                MediaResource::class.java
            )
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