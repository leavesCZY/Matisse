# 一、Matisse

一个基于 Jetpack Compose 实现的 Android 图片和视频选择框架

- **广泛的兼容性**：有效解决了不同系统版本间的兼容性问题
- **最佳权限实践**：遵循 Android 权限管理最佳实践，按需申请权限
- **纯 Kotlin 与 Compose**：完全采用 Kotlin 和 Jetpack Compose 构建，拥抱现代 Android 开发
- **灵活的拍照策略**：支持多种拍摄策略，并允许开发者自定义拍照逻辑
- **自定义图片加载**：支持接入各类图片加载框架（如 Coil, Glide）
- **灵活的选择模式**：支持同时选择图片和视频，也支持仅选择其中一种媒体类型
- **完善的主题系统**：提供深度的 UI 定制能力，并内置了浅色和深色两套默认主题

关联的文章：

- [Jetpack Compose 实现一个图片选择框架](https://juejin.cn/post/7108420791502372895)
- [Android 13 媒体权限适配指南](https://juejin.cn/post/7159999910748618766)

# 二、导入依赖

[![Maven Central](https://img.shields.io/maven-central/v/io.github.leavesczy/matisse.svg)](https://central.sonatype.com/artifact/io.github.leavesczy/matisse)

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencies {
    implementation("io.github.leavesczy:matisse:latestVersion")
}
```

Matisse 本身不传递 Coil 或 Glide 依赖。使用内置 `CoilImageEngine` / `GlideImageEngine` 时，还需按下文补充对应依赖。

库 Manifest 已声明选择器 / 拍照 Activity，以及用于解析系统相机、预览 Intent 的 `<queries>`。宿主**无需
**再声明这些组件；也不要用同名 Activity 覆盖库内配置。

# 三、基本使用

Matisse 包含两种使用场景，可以单独使用或者组合使用，分别对应两个 `ActivityResultContract`

- `MatisseContract`：展示系统相册内的图片和视频，支持同时开启拍照功能。选择器界面使用 `Theme.Matisse`
  ，固定为竖屏。宿主需提前在 Manifest 中声明 `mediaType` 对应的媒体读取权限，权限申请由选择器完成
- `MatisseCaptureContract`：启动独立拍照流程（可能先按需申请存储写入或相机权限），然后打开系统相机，不显示媒体选择界面。
  Activity 使用 `Theme.Matisse.Capture`（透明窗），不强制竖屏。此流程不请求媒体读取权限；宿主声明
  `CAMERA` 后会按需申请，存储权限和照片存储位置由 `captureStrategy` 决定

确认选择时，`MatisseContract` 返回非空的 `List<MediaResource>`；Activity 未以成功结果结束、结果 Intent
缺失或结果列表为空时返回 `null`。权限被拒或媒体加载失败不会自动结束选择器，用户返回后结果为 `null`
。若配置了 `captureStrategy`，选择器内拍照成功后不会结束流程，新照片会插入“全部”相册列表首位且不自动选中（详见下文
`captureStrategy`）。

`MatisseCaptureContract` 拍照并成功读取结果时返回 `MediaResource`；用户取消、相机不可用、权限被拒绝或结果无效时返回
`null`。

`MediaResource` 目前仅包含：

- `uri`：媒体 Uri。内置选择和拍照策略返回 `content://` Uri；实际访问范围取决于 Uri 来源及宿主权限
- `mimeType`：媒体的 MIME 类型，例如 `image/jpeg`、`video/mp4`。MediaStore
  未提供类型或自定义调用方传入非标准值时可能为空或无法识别，此时 `isImage` 与 `isVideo` 均为 `false`
- `isImage` / `isVideo`：根据 `mimeType` 是否以 `image/`、`video/` 开头判断

## 1、MatisseContract

Jetpack Compose：

```kotlin
val mediaPickerLauncher =
    rememberLauncherForActivityResult(contract = MatisseContract()) { result: List<MediaResource>? ->
        if (!result.isNullOrEmpty()) {
            val mediaResource = result[0]
            val uri = mediaResource.uri
            val mimeType = mediaResource.mimeType
        }
    }

val matisse = Matisse(
    maxSelectable = 1,
    imageEngine = CoilImageEngine(),
    mediaType = MediaType.ImageOnly
)
mediaPickerLauncher.launch(input = matisse)
```

View：

```kotlin
private val mediaPickerLauncher =
    registerForActivityResult(contract = MatisseContract()) { result: List<MediaResource>? ->
        if (!result.isNullOrEmpty()) {
            val mediaResource = result[0]
            val uri = mediaResource.uri
            val mimeType = mediaResource.mimeType
        }
    }

val matisse = Matisse(
    maxSelectable = 1,
    imageEngine = CoilImageEngine(),
    mediaType = MediaType.ImageOnly
)
mediaPickerLauncher.launch(input = matisse)
```

## 2、MatisseCaptureContract

Jetpack Compose：

```kotlin
val captureLauncher =
    rememberLauncherForActivityResult(contract = MatisseCaptureContract()) { result ->
        if (result != null) {
            val uri = result.uri
            val mimeType = result.mimeType
        }
    }

captureLauncher.launch(
    input = MatisseCapture(captureStrategy = MediaStoreCaptureStrategy())
)
```

View：

```kotlin
private val captureLauncher =
    registerForActivityResult(contract = MatisseCaptureContract()) { result: MediaResource? ->
        if (result != null) {
            val uri = result.uri
            val mimeType = result.mimeType
        }
    }

captureLauncher.launch(
    input = MatisseCapture(captureStrategy = MediaStoreCaptureStrategy())
)
```

# 四、请求参数

```kotlin
/**
 * 图片和视频选择器的启动配置。选择器 Activity 使用 Theme.Matisse，界面固定竖屏。
 * 可通过覆盖库内 matisse_* color / bool 资源定制外观。
 *
 * @param maxSelectable 最多可选择的媒体数量，必须大于 0
 * @param imageEngine 图片加载引擎。Matisse 不传递 Coil 或 Glide 依赖，宿主需要根据所选实现添加依赖，
 * 具体要求参见 CoilImageEngine 与 GlideImageEngine
 * @param gridColumns 媒体网格的列数，必须大于 0，默认为 4
 * @param fastSelect 是否启用快速选择。启用后点击缩略图会立即返回单个 MediaResource，
 * 不进入预览或多选确认流程，并且 maxSelectable 必须为 1，默认为 false
 * @param mediaType 需要展示的媒体类型，默认为 MediaType.ImageOnly
 * @param singleMediaType 是否禁止同时选择图片和视频。为 false 时允许在同一结果中混合图片和视频，
 * 默认为 true
 * @param captureStrategy 拍照策略。传入非空值，且已获得媒体读取权限（完整访问或部分访问均可）时，
 * 在“全部”相册中显示拍照入口。拍照成功后不结束选择器：新照片固定插入“全部”相册列表首位（拍照入口之后），
 * 且不会自动选中，由用户继续选择或确认。mediaType 必须包含图片（MediaType.includesImage 为 true），
 * 否则只能为 null。默认为 null
 */
data class Matisse(
    val maxSelectable: Int,
    val imageEngine: ImageEngine,
    val gridColumns: Int = 4,
    val fastSelect: Boolean = false,
    val mediaType: MediaType = MediaType.ImageOnly,
    val singleMediaType: Boolean = true,
    val captureStrategy: CaptureStrategy? = null
)

/**
 * 独立拍照功能的启动配置。通过 MatisseCaptureContract 启动后进入拍照流程
 * （可能先按需申请存储写入或相机权限），然后打开系统相机，不显示媒体选择界面。
 * Activity 使用 Theme.Matisse.Capture（透明窗、无媒体选择 UI），不强制竖屏。
 * 如果宿主在 Manifest 中声明了 CAMERA，Matisse 会按需申请相机权限；存储权限、输出位置和
 * FileProvider 配置要求由具体 CaptureStrategy 决定。
 *
 * @param captureStrategy 用于创建输出 Uri、读取拍照结果及清理无效结果的拍照策略，
 * 可参见 FileProviderCaptureStrategy、MediaStoreCaptureStrategy 与 SmartCaptureStrategy
 */
data class MatisseCapture(
    val captureStrategy: CaptureStrategy
)
```

## 1、maxSelectable

用于设置最多能选择几个媒体资源，必须大于 0。

## 2、imageEngine

`ImageEngine` 用于让引用方自由选择图片加载库：

```kotlin
interface ImageEngine : Parcelable {

    /**
     * 展示媒体网格和相册列表中的图片缩略图或视频封面缩略图。
     * 调用方会提供有界容器，实现应填充容器；允许通过裁切保持统一的缩略图尺寸。
     */
    @Composable
    fun Thumbnail(mediaResource: MediaResource)

    /**
     * 展示预览页面中的完整图片或视频封面。
     * 实现需要自行处理内容尺寸。图片应保持宽高比且避免裁切，超出预览区域时应提供可查看完整内容的方式；
     * 视频资源只需展示静态封面，视频播放由 Matisse 单独处理。
     */
    @Composable
    fun Preview(mediaResource: MediaResource)
}
```

Matisse 内置了 `GlideImageEngine` 和 `CoilImageEngine`。实现会随 `Matisse` 通过 Intent
传递，因此实现类及其成员必须满足 `Parcelable` 要求。需实现 `Thumbnail`（网格与相册列表缩略图）与
`Preview`（预览页完整图片或视频封面）两个主线程 Composable；实现应保持可重入且不得执行阻塞操作。

### GlideImageEngine

```kotlin
dependencies {
    implementation("com.github.bumptech.glide:compose:1.0.0-beta10")
}
```

```kotlin
val matisse = Matisse(
    maxSelectable = 1,
    imageEngine = GlideImageEngine(),
    mediaType = MediaType.ImageOnly
)
```

### CoilImageEngine

该实现直接使用 Coil 的视频解码器，因此必须通过 `implementation` 添加 `io.coil-kt.coil3:coil-compose`
和 `io.coil-kt.coil3:coil-video`。视频帧解码由引擎在请求中自行指定，宿主一般无需再全局注册
`VideoFrameDecoder`。

如需加载 GIF，还需添加 `io.coil-kt.coil3:coil-gif`，并在宿主的 `ImageLoader` 中注册对应的 GIF Decoder：

```kotlin
dependencies {
    val version = "3.6.2"
    implementation("io.coil-kt.coil3:coil-compose:$version")
    implementation("io.coil-kt.coil3:coil-video:$version")
    // 可选，需要展示 GIF 时引入
    implementation("io.coil-kt.coil3:coil-gif:$version")
}
```

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }
    }
}
```

```kotlin
val matisse = Matisse(
    maxSelectable = 1,
    imageEngine = CoilImageEngine(),
    mediaType = MediaType.ImageOnly
)
```

### 自定义

如果默认实现不满足需求，可以自行实现 `ImageEngine`。宿主项目需要开启 Jetpack
Compose，并参考 [Compose to Kotlin Compatibility Map](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
配置 Kotlin Compiler。建议通过 Parcelize 插件实现序列化。

内置引擎行为说明：

- `Thumbnail`：裁切并填满容器
- `Preview`：视频封面完整显示在预览区域内；非视频大图按容器宽度等比展示，支持纵向滚动；解码宽高最大限制为
  4096 像素。超过限制的图片会保持宽高比进行降采样，因此放大后清晰度可能降低

## 3、gridColumns

用于设置一行显示几个媒体资源，必须大于 0，默认值为 4。

## 4、fastSelect

启用后，点击缩略图会立即返回单个 `MediaResource`，不进入预览或多选确认流程。仅当 `maxSelectable == 1`
时可设为 `true`。

## 5、mediaType

```kotlin
// 仅图片
val imageOnly = MediaType.ImageOnly

// 仅视频
val videoOnly = MediaType.VideoOnly

// 图片 + 视频
val imageAndVideo = MediaType.ImageAndVideo

// 按 MIME 与 MediaStore 值精确匹配，仅支持 image/* 与 video/*
val mimeTypes = MediaType.MultipleMimeType(
    mimeTypes = setOf("image/gif", "video/mp4")
)
```

`MultipleMimeType` 不允许为空，且每项必须以 `image/` 或 `video/` 开头；其他类型无法匹配对应媒体权限与预览行为。

`MediaType.includesImage` / `includesVideo` 判定规则：

- `includesImage`：`ImageOnly` / `ImageAndVideo` 为 true；`VideoOnly` 为 false；`MultipleMimeType`
  中存在以 `image/` 开头的类型时为 true
- `includesVideo`：`VideoOnly` / `ImageAndVideo` 为 true；`ImageOnly` 为 false；`MultipleMimeType`
  中存在以 `video/` 开头的类型时为 true

权限申请依据上述属性，与具体 MIME 子集无关。

## 6、singleMediaType

当 `mediaType` 同时包含图片和视频时：

- `true`：禁止在同一结果中混合图片和视频，默认为 `true`
- `false`：允许混合选择

## 7、captureStrategy

用于支持两种拍照场景：

- 通过 `MatisseCaptureContract` 启动独立拍照流程
- 在选择器中，当媒体读取权限已授予（完整访问或部分访问均可）且 `captureStrategy`
  非空时，于“全部”相册显示拍照入口；拍照成功后不结束选择器，新照片固定插入列表首位（拍照入口之后），且不会自动选中

说明：

- `mediaType` 必须包含图片（`MediaType.includesImage` 为 true；例如不可为单独的
  `MediaType.VideoOnly`，也不可为仅含 `video/` 的 `MultipleMimeType`），否则 `captureStrategy` 只能为
  `null`
- 内置策略以 `.jpg` / `image/jpeg` 创建输出。`FileProviderCaptureStrategy` 返回前校验文件长度大于
  0，MIME 固定为 `image/jpeg`；`MediaStoreCaptureStrategy` 通过 MediaStore 查询该 Uri 对应记录，
  MIME 使用 MediaStore 记录值（通常为 `image/jpeg`），不校验文件字节是否非空

Matisse 提供三种默认实现：

### 1、FileProviderCaptureStrategy

通过 FileProvider 生成拍照 Uri。宿主必须在 Manifest 中配置 FileProvider，并将其 `authority`
传给构造参数。当前实现会在 `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)`
中创建文件，FileProvider 路径配置必须能够映射该目录。照片保存在应用专属外部存储目录，不会写入系统相册，也不需要
`WRITE_EXTERNAL_STORAGE`。当前内置实现使用 `.jpg` 文件名，并将返回结果的 MIME 类型固定标记为
`image/jpeg`；读取结果时会校验文件长度大于 0，否则视为无效。

如果宿主在 Manifest 中声明了 `CAMERA`，Matisse 会在需要时申请该权限；未声明时则直接调用系统相机。

```xml

