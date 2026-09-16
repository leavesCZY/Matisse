package github.leavesczy.matisse.samples.logic

import androidx.compose.runtime.Stable
import github.leavesczy.matisse.MediaResource

fun interface GridColumnsChangedHandler {
    operator fun invoke(gridColumns: Int)
}

fun interface MaxSelectableChangedHandler {
    operator fun invoke(maxSelectable: Int)
}

fun interface FastSelectChangedHandler {
    operator fun invoke(fastSelect: Boolean)
}

fun interface SingleMediaTypeChangedHandler {
    operator fun invoke(singleMediaType: Boolean)
}

fun interface ImageEngineChangedHandler {
    operator fun invoke(imageEngine: MediaImageEngine)
}

fun interface CaptureStrategyChangedHandler {
    operator fun invoke(captureStrategy: MediaCaptureStrategy)
}

fun interface UseFrontCameraChangedHandler {
    operator fun invoke(useFrontCamera: Boolean)
}

@Stable
data class MainPageViewState(
    val darkTheme: Boolean,
    val gridColumns: Int,
    val maxSelectable: Int,
    val fastSelect: Boolean,
    val singleMediaType: Boolean,
    val imageEngine: MediaImageEngine,
    val captureStrategy: MediaCaptureStrategy,
    val useFrontCamera: Boolean,
    val isInsertingPagingTestImages: Boolean,
    val isInsertingImageEngineTestImages: Boolean,
    val pickedMediaList: List<MediaResource>,
    val onGridColumnsChanged: GridColumnsChangedHandler,
    val onMaxSelectableChanged: MaxSelectableChangedHandler,
    val onFastSelectChanged: FastSelectChangedHandler,
    val onSingleMediaTypeChanged: SingleMediaTypeChangedHandler,
    val onImageEngineChanged: ImageEngineChangedHandler,
    val onCaptureStrategyChanged: CaptureStrategyChangedHandler,
    val onUseFrontCameraChanged: UseFrontCameraChangedHandler,
    val onToggleTheme: () -> Unit
)

@Stable
enum class MediaCaptureStrategy {
    Smart,
    FileProvider,
    MediaStore,
    Disabled;
}

@Stable
enum class MediaImageEngine {
    Coil,
    Glide;
}
