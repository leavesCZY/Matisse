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

|                           日间主题                           |                           夜间主题                           |                          自定义主题                          |
| :----------------------------------------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: |
| ![](https://user-images.githubusercontent.com/30774063/173228764-1a44b35d-3bdd-4bcd-a456-b4ddd4f7a7e2.png) | ![](https://user-images.githubusercontent.com/30774063/173228768-1d8759d6-6a93-475c-89c5-e2ea9ddcf7ad.png) | ![](https://user-images.githubusercontent.com/30774063/173228769-58269c2e-9f54-4011-957d-6e5da14ad0d6.png) |


实现思路请看：[Jetpack Compose 实现一个图片选择框架](https://juejin.cn/post/7108420791502372895)

Apk 下载体验请看：[Releases](https://github.com/leavesCZY/Matisse/releases)

接入指南请看：[Wiki](https://github.com/leavesCZY/Matisse/wiki/%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97)
