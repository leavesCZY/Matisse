import com.android.build.api.dsl.LibraryExtension
import github.leavesczy.matisse.configureAndroidLibrary
import github.leavesczy.matisse.configureAndroidProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class LibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(receiver = target) {
            apply(plugin = "com.android.library")
            val libraryExtension = extensions.getByType<LibraryExtension>()
            configureAndroidProject(commonExtension = libraryExtension)
            configureAndroidLibrary(libraryExtension = libraryExtension)
        }
    }

}