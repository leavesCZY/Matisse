package github.leavesczy.matisse.samples

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import github.leavesczy.matisse.*

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 17:00
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
class MainActivity : AppCompatActivity() {

    companion object {

        private const val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"

    }

    private val radioGroupMaxSelectable by lazy {
        findViewById<RadioGroup>(R.id.radioGroupMaxSelectable)
    }

    private val radioMimeType by lazy {
        findViewById<RadioGroup>(R.id.radioMimeType)
    }

    private val radioGroupSupportGif by lazy {
        findViewById<RadioGroup>(R.id.radioGroupSupportGif)
    }

    private val radioGroupCaptureStrategy by lazy {
        findViewById<RadioGroup>(R.id.radioGroupCaptureStrategy)
    }

    private val rvMediaList by lazy {
        findViewById<RecyclerView>(R.id.rvMediaList)
    }

    private val btnTakePhotos by lazy {
        findViewById<View>(R.id.btnTakePhotos)
    }

    private val btnSwitchTheme by lazy {
        findViewById<View>(R.id.btnSwitchTheme)
    }

    private val btnMediaPicker by lazy {
        findViewById<View>(R.id.btnMediaPicker)
    }

    private val mediaList = mutableListOf<MediaResource>()

    private val mediaAdapter = MediaAdapter(mediaList)

    @SuppressLint("NotifyDataSetChanged")
    private val takePictureLauncher = registerForActivityResult(MatisseCaptureContract()) {
        if (it != null) {
            mediaList.clear()
            mediaList.add(it)
            mediaAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private val mediaPickerLauncher = registerForActivityResult(MatisseContract()) {
        if (it.isNotEmpty()) {
            mediaList.clear()
            mediaList.addAll(it)
            mediaAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        btnSwitchTheme.setOnClickListener {
            val defaultNightMode = AppCompatDelegate.getDefaultNightMode()
            if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
        btnTakePhotos.setOnClickListener {
            takePictureLauncher.launch(MatisseCapture(captureStrategy = getCaptureStrategy()))
        }
        btnMediaPicker.setOnClickListener {
            val matisse = Matisse(
                mimeTypes = getSupportedMimeTypes(),
                maxSelectable = getMaxSelectable(),
                captureStrategy = getCaptureStrategy(),
            )
            mediaPickerLauncher.launch(matisse)
        }
        radioGroupCaptureStrategy.setOnCheckedChangeListener { _, checkedId ->
            btnTakePhotos.isEnabled = checkedId != R.id.rbNothing
        }
        radioMimeType.setOnCheckedChangeListener { _, checkedId ->
            radioGroupSupportGif.children.forEach {
                it.isEnabled = checkedId != R.id.rbVideo
            }
        }
        rvMediaList.layoutManager = LinearLayoutManager(this)
        rvMediaList.adapter = mediaAdapter
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
        return when (radioMimeType.checkedRadioButtonId) {
            R.id.rbAllMedia -> {
                MimeType.ofAll()
            }

            R.id.rbImage -> {
                MimeType.ofImage(hasGif = radioGroupSupportGif.checkedRadioButtonId == R.id.rbSupportGif)
            }

            R.id.rbVideo -> {
                MimeType.onVideo()
            }

            else -> {
                throw RuntimeException()
            }
        }
    }

    private fun getCaptureStrategy(): CaptureStrategy {
        when (radioGroupCaptureStrategy.checkedRadioButtonId) {
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