package github.leavesczy.matisse.samples.logic

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.CoilImageEngine
import github.leavesczy.matisse.FileProviderCaptureStrategy
import github.leavesczy.matisse.GlideImageEngine
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MatisseCapture
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaStoreCaptureStrategy
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.SmartCaptureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {

        private const val PAGING_TEST_ALBUM = "MatissePagingTest"

        private const val PAGING_TEST_IMAGE_COUNT = 200

        private const val PAGING_TEST_IMAGE_SIZE = 512

    }

    var pageViewState by mutableStateOf(
        value = MainPageViewState(
            darkTheme = false,
            gridColumns = 4,
            maxSelectable = 3,
            fastSelect = false,
            singleMediaType = false,
            imageEngine = MediaImageEngine.Coil,
            captureStrategy = MediaCaptureStrategy.Smart,
            useFrontCamera = false,
            isInsertingPagingTestImages = false,
            pickedMediaList = emptyList(),
            onGridColumnsChanged = ::onGridColumnsChanged,
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onFastSelectChanged = ::onFastSelectChanged,
            onSingleMediaTypeChanged = ::onSingleMediaTypeChanged,
            onImageEngineChanged = ::onImageEngineChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            onUseFrontCameraChanged = ::onUseFrontCameraChanged,
            onToggleTheme = ::onToggleTheme,
            onInsertPagingTestImages = ::insertPagingTestImages
        )
    )
        private set

    private fun onGridColumnsChanged(gridColumns: Int) {
        pageViewState = pageViewState.copy(gridColumns = gridColumns)
    }

    private fun onMaxSelectableChanged(maxSelectable: Int) {
        val currentPageViewState = pageViewState
        val fastSelect = if (currentPageViewState.fastSelect) {
            maxSelectable == 1
        } else {
            false
        }
        pageViewState = currentPageViewState.copy(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect
        )
    }

    private fun onFastSelectChanged(fastSelect: Boolean) {
        val currentPageViewState = pageViewState
        val maxSelectable = if (fastSelect) {
            1
        } else {
            currentPageViewState.maxSelectable
        }
        pageViewState = currentPageViewState.copy(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect
        )
    }

    private fun onSingleMediaTypeChanged(singleMediaType: Boolean) {
        pageViewState = pageViewState.copy(singleMediaType = singleMediaType)
    }

    private fun onImageEngineChanged(imageEngine: MediaImageEngine) {
        pageViewState = pageViewState.copy(imageEngine = imageEngine)
    }

    private fun onCaptureStrategyChanged(captureStrategy: MediaCaptureStrategy) {
        pageViewState = pageViewState.copy(captureStrategy = captureStrategy)
    }

    private fun onUseFrontCameraChanged(useFrontCamera: Boolean) {
        pageViewState = pageViewState.copy(useFrontCamera = useFrontCamera)
    }

    private fun onToggleTheme() {
        val currentPageViewState = pageViewState
        val darkTheme = !currentPageViewState.darkTheme
        pageViewState = currentPageViewState.copy(darkTheme = darkTheme)
        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun insertPagingTestImages() {
        if (pageViewState.isInsertingPagingTestImages) {
            return
        }
        pageViewState = pageViewState.copy(isInsertingPagingTestImages = true)
        viewModelScope.launch {
            val insertedCount = withContext(context = Dispatchers.IO) {
                insertPagingTestImagesInternal()
            }
            pageViewState = pageViewState.copy(isInsertingPagingTestImages = false)
            Toast.makeText(
                getApplication(),
                "已插入 $insertedCount / $PAGING_TEST_IMAGE_COUNT 张测试图片",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun insertPagingTestImagesInternal(): Int {
        val application = getApplication<Application>()
        val contentResolver = application.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val nowSeconds = System.currentTimeMillis() / 1000L
        val bitmap = createBitmap(
            width = PAGING_TEST_IMAGE_SIZE,
            height = PAGING_TEST_IMAGE_SIZE
        )
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val random = Random(seed = System.currentTimeMillis())
        var insertedCount = 0
        try {
            for (index in 0 until PAGING_TEST_IMAGE_COUNT) {
                drawRandomTestImage(
                    canvas = canvas,
                    paint = paint,
                    random = random,
                    index = index
                )
                val displayName = "matisse_paging_test_${System.currentTimeMillis()}_$index.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATE_ADDED, nowSeconds - index)
                    put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds - index)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/" + PAGING_TEST_ALBUM
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val imageUri = contentResolver.insert(imageCollection, contentValues) ?: continue
                try {
                    val written = contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    } ?: false
                    if (!written) {
                        contentResolver.delete(imageUri, null, null)
                        continue
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val pendingValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        contentResolver.update(imageUri, pendingValues, null, null)
                    }
                    insertedCount += 1
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    contentResolver.delete(imageUri, null, null)
                }
            }
        } finally {
            bitmap.recycle()
        }
        return insertedCount
    }

    private fun drawRandomTestImage(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        index: Int
    ) {
        canvas.drawColor(
            Color.rgb(
                random.nextInt(from = 40, until = 220),
                random.nextInt(from = 40, until = 220),
                random.nextInt(from = 40, until = 220)
            )
        )
        repeat(times = 8) {
            paint.color = Color.argb(
                random.nextInt(from = 120, until = 220),
                random.nextInt(from = 0, until = 256),
                random.nextInt(from = 0, until = 256),
                random.nextInt(from = 0, until = 256)
            )
            val left = random.nextInt(from = 0, until = PAGING_TEST_IMAGE_SIZE).toFloat()
            val top = random.nextInt(from = 0, until = PAGING_TEST_IMAGE_SIZE).toFloat()
            val size = random.nextInt(from = 48, until = 220).toFloat()
            if (random.nextBoolean()) {
                canvas.drawCircle(left, top, size / 2f, paint)
            } else {
                canvas.drawRoundRect(
                    left,
                    top,
                    left + size,
                    top + size,
                    24f,
                    24f,
                    paint
                )
            }
        }
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 72f
        canvas.drawText(
            "#${index + 1}",
            PAGING_TEST_IMAGE_SIZE / 2f,
            PAGING_TEST_IMAGE_SIZE / 2f,
            paint
        )
    }

    private fun resolveCaptureStrategy(): CaptureStrategy? {
        val currentPageViewState = pageViewState
        val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"
        val captureExtra = if (currentPageViewState.useFrontCamera) {
            val bundle = Bundle()
            bundle.putBoolean("android.intent.extra.USE_FRONT_CAMERA", true)
            bundle.putInt("android.intent.extras.CAMERA_FACING", 1)
            bundle
        } else {
            Bundle.EMPTY
        }
        return when (currentPageViewState.captureStrategy) {
            MediaCaptureStrategy.Smart -> {
                SmartCaptureStrategy(
                    fileProviderCaptureStrategy = FileProviderCaptureStrategy(
                        authority = fileProviderAuthority,
                        extra = captureExtra
                    )
                )
            }

            MediaCaptureStrategy.FileProvider -> {
                FileProviderCaptureStrategy(
                    authority = fileProviderAuthority,
                    extra = captureExtra
                )
            }

            MediaCaptureStrategy.MediaStore -> {
                MediaStoreCaptureStrategy(extra = captureExtra)
            }

            MediaCaptureStrategy.Disabled -> {
                null
            }
        }
    }

    fun buildMatisse(mediaType: MediaType): Matisse {
        val currentPageViewState = pageViewState
        val imageEngine = when (currentPageViewState.imageEngine) {
            MediaImageEngine.Coil -> {
                CoilImageEngine()
            }

            MediaImageEngine.Glide -> {
                GlideImageEngine()
            }
        }
        return Matisse(
            gridColumns = currentPageViewState.gridColumns,
            maxSelectable = currentPageViewState.maxSelectable,
            fastSelect = currentPageViewState.fastSelect,
            mediaType = mediaType,
            imageEngine = imageEngine,
            singleMediaType = currentPageViewState.singleMediaType,
            captureStrategy = resolveCaptureStrategy()
        )
    }

    fun buildMatisseCapture(): MatisseCapture? {
        val captureStrategy = resolveCaptureStrategy() ?: return null
        return MatisseCapture(captureStrategy = captureStrategy)
    }

    fun onTakePictureResult(mediaResource: MediaResource?) {
        if (mediaResource != null) {
            pageViewState = pageViewState.copy(pickedMediaList = listOf(element = mediaResource))
        }
    }

    fun onMediaPickerResult(result: List<MediaResource>?) {
        if (!result.isNullOrEmpty()) {
            pageViewState = pageViewState.copy(pickedMediaList = result)
        }
    }

}
