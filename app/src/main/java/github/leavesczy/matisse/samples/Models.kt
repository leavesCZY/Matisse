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
    val mediaList: List<MediaResource>
)