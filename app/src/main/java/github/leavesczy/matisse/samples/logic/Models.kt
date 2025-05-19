package github.leavesczy.matisse.samples.logic

import androidx.compose.runtime.Stable
import github.leavesczy.matisse.MediaResource

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 12:01
 * @Desc:
 */
@Stable
data class MainPageViewState(
    val gridColumns: Int,
    val maxSelectable: Int,
    val fastSelect: Boolean,
    val singleMediaType: Boolean,
    val imageEngine: MediaImageEngine,
    val filterStrategy: MediaFilterStrategy,
    val captureStrategy: MediaCaptureStrategy,
    val capturePreferencesCustom: Boolean,
    val mediaList: List<MediaResource>,
    val onGridColumnsChanged: (Int) -> Unit,
    val onMaxSelectableChanged: (Int) -> Unit,
    val onFastSelectChanged: (Boolean) -> Unit,
    val onSingleMediaTypeChanged: (Boolean) -> Unit,
    val onImageEngineChanged: (MediaImageEngine) -> Unit,
    val onFilterStrategyChanged: (MediaFilterStrategy) -> Unit,
    val onCaptureStrategyChanged: (MediaCaptureStrategy) -> Unit,
    val onCapturePreferencesCustomChanged: (Boolean) -> Unit,
    val switchTheme: () -> Unit
)

@Stable
enum class MediaCaptureStrategy {
    Smart,
    FileProvider,
    MediaStore,
    Close
}

@Stable
enum class MediaImageEngine {
    Coil,
    Glide
}

@Stable
enum class MediaFilterStrategy {
    Nothing,
    IgnoreSelected,
    AttachSelected
}