<provider android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.FileProvider" android:exported="false"
    android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />
</provider>
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="Matisse" path="Pictures" />
</paths>
```

```kotlin
FileProviderCaptureStrategy(
    authority = "${context.packageName}.FileProvider"
)
```

> `FileProviderCaptureStrategy` 为最终类，不可继承。如需自定义存储位置，请自行实现 `CaptureStrategy`。

### 2、MediaStoreCaptureStrategy

通过 MediaStore 生成拍照 Uri，并将照片写入系统相册。

- Android 9 及以下：宿主必须在 Manifest 中声明 `WRITE_EXTERNAL_STORAGE`，Matisse 会在拍照前申请该权限
- Android 10 及以上：无需该权限

当前内置实现使用 `.jpg` 文件名，创建 MediaStore 记录时声明 `image/jpeg`（不设置 `IS_PENDING`
，以便系统相机可直接写入该 Uri）。相机返回后轮询查询该记录：Android 10 及以上仅匹配非 pending，Android 11
及以上同时排除已移入回收站的记录；查到则返回其 MIME 类型（通常仍为 `image/jpeg`），否则视为无效。

如果宿主在 Manifest 中声明了 `CAMERA`，Matisse 会在需要时申请该权限；未声明时则直接调用系统相机。

### 3、SmartCaptureStrategy

根据系统版本自动选择策略：

- Android 9 及以下：委托给传入的 `FileProviderCaptureStrategy`，照片保存在应用专属外部存储目录
- Android 10 及以上：委托给 `MediaStoreCaptureStrategy`，照片写入系统相册

因此，即使宿主仅在新系统上测试，也仍应按照 `FileProviderCaptureStrategy` 的要求完成 FileProvider
配置，以兼容 Android 9 及以下设备。

```kotlin
SmartCaptureStrategy(
    fileProviderCaptureStrategy = FileProviderCaptureStrategy(
        authority = "${context.packageName}.FileProvider"
    )
)
```

### 4、总结

| 拍照策略                        | 需要的权限                                    | 配置项               | 图片对用户是否可见                         |
|-----------------------------|------------------------------------------|-------------------|-----------------------------------|
| FileProviderCaptureStrategy | 无                                        | 需要配置 FileProvider | 否，保存在应用专属目录                       |
| MediaStoreCaptureStrategy   | Android 9 及以下需要 `WRITE_EXTERNAL_STORAGE` | 无                 | 是，写入系统相册                          |
| SmartCaptureStrategy        | 通常无需额外写权限                                | 需要配置 FileProvider | Android 9 及以下不可见；Android 10 及以上可见 |

选择建议：

- 应用本身已有写存储权限：可优先使用 `MediaStoreCaptureStrategy`
- 应用不想申请写存储权限：使用 `FileProviderCaptureStrategy` 或 `SmartCaptureStrategy`

### 5、自定义

`CaptureStrategy` 是接口。若内置策略无法满足需求，可自行实现。

Matisse 会先调用 `shouldRequestWriteExternalStoragePermission`；在完成必要的存储写入与相机权限处理后，再调用
`createImageUri` 启动系统相机。相机以成功结果返回后调用 `loadCapturedMedia`
；相机取消、拍照失败、`loadCapturedMedia` 返回 null，或再次启动拍照前清理仍挂起的 Uri 时，会调用
`deleteImageUri` 清理已创建的资源。若 `createImageUri` 返回 null，则不会调用 `deleteImageUri`
（尚无 Uri 可清理）。实现会随 Intent 传递，必须满足 `Parcelable`；Matisse 从主线程发起策略调用，实现不得阻塞调用线程，文件与
ContentResolver 操作应自行切换到后台调度器。

需要实现：

- `shouldRequestWriteExternalStoragePermission`：返回 true 时，宿主必须同时在 Manifest 中声明该权限。Android
  10 及以上通常应返回 false
- `createImageUri`：返回供外部相机写入的 Uri；返回 null 会取消本次拍照，且不会调用
  `deleteImageUri`。Matisse 会通过 `MediaStore.EXTRA_OUTPUT` 传递该 Uri，并授予外部相机临时读写权限
- `loadCapturedMedia`：读取拍照结果；返回 null 表示结果无效，随后会调用 `deleteImageUri`
- `deleteImageUri`：清理未产生有效结果的资源。相机取消、拍照失败、`loadCapturedMedia` 返回
  null，以及再次启动拍照前清理仍挂起的 Uri 时会调用；`createImageUri` 返回 null 时不会调用

可选覆盖：

- `createImageName`：默认生成 `IMG_yyyyMMdd_HHmmssSSS.jpg`

如果宿主声明了 `CAMERA`，Matisse 会按需申请；未声明则直接调用系统相机。

# 五、主题和文本

Matisse 提供日间和夜间两套默认主题，也支持进一步自定义。

- 选择器 Activity：`Theme.Matisse`（固定竖屏）
- 独立拍照 Activity：`Theme.Matisse.Capture`（透明窗，不强制竖屏）

在项目的 `values` 与 `values-night` 中按需覆盖下列**同名**资源即可。下面列出的是日间默认值，仅作参考；夜间默认值不同，不要把日间色值原样抄到
`values-night`。

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <bool name="matisse_status_bar_icons_dark">true</bool>
    <bool name="matisse_navigation_bar_icons_dark">true</bool>

    <color name="matisse_status_bar_background_color">#FFFFFFFF</color>
    <color name="matisse_navigation_bar_background_color">#FFFFFFFF</color>

    <color name="matisse_main_page_background_color">#FFF7F8FA</color>
    <color name="matisse_media_item_background_color">#FFE6E8ED</color>
    <color name="matisse_media_item_scrim_unselected_color">#08000000</color>
    <color name="matisse_media_item_scrim_selected_color">#66000000</color>
    <color name="matisse_capture_background_color">#FFE6E8ED</color>
    <color name="matisse_capture_icon_color">#665B6B82</color>
    <color name="matisse_media_video_icon_background_color">#F7FFFFFF</color>
    <color name="matisse_media_video_icon_color">#FF0B1F3A</color>

    <color name="matisse_top_bar_background_color">#FFFFFFFF</color>
    <color name="matisse_top_bar_icon_color">#FF0B1F3A</color>
    <color name="matisse_top_bar_text_color">#FF0B1F3A</color>
    <color name="matisse_dropdown_menu_background_color">#FFFFFFFF</color>
    <color name="matisse_dropdown_menu_text_color">#FF0B1F3A</color>

    <color name="matisse_bottom_bar_background_color">#FFFFFFFF</color>
    <color name="matisse_bottom_bar_preview_text_color">#FF0B1F3A</color>
    <color name="matisse_bottom_bar_preview_text_disabled_color">#FFA0AEC0</color>
    <color name="matisse_bottom_bar_confirm_text_color">#FF3B9AFF</color>
    <color name="matisse_bottom_bar_confirm_text_disabled_color">#FFA0AEC0</color>

    <color name="matisse_preview_page_background_color">#FF0B1220</color>
    <color name="matisse_preview_page_bottom_bar_background_color">#E6162033</color>
    <color name="matisse_preview_page_back_text_color">#FFF5F7FA</color>
    <color name="matisse_preview_page_confirm_text_color">#FF5BA3F7</color>
    <color name="matisse_preview_page_confirm_text_disabled_color">#665BA3F7</color>

    <color name="matisse_checkbox_circle_stroke_color">#F7FFFFFF</color>
    <color name="matisse_checkbox_circle_stroke_disabled_color">#66FFFFFF</color>
    <color name="matisse_checkbox_circle_fill_selected_color">#FF3B9AFF</color>
    <color name="matisse_checkbox_circle_fill_unselected_color">#26000000</color>
    <color name="matisse_checkbox_text_color">#FFFFFFFF</color>

    <color name="matisse_loading_indicator_color">#FF3B9AFF</color>
    <color name="matisse_video_player_background_color">#FF0B1220</color>

    <color name="matisse_empty_glow_color">#243B9AFF</color>
    <color name="matisse_empty_frame_fill_color">#FFFFFFFF</color>
    <color name="matisse_empty_frame_stroke_color">#FFD5DEEA</color>
    <color name="matisse_empty_landscape_color">#FF9AADC6</color>
    <color name="matisse_empty_shadow_color">#12000000</color>
    <color name="matisse_empty_title_text_color">#FF0B1F3A</color>
    <color name="matisse_empty_subtitle_text_color">#FF5A7394</color>
</resources>
```

