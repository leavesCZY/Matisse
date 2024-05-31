package github.leavesczy.matisse

import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 17:53
 * @Desc:
 */
internal fun Project.configureAndroidLibrary(commonExtension: LibraryExtension) {
    commonExtension.apply {
        compileSdk = 34
        defaultConfig {
            minSdk = 21
        }
        buildFeatures {
            buildConfig = false
        }
        lint {
            checkDependencies = true
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }
}