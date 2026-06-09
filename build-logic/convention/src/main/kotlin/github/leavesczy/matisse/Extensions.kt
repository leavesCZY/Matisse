package github.leavesczy.matisse

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * @Author: leavesCZY
 * @Date: 2026/4/30 21:03
 * @Desc:
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.appVersionCode(): Int =
    libs.findVersion("app-version-code").get().requiredVersion.toInt()

internal fun Project.appVersionName(): String =
    libs.findVersion("app-version-name").get().requiredVersion

internal fun Project.androidCompileSdkVersion(): Int =
    libs.findVersion("android-compile-sdk").get().requiredVersion.toInt()

internal fun Project.androidTargetSdkVersion(): Int =
    libs.findVersion("android-target-sdk").get().requiredVersion.toInt()

internal fun Project.androidMinSdkVersion(): Int =
    libs.findVersion("android-min-sdk").get().requiredVersion.toInt()

internal fun Project.androidBuildToolsVersion(): String =
    libs.findVersion("android-build-tools").get().requiredVersion