package github.leavesczy.matisse.internal

import android.util.Log

internal object MatisseLog {

    private const val TAG = "Matisse"

    fun e(throwable: Throwable, message: String? = null) {
        Log.e(TAG, message ?: throwable.message, throwable)
    }

}
