import github.leavesczy.matisse.configureCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 18:05
 * @Desc:
 */
class ComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(receiver = target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            configureCompose()
        }
    }

}