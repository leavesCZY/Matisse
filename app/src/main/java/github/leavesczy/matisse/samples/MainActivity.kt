package github.leavesczy.matisse.samples

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import github.leavesczy.matisse.*

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 17:00
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
class MainActivity : AppCompatActivity() {

    private val radioGroupMaxSelectable by lazy {
        findViewById<RadioGroup>(R.id.radioGroupMaxSelectable)
    }

    private val radioGroupSupportGif by lazy {
        findViewById<RadioGroup>(R.id.radioGroupSupportGif)
    }

    private val radioGroupEnableCapture by lazy {
        findViewById<RadioGroup>(R.id.radioGroupEnableCapture)
    }

    private val rvImageList by lazy {
        findViewById<RecyclerView>(R.id.rvImageList)
    }

    private val imageList = mutableListOf<MediaResource>()

    private val imageAdapter = ImageAdapter(imageList)

    @SuppressLint("NotifyDataSetChanged")
    private val activityResultCallback = ActivityResultCallback<List<MediaResource>> {
        if (it.isNotEmpty()) {
            imageList.clear()
            imageList.addAll(it)
            imageAdapter.notifyDataSetChanged()
        }
    }

    private val matisseContractLauncher =
        registerForActivityResult(MatisseContract(), activityResultCallback)

    private val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        findViewById<View>(R.id.btnImagePicker).setOnClickListener {
            val matisse = Matisse(
                supportedMimeTypes = getSupportedMimeTypes(),
                maxSelectable = getMaxSelectable(),
                captureStrategy = getCaptureStrategy(),
            )
            matisseContractLauncher.launch(matisse)
        }
        findViewById<Button>(R.id.btnSwitchTheme).setOnClickListener {
            val defaultNightMode = AppCompatDelegate.getDefaultNightMode()
            if (defaultNightMode != AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        rvImageList.layoutManager = LinearLayoutManager(this)
        rvImageList.adapter = imageAdapter
    }

    private fun getMaxSelectable(): Int {
        when (radioGroupMaxSelectable.checkedRadioButtonId) {
            R.id.rbMaxSelectable1 -> {
                return 1
            }
            R.id.rbMaxSelectable2 -> {
                return 2
            }
            R.id.rbMaxSelectable3 -> {
                return 3
            }
        }
        return 1
    }

    private fun getSupportedMimeTypes(): List<MimeType> {
        return Matisse.ofImage(hasGif = radioGroupSupportGif.checkedRadioButtonId == R.id.rbSupportGif)
    }

    private fun getCaptureStrategy(): CaptureStrategy {
        when (radioGroupEnableCapture.checkedRadioButtonId) {
            R.id.rbNothing -> {
                return NothingCaptureStrategy
            }
            R.id.rbFileProvider -> {
                return FileProviderCaptureStrategy(authority = fileProviderAuthority)
            }
            R.id.rbMediaStore -> {
                return MediaStoreCaptureStrategy()
            }
            R.id.rbSmart -> {
                return SmartCaptureStrategy(authority = fileProviderAuthority)
            }
        }
        return NothingCaptureStrategy
    }

}