也可以在宿主的 `strings.xml` 中覆盖默认文案：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="matisse_action_back">返回</string>
    <string name="matisse_action_preview">预览</string>
    <string name="matisse_action_confirm">确定</string>
    <string name="matisse_action_confirm_with_count">确定(%1$d/%2$d)</string>

    <string name="matisse_bucket_all">全部</string>

    <string name="matisse_error_read_media_permission">请授予相册访问权限后重试</string>
    <string name="matisse_error_write_storage_permission">请授予存储写入权限后重试</string>
    <string name="matisse_error_camera_permission">请授予拍照权限后重试</string>
    <string name="matisse_error_max_images">最多只能选择 %1$d 张图片</string>
    <string name="matisse_error_max_videos">最多只能选择 %1$d 个视频</string>
    <string name="matisse_error_max_media">最多只能选择 %1$d 个图片或视频</string>
    <string name="matisse_error_mixed_media">不能同时选择图片和视频</string>
    <string name="matisse_error_no_camera_app">没有可用于拍照的应用</string>

    <string name="matisse_empty_no_image_title">暂无图片</string>
    <string name="matisse_empty_no_image_subtitle">相册中没有可选择的图片</string>
    <string name="matisse_empty_no_video_title">暂无视频</string>
    <string name="matisse_empty_no_video_subtitle">相册中没有可选择的视频</string>
    <string name="matisse_empty_no_media_title">暂无媒体资源</string>
    <string name="matisse_empty_no_media_subtitle">相册中没有可选择的媒体资源</string>
    <string name="matisse_empty_no_permission_title">无法访问相册</string>
    <string name="matisse_empty_no_permission_subtitle">请授予相册访问权限后重试</string>
