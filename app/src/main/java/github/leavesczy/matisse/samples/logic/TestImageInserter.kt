package github.leavesczy.matisse.samples.logic

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSkew
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.min
import kotlin.random.Random

internal class TestImageInserter(private val contentResolver: ContentResolver) {

    companion object {

        const val PAGING_TEST_IMAGE_COUNT = 200

        val IMAGE_ENGINE_TEST_SPEC_COUNT: Int
            get() = IMAGE_ENGINE_TEST_SPECS.size

        private const val TEST_ALBUM = "Matisse"

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

        private val TEST_IMAGE_PALETTES = listOf(
            intArrayOf(
                Color.rgb(255, 183, 178),
                Color.rgb(255, 218, 193),
                Color.rgb(226, 240, 203)
            ),
            intArrayOf(Color.rgb(72, 149, 239), Color.rgb(114, 9, 183), Color.rgb(247, 37, 133)),
            intArrayOf(Color.rgb(15, 76, 92), Color.rgb(0, 180, 216), Color.rgb(144, 224, 239)),
            intArrayOf(Color.rgb(255, 107, 107), Color.rgb(255, 159, 67), Color.rgb(254, 202, 87)),
            intArrayOf(Color.rgb(46, 64, 87), Color.rgb(99, 136, 137), Color.rgb(193, 218, 215)),
            intArrayOf(Color.rgb(67, 56, 202), Color.rgb(139, 92, 246), Color.rgb(244, 114, 182)),
            intArrayOf(Color.rgb(6, 95, 70), Color.rgb(16, 185, 129), Color.rgb(167, 243, 208)),
            intArrayOf(Color.rgb(30, 41, 59), Color.rgb(71, 85, 105), Color.rgb(226, 232, 240)),
            intArrayOf(Color.rgb(190, 24, 93), Color.rgb(244, 63, 94), Color.rgb(253, 164, 175)),
            intArrayOf(Color.rgb(8, 47, 73), Color.rgb(12, 74, 110), Color.rgb(56, 189, 248))
        )

    }

    private data class ImageTestSpec(
        val name: String,
        val width: Int,
        val height: Int
    )

    private enum class TestImageStyle(val label: String) {
        SoftGradient(label = "Soft Gradient"),
        MeshBlobs(label = "Mesh Blobs"),
        DiagonalBands(label = "Diagonal Bands"),
        RadialGlow(label = "Radial Glow"),
        RoundedMosaic(label = "Rounded Mosaic"),
        HorizonLayers(label = "Horizon Layers"),
        OrbitRings(label = "Orbit Rings"),
        DotField(label = "Dot Field")
    }

    suspend fun insertPagingTestImages(): Int {
        val imageCollection = imageCollectionUri()
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

    suspend fun insertImageEngineTestImages(): Int {
        val imageCollection = imageCollectionUri()
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

    private fun imageCollectionUri() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    private fun drawImageEngineTestImage(bitmap: Bitmap, spec: ImageTestSpec) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val random = Random(seed = System.currentTimeMillis() xor spec.name.hashCode().toLong())
        val style = TestImageStyle.entries[random.nextInt(TestImageStyle.entries.size)]
        val palette = TEST_IMAGE_PALETTES[random.nextInt(TEST_IMAGE_PALETTES.size)]
        drawStyledTestImage(
            canvas = canvas,
            paint = paint,
            random = random,
            width = spec.width,
            height = spec.height,
            style = style,
            palette = palette,
            title = "#${spec.name}",
            subtitle = "${spec.width}×${spec.height}"
        )
        drawImageEngineMeasurementOverlay(
            canvas = canvas,
            paint = paint,
            width = spec.width,
            height = spec.height
        )
    }

    private fun drawRandomTestImage(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        index: Int
    ) {
        val style = TestImageStyle.entries[random.nextInt(TestImageStyle.entries.size)]
        val palette = TEST_IMAGE_PALETTES[random.nextInt(TEST_IMAGE_PALETTES.size)]
        drawStyledTestImage(
            canvas = canvas,
            paint = paint,
            random = random,
            width = PAGING_TEST_IMAGE_SIZE,
            height = PAGING_TEST_IMAGE_SIZE,
            style = style,
            palette = palette,
            title = "#${index + 1}",
            subtitle = style.label
        )
    }

    private fun drawStyledTestImage(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        width: Int,
        height: Int,
        style: TestImageStyle,
        palette: IntArray,
        title: String,
        subtitle: String
    ) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        when (style) {
            TestImageStyle.SoftGradient -> {
                drawSoftGradientStyle(
                    canvas = canvas,
                    paint = paint,
                    random = random,
                    width = width,
                    height = height,
                    palette = palette
                )
            }

            TestImageStyle.MeshBlobs -> {
                drawMeshBlobsStyle(
                    canvas = canvas,
                    paint = paint,
                    random = random,
                    width = width,
                    height = height,
                    palette = palette
                )
            }

            TestImageStyle.DiagonalBands -> {
                drawDiagonalBandsStyle(
                    canvas = canvas,
                    paint = paint,
                    random = random,
                    width = width,
                    height = height,
                    palette = palette
                )
            }

            TestImageStyle.RadialGlow -> {
                drawRadialGlowStyle(
                    canvas = canvas,
                    paint = paint,
                    random = random,
                    width = width,
                    height = height,
                    palette = palette
                )
            }

            TestImageStyle.RoundedMosaic -> {
                drawRoundedMosaicStyle(
                    canvas = canvas,
                    paint = paint,
                    random = random,
                    width = width,
                    height = height,
                    palette = palette
                )
            }

            TestImageStyle.HorizonLayers -> {
                drawHorizonLayersStyle(
                    canvas = canvas,
                    paint = paint,
                    width = width,
                    height = height,
                    palette = palette
                )
            }

            TestImageStyle.OrbitRings -> {
                drawOrbitRingsStyle(
                    canvas = canvas,
                    paint = paint,
                    random = random,
                    width = width,
                    height = height,
                    palette = palette
                )
            }

            TestImageStyle.DotField -> {
                drawDotFieldStyle(
                    canvas = canvas,
                    paint = paint,
                    random = random,
                    width = width,
                    height = height,
                    palette = palette
                )
            }
        }
        drawTestImageCaption(
            canvas = canvas,
            paint = paint,
            width = width,
            height = height,
            title = title,
            subtitle = subtitle
        )
    }

