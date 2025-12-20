package github.leavesczy.matisse

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.internal.dsl.LibraryExtensionImpl
import org.gradle.api.Project
import java.io.File

/**
 * @Author: leavesCZY
 * @Date: 2025/9/12 16:20
 * @Desc:
 */
internal fun Project.configureAndroidLibrary(commonExtension: CommonExtension) {
    commonExtension.apply {
        this as LibraryExtensionImpl
        defaultConfig.apply {
            consumerProguardFiles.add(File("consumer-rules.pro"))
        }
    }
}