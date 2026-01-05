package github.leavesczy.matisse

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * @Author: leavesCZY
 * @Date: 2025/9/12 15:32
 * @Desc:
 */
internal fun Project.configureAndroidProject(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = 36
        buildToolsVersion = "36.1.0"
        defaultConfig.apply {
            minSdk = 23
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