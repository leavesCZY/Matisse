package github.leavesczy.matisse

import com.android.build.api.dsl.LibraryExtension
import java.io.File

internal fun configureAndroidLibrary(libraryExtension: LibraryExtension) {
    libraryExtension.apply {
        defaultConfig.apply {
            consumerProguardFiles.add(File("consumer-rules.pro"))
        }
    }
}