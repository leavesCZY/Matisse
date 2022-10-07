# Matisse [![](https://jitpack.io/v/leavesCZY/Matisse.svg)](https://jitpack.io/#leavesCZY/Matisse)

一个用 Jetpack Compose 实现的 Android 图片选择框架

特点 & 优势：

- 完全用 Kotlin 实现，拒绝 Java
- UI 层完全用 Jetpack Compose 实现，拒绝原生 View 体系
- 支持精细自定义主题，默认提供了 日间 和 夜间 两种主题
- 支持精准筛选图片类型，只显示想要的图片类型
- 支持在图片列表页开启拍照入口，同时支持 FileProvider 和 MediaStore 两种拍照策略
- 支持详细获取图片信息，一共包含 uri、displayName、mimeType、width、height、orientation、size、path、bucketId、bucketDisplayName 等十个属性值
- 适配到 Android 12

Apk 下载体验：[releases](https://github.com/leavesCZY/Matisse/releases)

|                           日间主题                           |                           夜间主题                           |                          自定义主题                          |
| :----------------------------------------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: |
| ![](https://user-images.githubusercontent.com/30774063/173228764-1a44b35d-3bdd-4bcd-a456-b4ddd4f7a7e2.png) | ![](https://user-images.githubusercontent.com/30774063/173228768-1d8759d6-6a93-475c-89c5-e2ea9ddcf7ad.png) | ![](https://user-images.githubusercontent.com/30774063/173228769-58269c2e-9f54-4011-957d-6e5da14ad0d6.png) |

# 一、引入

```kotlin
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}

dependencies {
    implementation "com.github.leavesCZY:Matisse:latestVersion"
}
```

# 二、使用

通过 MatisseContract 来启动 Matisse，在回调函数里获取用户选择的图片或拍摄的照片

```kotlin
class MainActivity : AppCompatActivity() {

    private val btnImagePicker by lazy {
        findViewById<View>(R.id.btnImagePicker)
    }

    private val matisseContractLauncher = registerForActivityResult(MatisseContract()) {
        if (it.isNotEmpty()) {
            val mediaResource = it[0]
            val imageUri = mediaResource.uri
            val imagePath = mediaResource.path
            val imageWidth = mediaResource.width
            val imageHeight = mediaResource.height
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnImagePicker.setOnClickListener {
            val matisse = Matisse()
            matisseContractLauncher.launch(matisse)
        }
    }

}
```

# 三、自定义

通过 Matisse 对象来实现自定义，一共支持以下六个自定义属性

```kotlin
/**
 * @param theme 主题。默认是日间主题
 * @param supportedMimeTypes 需要显示的图片类型。默认是包含 Gif 在内的所有图片
 * @param maxSelectable 可以选择的最大图片数量。默认是 1
 * @param spanCount 显示图片列表时的列表。默认是 4
 * @param tips 权限被拒绝、图片数量超限时的 Toast 提示
 * @param captureStrategy 拍照策略。默认不开启拍照功能
 */
data class Matisse(
    val theme: MatisseTheme = LightMatisseTheme,
    val supportedMimeTypes: List<MimeType> = ofImage(hasGif = true),
    val maxSelectable: Int = 1,
    val spanCount: Int = 4,
    val tips: MatisseTips = defaultMatisseTips,
    val captureStrategy: CaptureStrategy = NothingCaptureStrategy
)
```

## theme

theme 用于设置 Matisse 的主题，包括：背景色、系统状态栏颜色、文本颜色、字体大小、按钮文本、Icon

Matisse 提供了两种默认主题：

- LightMatisseTheme。日间主题
- DarkMatisseTheme。夜间主题

开发者可以在这两个主题的基础上，通过 `copy` 方法来快速构建想要的效果

```kotlin
LightMatisseTheme.copy(
    surfaceColor = Color.White,
    topAppBarTheme = TopAppBarTheme(
        defaultBucketName = "全部",
        backgroundColor = blueColor,
        contentColor = Color.White,
        fontSize = 18.sp,
    ),
)
```

或者是自己来完整实例化 MatisseTheme 对象

```kotlin
val darkColor = Color(color = 0xFF1F1F20)
val blueColor = Color(color = 0xFF03A9F4)
MatisseTheme(
    surfaceColor = Color.White,
    onPreviewSurfaceColor = darkColor,
    imageBackgroundColor = Color.LightGray.copy(alpha = 0.4f),
    alphaIfDisable = 0.5f,
    captureIconTheme = CaptureIconTheme(
        backgroundColor = Color.LightGray.copy(alpha = 0.4f),
        icon = Icons.Filled.PhotoCamera,
        tint = Color.White,
    ),
    topAppBarTheme = TopAppBarTheme(
        defaultBucketName = "全部",
        backgroundColor = blueColor,
        contentColor = Color.White,
        fontSize = 18.sp,
    ),
    bottomNavigationTheme = BottomNavigationTheme(
        backgroundColor = Color.White,
    ),
    dropdownMenuTheme = DropdownMenuTheme(
        backgroundColor = Color.White,
        textStyle = TextStyle(
            fontSize = 14.sp,
            color = Color.Black,
        ),
    ),
    checkBoxTheme = CheckBoxTheme(
        countable = true,
        frameColor = Color.Transparent,
        circleColor = Color.White,
        circleFillColor = blueColor,
        fontSize = 14.sp,
        textColor = Color.White,
    ),
    previewButtonTheme = PreviewButtonTheme(
        textBuilder = { selectedSize: Int, maxSelectable: Int ->
            "点击预览($selectedSize/$maxSelectable)"
        },
        textStyle = TextStyle(
            fontSize = 14.sp,
            color = blueColor,
        ),
    ),
    sureButtonTheme = SureButtonTheme(
        textBuilder = { selectedSize: Int, _: Int ->
            "使用($selectedSize)"
        },
        textStyle = TextStyle(
            fontSize = 14.sp,
            color = Color.White,
        ),
        backgroundColor = blueColor,
    ),
    systemBarsTheme = SystemBarsTheme(
        statusBarColor = blueColor,
        statusBarDarkIcons = false,
        navigationBarColor = Color.White,
        navigationBarDarkIcons = true
    ),
)
```

## supportedMimeTypes

supportedMimeTypes 用于设置想要展示的图片格式，默认包含以下所有类型

```kotlin
enum class MimeType(val type: String) {
    JPEG("image/jpeg"),
    PNG("image/png"),
    HEIC("image/heic"),
    HEIF("image/heif"),
    BMP("image/x-ms-bmp"),
    WEBP("image/webp"),
    GIF("image/gif");
}
```

如果希望只展示 Gif 类型的图片，可以这么做：

```kotlin
val matisse = Matisse(supportedMimeTypes = listOf(MimeType.GIF))
```

## maxSelectable

maxSelectable 用于设置允许选择的最大图片数量，默认是 1

## spanCount

spanCount 用于设置展示图片时的列数，默认是 4

## tips

tips 用于设置通过 Toast 向用户弹出的文案内容，包括：权限被拒绝时的提示、图片选择数量超限时的提示。默认值如下所示

```kotlin
data class MatisseTips(
    val onReadExternalStorageDenied: String,
    val onWriteExternalStorageDenied: String,
    val onCameraDenied: String,
    val onSelectLimit: (selectedSize: Int, maxSelectable: Int) -> String,
)

private val defaultMatisseTips = MatisseTips(onReadExternalStorageDenied = "请授予存储访问权限后重试",
    onWriteExternalStorageDenied = "请授予存储写入权限后重试",
    onCameraDenied = "请授予拍照权限后重试",
    onSelectLimit = { _: Int, maxSelectable: Int ->
        "最多只能选择${maxSelectable}张图片"
    }
)
```

## captureStrategy

captureStrategy 用于控制是否开启拍照入口，以及开启拍照功能后要采用的具体策略

Matisse 提供了三种默认实现，默认值是 NothingCaptureStrategy，代表不开启拍照功能。其他两种策略所需要的权限和图片存储的位置都不一样，对用户的感知也不一样

| 拍照策略                    | 所需权限                                                     | 配置项                    | 对用户是否可见                             |
| --------------------------- | ------------------------------------------------------------ | ------------------------- | ------------------------------------------ |
| NothingCaptureStrategy      |                                                              |                           |                                            |
| FileProviderCaptureStrategy | 无                                                           | 外部需要配置 FileProvider | 否，图片存储在应用私有目录内，对用户不可见 |
| MediaStoreCaptureStrategy   | Android 10 之前需要 WRITE_EXTERNAL_STORAGE 权限，Android 10 开始不需要权限 | 无                        | 是，图片存储在系统相册内，对用户可见       |

如果使用的是 FileProviderCaptureStrategy，外部还需要配置 FileProvider，authorities 视自身情况而定，通过 authorities 来实例化 FileProviderCaptureStrategy

```kotlin
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="github.leavesczy.matisse.samples.FileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

`file_paths.xml` 中需要配置 `external-files-path` 路径的 Pictures 文件夹，name 可以随意命名

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path
        name="Capture"
        path="Pictures" />
</paths>
```

开发者根据自己的实际情况来决定选择哪一种策略：

- 如果应用本身就需要申请 WRITE_EXTERNAL_STORAGE 权限的话，选 MediaStoreCaptureStrategy，拍照后的图片保存在系统相册中也比较符合用户的认知
- 如果应用本身就不需要申请 WRITE_EXTERNAL_STORAGE 权限的话，选 FileProviderCaptureStrategy，为了相册问题而多申请一个敏感权限得不偿失

# 四、权限

Matisse 要求一个必需权限和一个可选权限

## 必需权限

用于读取系统相册内的所有图片

```kotlin
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 可选权限

可选权限是否需要声明取决于开发者采用的拍照策略：

- MediaStoreCaptureStrategy。由于在 Android 10 之前向系统相册写入图片需要存储写入权限，所以需要申请 WRITE_EXTERNAL_STORAGE 权限。而 Android 10 开始之后的版本则不需要，因此可以将该权限的 maxSdkVersion 设为 28
- FileProviderCaptureStrategy。无需申请此权限

```kotlin
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

# 五、更多介绍

- [Jetpack Compose 实现一个图片选择框架](https://juejin.cn/post/7108420791502372895)

# 贡献者 ❤

<a href="https://github.com/leavesCZY/Matisse/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=leavesCZY/Matisse" />
</a>