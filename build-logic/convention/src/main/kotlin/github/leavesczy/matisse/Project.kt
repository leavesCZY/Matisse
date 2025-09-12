package github.leavesczy.matisse

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * @Author: leavesCZY
 * @Date: 2025/9/12 15:32
 * @Desc:
 */
internal fun Project.configureAndroidProject() {
    val commonExtension =
        extensions.findByType<ApplicationExtension>() ?: extensions.findByType<LibraryExtension>()!!
    commonExtension.apply {
        compileSdk = 36
        buildToolsVersion = "36.0.0"
        defaultConfig {
            minSdk = 21
        }
        lint {
            checkDependencies = true
        }
        buildFeatures {
            buildConfig = false
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
    }
}