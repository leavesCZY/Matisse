# Matisse

[中文](./README.zh.md) | English

## Features

A modern image and video picker framework for Android, built with Jetpack Compose.

- **Broad Compatibility**: Effectively resolves compatibility issues across various Android system
  versions.
- **Permission Best Practices**: Follows Android's recommended permission management practices by
  requesting permissions only as needed.
- **Pure Kotlin & Compose**: Built entirely with Kotlin and Jetpack Compose, embracing the modern
  Android development ecosystem.
- **Flexible Capture Strategies**: Supports multiple media capture strategies and allows developers
  to define custom camera logic.
- **Custom Image Loading**: Seamlessly integrate with any image loading library (such as Coil or
  Glide).
- **Versatile Selection Modes**: Supports picking images and videos simultaneously or filtering for
  a specific media type.
- **Comprehensive Theming**: Provides deep UI customization capabilities with built-in Light and
  Dark default themes.

## Dependency

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

## Documentation

[How to Use the Matisse](https://github.com/leavesCZY/Matisse/wiki)

|                                          Light Theme                                           |                                           Dark Theme                                           |                                          Custom Theme                                          |
|:----------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------:|
| ![](https://github.com/leavesCZY/Matisse/assets/30774063/f2a0f801-d450-4c2c-81f8-07f71d6f6fd6) | ![](https://github.com/leavesCZY/Matisse/assets/30774063/7960c579-6ca7-4a63-bce7-f81d182e1df3) | ![](https://github.com/leavesCZY/Matisse/assets/30774063/d5a8b2cd-63fb-4c36-ad45-0659f4154bc5) |