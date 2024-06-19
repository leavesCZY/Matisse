package github.leavesczy.matisse.internal.custom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.result.contract.ActivityResultContract

class SettingsActivityResultContract : ActivityResultContract<Unit?, Boolean>() {


    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        // 如果用户从设置页面返回，通常会看到resultCode为RESULT_OK
        return resultCode == Activity.RESULT_OK
    }
}