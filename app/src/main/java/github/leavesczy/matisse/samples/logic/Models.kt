package github.leavesczy.matisse.samples.logic

import androidx.compose.runtime.Stable
import github.leavesczy.matisse.MediaResource

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
    val onGridColumnsChanged: (Int) -> Unit,
    val onMaxSelectableChanged: (Int) -> Unit,
    val onFastSelectChanged: (Boolean) -> Unit,
    val onSingleMediaTypeChanged: (Boolean) -> Unit,
    val onImageEngineChanged: (MediaImageEngine) -> Unit,
    val onCaptureStrategyChanged: (MediaCaptureStrategy) -> Unit,
    val onUseFrontCameraChanged: (Boolean) -> Unit,
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