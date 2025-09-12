package github.leavesczy.matisse

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File

/**
 * @Author: leavesCZY
 * @Date: 2025/9/12 16:20
 * @Desc:
 */
internal fun Project.configureAndroidLibrary() {
    extensions.configure<LibraryExtension> {
        defaultConfig {
            consumerProguardFiles.add(File("consumer-rules.pro"))
        }
    }
}