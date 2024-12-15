package github.leavesczy.matisse

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
internal fun Project.configureAndroidApplication(commonExtension: BaseAppModuleExtension) {
    commonExtension.apply {
        compileSdk = 35
        buildToolsVersion = "35.0.0"
        defaultConfig {
            applicationId = "github.leavesczy.matisse.samples"
            minSdk = 23
            targetSdk = 35
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
        buildFeatures {
            buildConfig = false
        }
        lint {
            checkDependencies = true
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_11
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

fun getApkBuildTime(): String {
    val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
    simpleDateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    val time = Calendar.getInstance().time
    return simpleDateFormat.format(time)
}