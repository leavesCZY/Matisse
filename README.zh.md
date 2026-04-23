# Matisse

中文 | [English](./README.md)

## 功能

一个基于 Jetpack Compose 实现的 Android 图片和视频选择框架

- **广泛的兼容性**：有效解决了不同系统版本间的兼容性问题
- **最佳权限实践**：遵循 Android 权限管理最佳实践，按需申请权限
- **纯 Kotlin 与 Compose**：完全采用 Kotlin 和 Jetpack Compose 构建，拥抱现代 Android 开发
- **灵活的拍照策略**：支持多种拍摄策略，并允许开发者自定义拍照逻辑
- **自定义图片加载**：支持接入各类图片加载框架（如 Coil, Glide）
- **灵活的选择模式**：支持同时选择图片和视频，也支持仅选择其中一种媒体类型
- **完善的主题系统**：提供深度的 UI 定制能力，并内置了浅色和深色两套默认主题

## 导入依赖

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

## 文档

[Matisse 接入指南](https://github.com/leavesCZY/Matisse/wiki)

|                                              日间主题                                              |                                              夜间主题                                              |                                             自定义主题                                              |
|:----------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------:|
| ![](https://github.com/leavesCZY/Matisse/assets/30774063/f2a0f801-d450-4c2c-81f8-07f71d6f6fd6) | ![](https://github.com/leavesCZY/Matisse/assets/30774063/7960c579-6ca7-4a63-bce7-f81d182e1df3) | ![](https://github.com/leavesCZY/Matisse/assets/30774063/d5a8b2cd-63fb-4c36-ad45-0659f4154bc5) |