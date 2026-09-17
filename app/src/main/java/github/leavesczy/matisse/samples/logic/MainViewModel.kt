package github.leavesczy.matisse.samples.logic

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application = application) {

    companion object {

        private const val TEST_ALBUM = "Matisse"

        private const val PAGING_TEST_IMAGE_COUNT = 200

        private const val PAGING_TEST_IMAGE_SIZE = 512

        private val IMAGE_ENGINE_TEST_SPECS = listOf(
            ImageTestSpec(name = "small_square", width = 320, height = 320),
            ImageTestSpec(name = "landscape", width = 1920, height = 1080),
            ImageTestSpec(name = "portrait", width = 1080, height = 1920),
            ImageTestSpec(name = "large_square", width = 2048, height = 2048),
            ImageTestSpec(name = "long_portrait", width = 1080, height = 5000),
            ImageTestSpec(name = "ultra_long", width = 720, height = 8000),
            ImageTestSpec(name = "wide_landscape", width = 5000, height = 1080),
            ImageTestSpec(name = "ultra_wide", width = 8000, height = 720)
        )

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
            isInsertingPagingTestImages = false,
            isInsertingImageEngineTestImages = false,
            pickedMediaList = emptyList(),
            onGridColumnsChanged = ::onGridColumnsChanged,
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onFastSelectChanged = ::onFastSelectChanged,
            onSingleMediaTypeChanged = ::onSingleMediaTypeChanged,
            onImageEngineChanged = ::onImageEngineChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            onToggleTheme = ::onToggleTheme
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

    fun insertPagingTestImages() {
        if (pageViewState.isInsertingPagingTestImages ||
            pageViewState.isInsertingImageEngineTestImages
        ) {
            return
        }
        pageViewState = pageViewState.copy(isInsertingPagingTestImages = true)
        viewModelScope.launch {
            try {
                val insertedCount = withContext(context = Dispatchers.IO) {
                    insertPagingTestImagesInternal()
                }
                Toast.makeText(
                    getApplication(),
                    "已插入 $insertedCount / $PAGING_TEST_IMAGE_COUNT 张测试图片",
                    Toast.LENGTH_LONG
                ).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                exception.printStackTrace()
                Toast.makeText(getApplication(), "插入测试图片失败", Toast.LENGTH_LONG).show()
            } finally {
                pageViewState = pageViewState.copy(isInsertingPagingTestImages = false)
            }
        }
    }

    private suspend fun insertPagingTestImagesInternal(): Int {
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
                currentCoroutineContext().ensureActive()
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
                            Environment.DIRECTORY_PICTURES + "/" + TEST_ALBUM
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val imageUri =
                    contentResolver.insert(imageCollection, contentValues) ?: continue
                var shouldDeleteImage = true
                try {
                    val written = contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    } ?: false
                    val published = if (written && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val pendingValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        contentResolver.update(imageUri, pendingValues, null, null) > 0
                    } else {
                        written
                    }
                    if (published) {
                        insertedCount += 1
                        shouldDeleteImage = false
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    exception.printStackTrace()
                } finally {
                    if (shouldDeleteImage) {
                        contentResolver.delete(imageUri, null, null)
                    }
                }
            }
        } finally {
            bitmap.recycle()
        }
        return insertedCount
    }

    fun insertImageEngineTestImages() {
        if (pageViewState.isInsertingImageEngineTestImages ||
            pageViewState.isInsertingPagingTestImages
        ) {
            return
        }
        pageViewState = pageViewState.copy(isInsertingImageEngineTestImages = true)
        viewModelScope.launch {
            try {
                val insertedCount = withContext(context = Dispatchers.IO) {
                    insertImageEngineTestImagesInternal()
                }
                Toast.makeText(
                    getApplication(),
                    "已插入 $insertedCount / ${IMAGE_ENGINE_TEST_SPECS.size} 张多尺寸测试图片",
                    Toast.LENGTH_LONG
                ).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                exception.printStackTrace()
                Toast.makeText(getApplication(), "插入多尺寸图片失败", Toast.LENGTH_LONG).show()
            } finally {
                pageViewState = pageViewState.copy(isInsertingImageEngineTestImages = false)
            }
        }
    }

    private suspend fun insertImageEngineTestImagesInternal(): Int {
        val application = getApplication<Application>()
        val contentResolver = application.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val timestamp = System.currentTimeMillis()
        var insertedCount = 0
        IMAGE_ENGINE_TEST_SPECS.forEachIndexed { index, spec ->
            currentCoroutineContext().ensureActive()
            var bitmap: Bitmap? = null
            var imageUri: android.net.Uri? = null
            try {
                bitmap = createBitmap(width = spec.width, height = spec.height)
                drawImageEngineTestImage(bitmap = bitmap, spec = spec)
                val displayName =
                    "matisse_image_engine_${timestamp}_${index}_${spec.width}x${spec.height}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATE_ADDED, timestamp / 1000L + index)
                    put(MediaStore.Images.Media.DATE_MODIFIED, timestamp / 1000L + index)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/" + TEST_ALBUM
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                imageUri = contentResolver.insert(imageCollection, contentValues)
                if (imageUri != null) {
                    val written = contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, outputStream)
                    } ?: false
                    val published = if (written && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val pendingValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        contentResolver.update(imageUri, pendingValues, null, null) > 0
                    } else {
                        written
                    }
                    if (published) {
                        insertedCount += 1
                        imageUri = null
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                exception.printStackTrace()
            } finally {
                if (imageUri != null) {
                    contentResolver.delete(imageUri, null, null)
                }
                bitmap?.recycle()
            }
        }
        return insertedCount
    }

    private fun drawImageEngineTestImage(bitmap: Bitmap, spec: ImageTestSpec) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sectionCount = 10
        val sectionHeight = spec.height.toFloat() / sectionCount
        repeat(times = sectionCount) { section ->
            paint.color = if (section % 2 == 0) {
                Color.rgb(39, 91, 173)
            } else {
                Color.rgb(26, 145, 126)
            }
            val top = section * sectionHeight
            canvas.drawRect(0f, top, spec.width.toFloat(), top + sectionHeight, paint)
        }
        paint.color = Color.argb(150, 255, 255, 255)
        paint.strokeWidth =
            (minOf(a = spec.width, b = spec.height) * 0.008f).coerceAtLeast(minimumValue = 2f)
        canvas.drawLine(
            spec.width / 2f,
            0f,
            spec.width / 2f,
            spec.height.toFloat(),
            paint
        )
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = (minOf(a = spec.width, b = spec.height) * 0.07f).coerceIn(
            minimumValue = 26f,
            maximumValue = 96f
        )
        repeat(times = sectionCount) { section ->
            val progress = section * 100 / (sectionCount - 1)
            val centerY = section * sectionHeight + sectionHeight / 2f
            canvas.drawText(
                "${spec.name}  ${spec.width}×${spec.height}  $progress%",
                spec.width / 2f,
                centerY - (paint.ascent() + paint.descent()) / 2f,
                paint
            )
        }
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
        return when (currentPageViewState.captureStrategy) {
            MediaCaptureStrategy.Smart -> {
                SmartCaptureStrategy(
                    fileProviderCaptureStrategy = FileProviderCaptureStrategy(
                        authority = fileProviderAuthority
                    )
                )
            }

            MediaCaptureStrategy.FileProvider -> {
                FileProviderCaptureStrategy(
                    authority = fileProviderAuthority
                )
            }

            MediaCaptureStrategy.MediaStore -> {
                MediaStoreCaptureStrategy()
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
            captureStrategy = if (mediaType.includesImage) {
                resolveCaptureStrategy()
            } else {
                null
            }
        )
    }

    fun buildMatisseCapture(): MatisseCapture? {
        val captureStrategy = resolveCaptureStrategy() ?: return null
        return MatisseCapture(captureStrategy = captureStrategy)
    }

    fun onCaptureResult(mediaResource: MediaResource?) {
        if (mediaResource != null) {
            pageViewState = pageViewState.copy(pickedMediaList = listOf(element = mediaResource))
        }
    }

    fun onMediaPickerResult(result: List<MediaResource>?) {
        if (!result.isNullOrEmpty()) {
            pageViewState = pageViewState.copy(pickedMediaList = result)
        }
    }

    private data class ImageTestSpec(
        val name: String,
        val width: Int,
        val height: Int
    )

}
