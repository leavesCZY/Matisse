package github.leavesczy.matisse.samples

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
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

    private val imageList = mutableListOf<MediaResources>()

    private val imageAdapter = ImageAdapter(imageList)

    private val activityResultCallback = ActivityResultCallback<List<MediaResources>> {
        if (it.isNotEmpty()) {
            imageList.clear()
            imageList.addAll(it)
            imageAdapter.notifyDataSetChanged()
        }
    }

    private val matisseContractLauncher =
        registerForActivityResult(MatisseContract(), activityResultCallback)

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
            R.id.rbFileProvider -> {
                return FileProviderCaptureStrategy(authority = "github.leavesczy.matisse.samples.FileProvider")
            }
            R.id.rbMediaStore -> {
                return MediaStoreCaptureStrategy()
            }
            R.id.rbNothing -> {
                return NothingCaptureStrategy
            }
        }
        return NothingCaptureStrategy
    }

    private fun getCustomMatisseTheme(): MatisseTheme {
        val darkColor = Color(color = 0xFF1F1F20)
        val blueColor = Color(color = 0xFF03A9F4)
        return MatisseTheme(
            surfaceColor = Color.White,
            onPreviewSurfaceColor = darkColor,
            imageBackgroundColor = Color.LightGray.copy(alpha = 0.4f),
            alphaIfDisable = 0.5f,
            captureIconTheme = CaptureIconTheme(
                backgroundColor = Color.LightGray.copy(alpha = 0.4f),
                icon = Icons.Filled.PhotoCamera,
                tint = Color.White,
            ),
            topAppBarTheme = TopAppBarTheme(
                defaultBucketName = "全部",
                backgroundColor = blueColor,
                contentColor = Color.White,
                fontSize = 18.sp,
            ),
            bottomNavigationTheme = BottomNavigationTheme(
                backgroundColor = Color.White,
            ),
            dropdownMenuTheme = DropdownMenuTheme(
                backgroundColor = Color.White,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Black,
                ),
            ),
            checkBoxTheme = CheckBoxTheme(
                countable = true,
                frameColor = Color.Transparent,
                circleColor = Color.White,
                circleFillColor = blueColor,
                fontSize = 14.sp,
                textColor = Color.White,
            ),
            previewButtonTheme = PreviewButtonTheme(
                textBuilder = { selectedSize: Int, maxSelectable: Int ->
                    "点击预览($selectedSize/$maxSelectable)"
                },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = blueColor,
                ),
            ),
            sureButtonTheme = SureButtonTheme(
                textBuilder = { selectedSize: Int, _: Int ->
                    "使用($selectedSize)"
                },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White,
                ),
                backgroundColor = blueColor,
            ),
            systemBarsTheme = SystemBarsTheme(
                statusBarColor = blueColor,
                statusBarDarkIcons = false,
                navigationBarColor = Color.White,
                navigationBarDarkIcons = true
            ),
        )
    }

}