# Matisse [![](https://jitpack.io/v/leavesCZY/Matisse.svg)](https://jitpack.io/#leavesCZY/Matisse)

一个用 Jetpack Compose 实现的 Android 图片选择框架

特点 & 优势：

- 适配到 Android 13
- 完全用 Kotlin & Jetpack Compose 实现
- 支持精细自定义主题，默认提供了 日间 和 夜间 两套主题
- 支持精准筛选图片类型，只显示想要的图片类型
- 支持详细获取图片信息，一共包含 uri、displayName、mimeType、width、height、orientation、size、path、bucketId、bucketDisplayName 十个属性值
- 支持开启拍照功能，一共包含 NothingCaptureStrategy、FileProviderCaptureStrategy、MediaStoreCaptureStrategy、SmartCaptureStrategy 四种拍照策略，可以自由选择是否要申请权限 

|                           日间主题                           |                           夜间主题                           |                          自定义主题                          |
| :----------------------------------------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: |
| ![](https://user-images.githubusercontent.com/30774063/221350097-6ef7343a-379a-4715-a86f-ea9e67674560.jpg) | ![](https://user-images.githubusercontent.com/30774063/221350113-251f2e7a-27dc-434b-b578-95e79267aae3.jpg) | ![](https://user-images.githubusercontent.com/30774063/221350303-07c065da-de5b-4550-ad89-92a1bfffba4d.jpg) |


实现思路请看：[Jetpack Compose 实现一个图片选择框架](https://juejin.cn/post/7108420791502372895)

Apk 下载体验请看：[Releases](https://github.com/leavesCZY/Matisse/releases)

接入指南请看：[Wiki](https://github.com/leavesCZY/Matisse/wiki/%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97)
