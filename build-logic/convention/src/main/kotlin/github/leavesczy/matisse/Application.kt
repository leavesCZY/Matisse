package github.leavesczy.matisse

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.kotlin.dsl.getByType
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal fun Project.configureAndroidApplication(applicationExtension: ApplicationExtension) {
    applicationExtension.apply {
        defaultConfig {
            applicationId = "github.leavesczy.matisse.samples"
            targetSdk {
                version = release(version = androidTargetSdkVersion())
            }
            versionCode = appVersionCode()
            versionName = appVersionName()
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables {
                useSupportLibrary = true
            }
        }
        val basePluginExtension = project.extensions.getByType<BasePluginExtension>()
        basePluginExtension.apply {
            archivesName.set("matisse_v${defaultConfig.versionName}_${defaultConfig.versionCode}_${getApkBuildTime()}")
        }
        signingConfigs {
            create("release") {
                storeFile = File(rootDir, "key.jks")
                keyAlias = "leavesCZY"
                keyPassword = "123456"
                storePassword = "123456"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
        buildTypes {
            debug {
                signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = false
                isShrinkResources = false
                isDebuggable = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
            release {
                signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = true
                isShrinkResources = true
                isDebuggable = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                ndk {
                    abiFilters.apply {
                        clear()
                        add("arm64-v8a")
                    }
                }
            }
        }
        packaging {
            jniLibs {
                excludes += setOf("META-INF/{AL2.0,LGPL2.1}")
            }
            resources {
                excludes += setOf(
                    "**/*.md",
                    "**/*.version",
                    "**/*.properties",
                    "**/*.kotlin_module",
                    "**/CHANGES",
                    "**/LICENSE.txt",
                    "**/{AL2.0,LGPL2.1}",
                    "**/DebugProbesKt.bin",
                    "**/app-metadata.properties",
                    "**/kotlin-tooling-metadata.json",
                    "**/version-control-info.textproto",
                    "**/androidsupportmultidexversion.txt"
                )
            }
        }
    }
}

private fun getApkBuildTime(): String {
    val now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    return now.format(formatter)
}