    private fun drawSoftGradientStyle(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        width: Int,
        height: Int,
        palette: IntArray
    ) {
        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            palette,
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        repeat(times = 4) {
            paint.color = Color.argb(
                random.nextInt(from = 40, until = 90),
                Color.red(palette[it % palette.size]),
                Color.green(palette[it % palette.size]),
                Color.blue(palette[it % palette.size])
            )
            val radius = min(a = width, b = height) * random.nextFloat(from = 0.18f, until = 0.42f)
            canvas.drawCircle(
                random.nextFloat(from = 0f, until = width.toFloat()),
                random.nextFloat(from = 0f, until = height.toFloat()),
                radius,
                paint
            )
        }
    }

    private fun drawMeshBlobsStyle(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        width: Int,
        height: Int,
        palette: IntArray
    ) {
        canvas.drawColor(palette.first())
        repeat(times = 7) { index ->
            val color = palette[(index + 1) % palette.size]
            paint.color = Color.argb(
                random.nextInt(from = 90, until = 170),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            val size = min(a = width, b = height) * random.nextFloat(from = 0.22f, until = 0.55f)
            val left = random.nextFloat(from = -size / 3f, until = width - size / 2f)
            val top = random.nextFloat(from = -size / 3f, until = height - size / 2f)
            if (random.nextBoolean()) {
                canvas.drawCircle(left + size / 2f, top + size / 2f, size / 2f, paint)
            } else {
                canvas.drawRoundRect(
                    left,
                    top,
                    left + size,
                    top + size * random.nextFloat(from = 0.7f, until = 1.2f),
                    size * 0.28f,
                    size * 0.28f,
                    paint
                )
            }
        }
    }

    private fun drawDiagonalBandsStyle(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        width: Int,
        height: Int,
        palette: IntArray
    ) {
        canvas.drawColor(palette.first())
        val bandCount = random.nextInt(from = 5, until = 9)
        val step = (width + height).toFloat() / bandCount
        repeat(times = bandCount) { index ->
            paint.color = palette[index % palette.size]
            val offset = index * step - height
            canvas.withSkew(-0.35f, 0f) {
                drawRect(
                    offset,
                    -height.toFloat(),
                    offset + step * 0.72f,
                    height * 2f,
                    paint
                )
            }
        }
    }

    private fun drawRadialGlowStyle(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        width: Int,
        height: Int,
        palette: IntArray
    ) {
        canvas.drawColor(palette.last())
        val centerX = width * random.nextFloat(from = 0.28f, until = 0.72f)
        val centerY = height * random.nextFloat(from = 0.28f, until = 0.72f)
        val radius = min(a = width, b = height) * random.nextFloat(from = 0.45f, until = 0.85f)
        paint.shader = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(palette[1 % palette.size], palette[0], palette.last()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        paint.color = Color.argb(55, 255, 255, 255)
        canvas.drawCircle(centerX, centerY, radius * 0.22f, paint)
    }

    private fun drawRoundedMosaicStyle(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        width: Int,
        height: Int,
        palette: IntArray
    ) {
        canvas.drawColor(palette.first())
        val columns = random.nextInt(from = 3, until = 6)
        val rows = random.nextInt(from = 3, until = 6)
        val gap = min(a = width, b = height) * 0.02f
        val cellWidth = (width - gap * (columns + 1)) / columns
        val cellHeight = (height - gap * (rows + 1)) / rows
        val radius = min(a = cellWidth, b = cellHeight) * 0.22f
        repeat(times = rows) { row ->
            repeat(times = columns) { column ->
                paint.color = palette[(row * columns + column) % palette.size]
                val left = gap + column * (cellWidth + gap)
                val top = gap + row * (cellHeight + gap)
                canvas.drawRoundRect(
                    left,
                    top,
                    left + cellWidth,
                    top + cellHeight,
                    radius,
                    radius,
                    paint
                )
            }
        }
    }

    private fun drawHorizonLayersStyle(
        canvas: Canvas,
        paint: Paint,
        width: Int,
        height: Int,
        palette: IntArray
    ) {
        val layerCount = palette.size.coerceAtLeast(minimumValue = 3)
        val layerHeight = height.toFloat() / layerCount
        repeat(times = layerCount) { index ->
            paint.shader = LinearGradient(
                0f,
                index * layerHeight,
                0f,
                (index + 1) * layerHeight,
                palette[index % palette.size],
                palette[(index + 1) % palette.size],
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(
                0f,
                index * layerHeight,
                width.toFloat(),
                (index + 1) * layerHeight + 1f,
                paint
            )
        }
        paint.shader = null
    }

    private fun drawOrbitRingsStyle(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        width: Int,
        height: Int,
        palette: IntArray
    ) {
        paint.shader = LinearGradient(
            0f,
            height.toFloat(),
            width.toFloat(),
            0f,
            palette.first(),
            palette.last(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(a = width, b = height) * 0.48f
        val ringCount = random.nextInt(from = 4, until = 7)
        repeat(times = ringCount) { index ->
            val color = palette[index % palette.size]
            paint.color = Color.argb(
                random.nextInt(from = 120, until = 210),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            paint.strokeWidth = maxRadius * random.nextFloat(from = 0.03f, until = 0.08f)
            canvas.drawCircle(
                centerX,
                centerY,
                maxRadius * (index + 1).toFloat() / (ringCount + 1),
                paint
            )
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(180, 255, 255, 255)
        canvas.drawCircle(centerX, centerY, maxRadius * 0.08f, paint)
    }

    private fun drawDotFieldStyle(
        canvas: Canvas,
        paint: Paint,
        random: Random,
        width: Int,
        height: Int,
        palette: IntArray
    ) {
        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            palette.first(),
            palette[1 % palette.size],
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        val minSide = min(a = width, b = height).toFloat()
        repeat(times = random.nextInt(from = 28, until = 48)) {
            val color = palette[random.nextInt(palette.size)]
            paint.color = Color.argb(
                random.nextInt(from = 70, until = 160),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            canvas.drawCircle(
                random.nextFloat(from = 0f, until = width.toFloat()),
                random.nextFloat(from = 0f, until = height.toFloat()),
                minSide * random.nextFloat(from = 0.01f, until = 0.05f),
                paint
            )
        }
    }

    private fun drawTestImageCaption(
        canvas: Canvas,
        paint: Paint,
        width: Int,
        height: Int,
        title: String,
        subtitle: String
    ) {
        val minSide = min(a = width, b = height).toFloat()
        val cardWidth = width * 0.72f
        val cardHeight = minSide * 0.28f
        val left = (width - cardWidth) / 2f
        val top = (height - cardHeight) / 2f
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(140, 20, 24, 32)
        canvas.drawRoundRect(
            left,
            top,
            left + cardWidth,
            top + cardHeight,
            cardHeight * 0.22f,
            cardHeight * 0.22f,
            paint
        )
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = (minSide * 0.09f).coerceIn(minimumValue = 28f, maximumValue = 92f)
        val titleY = top + cardHeight * 0.42f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(title, width / 2f, titleY, paint)
        paint.color = Color.argb(210, 230, 235, 245)
        paint.textSize = (minSide * 0.045f).coerceIn(minimumValue = 18f, maximumValue = 48f)
        val subtitleY = top + cardHeight * 0.72f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(subtitle, width / 2f, subtitleY, paint)
    }

    private fun drawImageEngineMeasurementOverlay(
        canvas: Canvas,
        paint: Paint,
        width: Int,
        height: Int
    ) {
        val sectionCount = 10
        val sectionHeight = height.toFloat() / sectionCount
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(120, 255, 255, 255)
        paint.strokeWidth = (min(a = width, b = height) * 0.004f).coerceAtLeast(minimumValue = 2f)
        canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = (min(a = width, b = height) * 0.035f).coerceIn(
            minimumValue = 18f,
            maximumValue = 48f
        )
        paint.color = Color.argb(200, 255, 255, 255)
        repeat(times = sectionCount) { section ->
            val progress = section * 100 / (sectionCount - 1)
            val centerY = section * sectionHeight + sectionHeight / 2f
            canvas.drawText(
                "$progress%",
                width * 0.86f,
                centerY - (paint.ascent() + paint.descent()) / 2f,
                paint
            )
        }
    }

    private fun Random.nextFloat(from: Float, until: Float): Float {
        return from + nextFloat() * (until - from)
    }

}
