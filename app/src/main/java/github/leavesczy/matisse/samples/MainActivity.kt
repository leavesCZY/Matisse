package github.leavesczy.matisse.samples

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import github.leavesczy.matisse.*

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 17:00
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
@SuppressLint("NotifyDataSetChanged")
class MainActivity : AppCompatActivity() {

    private val btnImagePicker by lazy {
        findViewById<View>(R.id.btnImagePicker)
    }

    private val rvImageList by lazy {
        findViewById<RecyclerView>(R.id.rvImageList)
    }

    private val radioGroupMaxSelectable by lazy {
        findViewById<RadioGroup>(R.id.radioGroupMaxSelectable)
    }

    private val radioGroupTheme by lazy {
        findViewById<RadioGroup>(R.id.radioGroupTheme)
    }

    private val radioGroupSpanCount by lazy {
        findViewById<RadioGroup>(R.id.radioGroupSpanCount)
    }

    private val radioGroupSupportGif by lazy {
        findViewById<RadioGroup>(R.id.radioGroupSupportGif)
    }

    private val radioGroupEnableCapture by lazy {
        findViewById<RadioGroup>(R.id.radioGroupEnableCapture)
    }

    private val imageList = mutableListOf<MediaResource>()

    private val imageAdapter = ImageAdapter(imageList)

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
        btnImagePicker.setOnClickListener {
            val matisse = Matisse(
                theme = getMatisseTheme(),
                supportedMimeTypes = getSupportedMimeTypes(),
                maxSelectable = getMaxSelectable(),
                spanCount = getSpanCount(),
                captureStrategy = getCaptureStrategy()
            )
            matisseContractLauncher.launch(matisse)
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

    private fun getSpanCount(): Int {
        when (radioGroupSpanCount.checkedRadioButtonId) {
            R.id.rbSpanCount3 -> {
                return 3
            }
            R.id.rbSpanCount4 -> {
                return 4
            }
            R.id.rbSpanCount5 -> {
                return 5
            }
        }
        return 3
    }

    private fun getMatisseTheme(): MatisseTheme {
        when (radioGroupTheme.checkedRadioButtonId) {
            R.id.rbThemeLight -> {
                return LightMatisseTheme
            }
            R.id.rbThemeDark -> {
                return DarkMatisseTheme
            }
            R.id.rbThemeCustom -> {
                return getCustomMatisseTheme()
            }
        }
        return LightMatisseTheme
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

    private fun getCustomMatisseTheme(): MatisseTheme {
        val darkColor = 0xFF1F1F20
        val greenColor = 0xFF009688
        return MatisseTheme(
            backgroundColor = 0xFFFFFFFF,
            previewBackgroundColor = darkColor,
            imageBackgroundColor = 0x66CCCCCC,
            alphaIfDisable = 0.6f,
            captureIconTheme = CaptureIconTheme(
                backgroundColor = 0x66CCCCCC,
                iconTint = 0xFFFFFFFF
            ),
            topAppBarTheme = TopAppBarTheme(
                defaultBucketName = "所有图片",
                backgroundColor = greenColor,
                iconColor = 0xFFFFFFFF,
                textTheme = TextTheme(
                    fontSize = 19,
                    color = 0xFFFFFFFF
                )
            ),
            bottomNavigationTheme = BottomNavigationTheme(backgroundColor = 0xFFFFFFFF),
            dropdownMenuTheme = DropdownMenuTheme(
                backgroundColor = 0xFFFFFFFF,
                textTheme = TextTheme(
                    fontSize = 14,
                    color = 0xFF000000
                )
            ),
            checkBoxTheme = CheckBoxTheme(
                countable = true,
                frameColor = greenColor,
                circleColor = greenColor,
                circleFillColor = greenColor,
                textTheme = TextTheme(
                    fontSize = 14,
                    color = 0xFFFFFFFF
                )
            ),
            previewButtonTheme = PreviewButtonTheme(
                textBuilder = { selectedSize: Int, maxSelectable: Int ->
                    "点击预览($selectedSize/$maxSelectable)"
                },
                textTheme = TextTheme(
                    fontSize = 14,
                    color = greenColor
                )
            ),
            sureButtonTheme = SureButtonTheme(
                textBuilder = { selectedSize: Int, maxSelectable: Int ->
                    "使用($selectedSize/$maxSelectable)"
                },
                textTheme = TextTheme(
                    fontSize = 14,
                    color = 0xFFFFFFFF
                ),
                backgroundColor = greenColor
            ),
            systemBarsTheme = SystemBarsTheme(
                statusBarColor = greenColor,
                statusBarDarkIcons = false,
                navigationBarColor = 0xFFFFFFFF,
                navigationBarDarkIcons = true
            )
        )
    }

}