package github.leavesczy.matisse

import com.android.build.api.dsl.LibraryExtension
import java.io.File

/**
 * @Author: leavesCZY
 * @Date: 2025/9/12 16:20
 * @Desc:
 */
internal fun configureAndroidLibrary(libraryExtension: LibraryExtension) {
    libraryExtension.apply {
        defaultConfig.apply {
            consumerProguardFiles.add(File("consumer-rules.pro"))
        }
    }
}