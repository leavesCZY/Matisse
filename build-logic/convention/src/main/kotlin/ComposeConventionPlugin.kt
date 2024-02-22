import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import github.leavesczy.matisse.configureCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 18:05
 * @Desc:
 */
class ComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            val applicationExtension = extensions.findByType<ApplicationExtension>()
            if (applicationExtension != null) {
                configureCompose(commonExtension = applicationExtension)
            }
            val libraryExtension = extensions.findByType<LibraryExtension>()
            if (libraryExtension != null) {
                configureCompose(commonExtension = libraryExtension)
            }
        }
    }

}