package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel

internal abstract class BaseMatisseViewModel(application: Application) :
    AndroidViewModel(application = application) {

    protected val context: Context
        get() = getApplication()

    protected fun showToast(@StringRes id: Int) {
        showToast(text = getString(id = id))
    }

    protected fun showToast(text: String) {
        if (text.isBlank()) {
            return
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun getString(@StringRes id: Int): String {
        return ContextCompat.getString(context, id)
    }

    /** 与 [ContextCompat.getString] 一样遵循 AppCompat 设置的应用内语言。 */
    protected fun getQuantityString(
        @PluralsRes id: Int,
        quantity: Int,
        vararg formatArgs: Any
    ): String {
        return ContextCompat.getContextForLanguage(context).resources.getQuantityString(
            id,
            quantity,
            *formatArgs
        )
    }

}
