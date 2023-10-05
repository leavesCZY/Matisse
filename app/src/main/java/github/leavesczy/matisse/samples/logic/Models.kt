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

enum class MediaCapturePreferences {
    Normal,
    Custom
}

enum class MediaImageEngine {
    Coil,
    CoilZoomable,
    CoilZoom,
    Glide,
    GlideZoomable,
    GlideZoom
}

enum class MediaFilterStrategy {
    Close,
    IgnoreSelected,
    AttachSelected
}

data class MainPageViewState(
    val maxSelectable: Int,
    val mediaType: MediaType,
    val imageEngine: MediaImageEngine,
    val captureStrategy: MediaCaptureStrategy,
    val filterStrategy: MediaFilterStrategy,
    val capturePreferences: MediaCapturePreferences,
    val mediaList: List<MediaResource>,
    val onMaxSelectableChanged: (Int) -> Unit,
    val onMediaTypeChanged: (MediaType) -> Unit,
    val onCaptureStrategyChanged: (MediaCaptureStrategy) -> Unit,
    val onCapturePreferencesChanged: (MediaCapturePreferences) -> Unit,
    val onFilterStrategyChanged: (MediaFilterStrategy) -> Unit,
    val onImageEngineChanged: (MediaImageEngine) -> Unit,
    val switchTheme: () -> Unit
)