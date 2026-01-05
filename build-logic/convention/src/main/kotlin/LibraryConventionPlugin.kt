import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import github.leavesczy.matisse.configureAndroidLibrary
import github.leavesczy.matisse.configureAndroidProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 18:05
 * @Desc:
 */
class LibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(receiver = target) {
            apply(plugin = "com.android.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.parcelize")
            val commonExtension = extensions.getByType(type = CommonExtension::class)
            configureAndroidProject(commonExtension = commonExtension)
            configureAndroidLibrary(libraryExtension = commonExtension as LibraryExtension)
        }
    }

}