package github.leavesczy.matisse.samples.logic

import androidx.compose.runtime.Stable
import github.leavesczy.matisse.MediaResource

@Stable
data class MainPageViewState(
    val darkTheme: Boolean,
    val gridColumns: Int,
    val maxSelectable: Int,
    val returnOnTap: Boolean,
    val allowMixedMedia: Boolean,
    val imageEngine: MediaImageEngine,
    val captureStrategy: MediaCaptureStrategy,
    val isInsertingPagingTestImages: Boolean,
    val isInsertingImageEngineTestImages: Boolean,
    val pickedMediaList: List<MediaResource>,
    val onGridColumnsChanged: (gridColumns: Int) -> Unit,
    val onMaxSelectableChanged: (maxSelectable: Int) -> Unit,
    val onReturnOnTapChanged: (returnOnTap: Boolean) -> Unit,
    val onAllowMixedMediaChanged: (allowMixedMedia: Boolean) -> Unit,
    val onImageEngineChanged: (imageEngine: MediaImageEngine) -> Unit,
    val onCaptureStrategyChanged: (captureStrategy: MediaCaptureStrategy) -> Unit,
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
