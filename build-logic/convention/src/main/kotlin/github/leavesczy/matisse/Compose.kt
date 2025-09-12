package github.leavesczy.matisse

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 17:51
 * @Desc:
 */
internal fun Project.configureCompose() {
    val commonExtension =
        extensions.findByType<ApplicationExtension>() ?: extensions.findByType<LibraryExtension>()!!
    commonExtension.apply {
        buildFeatures {
            compose = true
        }
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                optIn.set(
                    setOf(
                        "androidx.compose.foundation.ExperimentalFoundationApi",
                        "androidx.compose.foundation.layout.ExperimentalLayoutApi",
                        "com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi"
                    )
                )
            }
        }
    }
}