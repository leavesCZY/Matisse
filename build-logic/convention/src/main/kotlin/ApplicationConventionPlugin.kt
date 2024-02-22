import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import github.leavesczy.matisse.configureAndroidApplication
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 18:05
 * @Desc:
 */
class ApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.plugin.parcelize")
            }
            extensions.configure<BaseAppModuleExtension> {
                configureAndroidApplication(commonExtension = this)
            }
        }
    }

}