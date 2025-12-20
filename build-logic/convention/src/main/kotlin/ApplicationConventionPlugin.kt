import com.android.build.api.dsl.ApplicationExtension
import github.leavesczy.matisse.configureAndroidApplication
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
class ApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(receiver = target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.plugin.parcelize")
            val commonExtension = extensions.getByType(type = ApplicationExtension::class)
            configureAndroidApplication(commonExtension = commonExtension)
            configureAndroidProject(commonExtension = commonExtension)
        }
    }

}