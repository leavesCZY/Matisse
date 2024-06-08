package github.leavesczy.matisse.samples.logic

import github.leavesczy.matisse.MediaResource

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 12:01
 * @Desc:
 */
enum class MediaCaptureStrategy {
    Smart,
    FileProvider,
    MediaStore,
    Close
}

enum class MediaImageEngine {
    Glide,
    Coil,
    GlideZoomable,
    CoilZoomable,
    GlideZoom,
    CoilZoom
}

enum class MediaFilterStrategy {
    Nothing,
    IgnoreSelected,
    AttachSelected
}

data class MainPageViewState(
    val maxSelectable: Int,
    val fastSelect: Boolean,
    val singleMediaType: Boolean,
    val includeGif: Boolean,
    val imageEngine: MediaImageEngine,
    val filterStrategy: MediaFilterStrategy,
    val captureStrategy: MediaCaptureStrategy,
    val capturePreferencesCustom: Boolean,
    val mediaList: List<MediaResource>,
    val onMaxSelectableChanged: (Int) -> Unit,
    val onFastSelectChanged: (Boolean) -> Unit,
    val onSingleMediaTypeChanged: (Boolean) -> Unit,
    val onIncludeGifChanged: (Boolean) -> Unit,
    val onImageEngineChanged: (MediaImageEngine) -> Unit,
    val onFilterStrategyChanged: (MediaFilterStrategy) -> Unit,
    val onCaptureStrategyChanged: (MediaCaptureStrategy) -> Unit,
    val onCapturePreferencesCustomChanged: (Boolean) -> Unit,
    val switchTheme: () -> Unit
)