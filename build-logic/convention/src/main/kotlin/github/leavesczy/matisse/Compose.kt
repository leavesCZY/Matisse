package github.leavesczy.matisse

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 17:51
 * @Desc:
 */
internal fun Project.configureCompose(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.value(
                setOf(
                    "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                    "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                    "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                    "-opt-in=com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi"
                )
            )
        }
    }
}