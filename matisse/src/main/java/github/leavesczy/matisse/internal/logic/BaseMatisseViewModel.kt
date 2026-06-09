package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel

/**
 * @Author: leavesCZY
 * @Date: 2026/6/9 21:28
 * @Desc:
 */
internal abstract class BaseMatisseViewModel(application: Application) :
    AndroidViewModel(application) {

    protected val context: Context
        get() = getApplication()

    var loadingDialogVisible by mutableStateOf(value = false)
        private set

    protected fun showLoadingDialog() {
        loadingDialogVisible = true
    }

    protected fun dismissLoadingDialog() {
        loadingDialogVisible = false
    }

    protected fun showToast(@StringRes id: Int) {
        showToast(text = getString(id = id))
    }

    protected fun showToast(text: String) {
        if (text.isNotBlank()) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    protected fun getString(@StringRes id: Int): String {
        return ContextCompat.getString(context, id)
    }

    protected fun getString(@StringRes id: Int, vararg formatArgs: Any): String {
        return ContextCompat.getString(context, id).format(*formatArgs)
    }

}