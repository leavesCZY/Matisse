package github.leavesczy.matisse.samples.logic

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

enum class MediaImageEngine {
    Coil,
    Glide
}

data class MainPageViewState(
    val maxSelectable: Int,
    val mediaType: MediaType,
    val supportGif: Boolean,
    val imageEngine: MediaImageEngine,
    val captureStrategy: MediaCaptureStrategy,
    val mediaList: List<MediaResource>,
    val onMaxSelectableChanged: (Int) -> Unit,
    val onMediaTypeChanged: (MediaType) -> Unit,
    val onSupportGifChanged: (Boolean) -> Unit,
    val onCaptureStrategyChanged: (MediaCaptureStrategy) -> Unit,
    val onImageEngineChanged: (MediaImageEngine) -> Unit,
    val switchTheme: () -> Unit
)