</resources>
```

# 六、声明权限

Matisse 不会在库 Manifest 中声明媒体读取权限，开发者需要按 `mediaType`
、系统版本和拍照策略按需声明。权限申请由选择器 / 拍照流程完成。

## 1、媒体读取权限

仅 `MatisseContract` 会申请媒体读取权限。实际请求内容同时取决于设备系统版本与宿主 `targetSdkVersion`
，以及 `MediaType.includesImage` / `includesVideo`。

### 设备为 Android 13 以下，或 targetSdkVersion 小于 33

申请并依赖 `READ_EXTERNAL_STORAGE`：

```xml

<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 设备为 Android 13 及以上，且 targetSdkVersion 大于等于 33

Matisse 按 `includesImage` / `includesVideo` 请求对应的 `READ_MEDIA_IMAGES` 和/或
`READ_MEDIA_VIDEO`：

- 包含图片（`ImageOnly`、`ImageAndVideo`，或 `MultipleMimeType` 含 `image/`）：`READ_MEDIA_IMAGES`
- 包含视频（`VideoOnly`、`ImageAndVideo`，或 `MultipleMimeType` 含 `video/`）：`READ_MEDIA_VIDEO`
- 同时包含图片和视频时，完整访问需两者都授予

未启用部分访问，或未获得部分访问权限时，已请求的图片和/或视频权限必须全部授予后才能进入选择界面。

