import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import github.leavesczy.matisse.configureAndroidApplication
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 18:05
 * @Desc:
 */
class ApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(receiver = target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.android")
            apply(plugin = "org.jetbrains.kotlin.plugin.parcelize")
            extensions.configure<BaseAppModuleExtension> {
                configureAndroidApplication(commonExtension = this)
            }
        }
    }

}