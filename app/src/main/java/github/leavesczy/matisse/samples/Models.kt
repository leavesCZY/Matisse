package github.leavesczy.matisse.samples

import github.leavesczy.matisse.MediaResource

enum class MediaType {
    All,
    Image,
    Video
}

enum class MediaCaptureStrategy {
    Nothing,
    FileProvider,
    MediaStore,
    Smart
}

data class MainPageViewState(
    val maxSelectable: Int,
    val mediaType: MediaType,
    val supportGif: Boolean,
    val captureStrategy: MediaCaptureStrategy,
    val mediaList: List<MediaResource>,
    val onMaxSelectableChanged: (Int) -> Unit,
    val onMediaTypeChanged: (MediaType) -> Unit,
    val onSupportGifChanged: (Boolean) -> Unit,
    val onCaptureStrategyChanged: (MediaCaptureStrategy) -> Unit,
    val switchTheme: () -> Unit
)