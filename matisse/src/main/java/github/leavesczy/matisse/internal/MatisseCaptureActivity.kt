package github.leavesczy.matisse.internal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MatisseCapture
import github.leavesczy.matisse.MediaResource

/**
 * @Author: leavesCZY
 * @Date: 2023/4/11 16:31
 * @Desc:
 */
internal class MatisseCaptureActivity : BaseCaptureActivity() {

    private val matisseCapture by lazy(mode = LazyThreadSafetyMode.NONE) {
        intent.getParcelableExtra<MatisseCapture>(MatisseCapture::class.java.name)!!
    }

    override val captureStrategy: CaptureStrategy
        get() = matisseCapture.captureStrategy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestTakePicture()
    }

    override fun dispatchTakePictureResult(mediaResource: MediaResource) {
        val intent = Intent()
        intent.putExtra(MediaResource::class.java.name, mediaResource)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun takePictureCancelled() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

}