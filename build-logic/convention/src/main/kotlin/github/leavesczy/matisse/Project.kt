package github.leavesczy.matisse

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureAndroidProject(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk {
            version = release(version = androidCompileSdkVersion())
        }
        buildToolsVersion = androidBuildToolsVersion()
        defaultConfig.apply {
            minSdk {
                version = release(version = androidMinSdkVersion())
            }
        }
        buildFeatures.apply {
            buildConfig = false
        }
        lint.apply {
            checkDependencies = true
        }
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
}