因此 `targetSdkVersion` 大于等于 33 时，通常还要保留带 `maxSdkVersion="32"` 的旧权限，以覆盖低版本设备：

```xml

<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" /><uses-permission
android:name="android.permission.READ_MEDIA_VIDEO" /><uses-permission
android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
```

### Android 14 部分媒体访问（可选）

当设备为 Android 14 及以上、宿主 `targetSdkVersion` 大于等于 34，并且 Manifest 声明了：

```xml

<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
```

Matisse 会同时请求并接受用户授予的部分媒体访问权限：仅获得 `READ_MEDIA_VISUAL_USER_SELECTED` 也可进入选择器。

再次启动选择器时，若当前不是完整访问（包括仅有部分授权），会重新打开系统授权界面，以便调整可访问的媒体范围；已是完整访问时不会重复弹窗。

未声明该权限时，不会主动 opt-in 部分访问能力。

## 2、拍照相关权限

- `FileProviderCaptureStrategy` / `SmartCaptureStrategy`：通常无需写存储权限，但需要正确配置
  FileProvider
- `MediaStoreCaptureStrategy`：
    - Android 10 及以上：无需写存储权限
    - Android 9 及以下：会申请 `WRITE_EXTERNAL_STORAGE`；若 `minSdkVersion` 小于 29，Manifest 需声明该权限，可将
      `maxSdkVersion` 设为 28

```xml

<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

- `CAMERA`：可选。宿主声明后，Matisse 会在需要时申请；未声明则直接调用系统相机
