package github.leavesczy.matisse.internal

import android.app.Activity
import android.os.Bundle
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MatisseCaptureContract
import github.leavesczy.matisse.MediaResource

/**
 * @Author: CZY
 * @Date: 2023/4/11 16:31
 * @Desc:
 */
internal class MatisseCaptureActivity : BaseMatisseCaptureActivity() {

    private val matisseCapture by lazy {
        MatisseCaptureContract.getRequest(intent = intent)
    }

    override val captureStrategy: CaptureStrategy
        get() = matisseCapture.captureStrategy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestTakePicture()
    }

    override fun dispatchTakePictureResult(mediaResource: MediaResource) {
        val data = MatisseCaptureContract.buildResult(mediaResource = mediaResource)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onCancelTakePicture() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

}