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
    val imageEngine: SampleImageEngine,
    val captureStrategy: SampleCaptureStrategy,
    val isInsertingPagingTestImages: Boolean,
    val isInsertingImageEngineTestImages: Boolean,
    val pickedMediaList: List<MediaResource>,
    val onGridColumnsChanged: (gridColumns: Int) -> Unit,
    val onMaxSelectableChanged: (maxSelectable: Int) -> Unit,
    val onReturnOnTapChanged: (returnOnTap: Boolean) -> Unit,
    val onAllowMixedMediaChanged: (allowMixedMedia: Boolean) -> Unit,
    val onImageEngineChanged: (imageEngine: SampleImageEngine) -> Unit,
    val onCaptureStrategyChanged: (captureStrategy: SampleCaptureStrategy) -> Unit,
    val onToggleTheme: () -> Unit
)

@Stable
enum class SampleCaptureStrategy {
    Smart,
    FileProvider,
    MediaStore,
    Disabled;
}

@Stable
enum class SampleImageEngine {
    Coil,
    Glide;
}
