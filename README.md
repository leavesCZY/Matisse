# Matisse [![](https://jitpack.io/v/leavesCZY/Matisse.svg)](https://jitpack.io/#leavesCZY/Matisse)

一个用 Jetpack Compose 实现的 Android 图片/视频 选择框架

特点 & 优势：

- 更新构建脚本为gradle kts,依赖使用properties配置,避免代码看上去乱糟糟的
- 全部换成大陆镜像(尽量避免使用阿里云,官网是写着说中央仓库限制了,然而腾讯没有一点影响)
- 升级accompanist到0.31,0.31版本以下不再兼容,[解决no such method(no static method xxxx)问题](https://github.com/leavesCZY/Matisse/issues/10)
- 适配到 Android 13
- 解决了多个系统兼容性问题
- 按需索取权限，极简的权限声明
- 支持自定义主题，提供了日夜间两套默认主题
- 完全用 Kotlin & Jetpack Compose 实现
- 支持多种拍照策略，可以自由选择是否要申请权限

接入指南：[Wiki](https://github.com/leavesCZY/Matisse/wiki)

Apk 下载：[Releases](https://github.com/leavesCZY/Matisse/releases)

|                                                    日间主题                                                    |                                                    夜间主题                                                    |                                                   自定义主题                                                    |
|:----------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------:|
| ![](https://user-images.githubusercontent.com/30774063/221350097-6ef7343a-379a-4715-a86f-ea9e67674560.jpg) | ![](https://user-images.githubusercontent.com/30774063/221350113-251f2e7a-27dc-434b-b578-95e79267aae3.jpg) | ![](https://user-images.githubusercontent.com/30774063/221350303-07c065da-de5b-4550-ad89-92a1bfffba4d.jpg) |