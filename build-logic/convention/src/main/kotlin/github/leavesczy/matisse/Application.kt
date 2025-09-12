package github.leavesczy.matisse

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 17:46
 * @Desc:
 */
internal fun Project.configureAndroidApplication() {
    configure<BaseAppModuleExtension> {
        defaultConfig {
            applicationId = "github.leavesczy.matisse.samples"
            targetSdk = 36
            versionCode = 1
            versionName = "1.0.0"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables {
                useSupportLibrary = true
            }
            applicationVariants.all {
                outputs.all {
                    if (this is ApkVariantOutputImpl) {
                        this.outputFileName = "matisse_${getApkBuildTime()}.apk"
                    }
                }
            }
        }
        signingConfigs {
            create("release") {
                storeFile = File(rootDir.absolutePath, "key.jks")
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
                    "**/**/*.properties",
                    "META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/CHANGES",
                    "DebugProbesKt.bin",
                    "kotlin-tooling-metadata.json"
                )
            }
        }
    }
}

private fun getApkBuildTime(): String {
    val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    simpleDateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    val time = Calendar.getInstance().time
    return simpleDateFormat.format(time)
}