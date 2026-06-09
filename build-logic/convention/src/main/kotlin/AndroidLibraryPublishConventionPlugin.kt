import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import github.leavesczy.matisse.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * @Author: leavesCZY
 * @Date: 2026/5/1 16:42
 * @Desc:
 */
class AndroidLibraryPublishConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(receiver = target) {
            apply(plugin = "com.vanniktech.maven.publish")
            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral()
                signAllPublications()
                configure(platform = AndroidSingleVariantLibrary())
                coordinates(
                    groupId = "io.github.leavesczy",
                    artifactId = "matisse",
                    version = libs.findVersion("leavesczy-matisse").get().toString()
                )
                pom {
                    name.set("Matisse")
                    description.set("An Android Image and Video Selection Framework Implemented with Jetpack Compose")
                    inceptionYear.set("2025")
                    url.set("https://github.com/leavesCZY/Matisse")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("leavesCZY")
                            name.set("leavesCZY")
                            url.set("https://github.com/leavesCZY")
                        }
                    }
                    scm {
                        url.set("https://github.com/leavesCZY/Matisse")
                        connection.set("scm:git:git://github.com/leavesCZY/Matisse.git")
                        developerConnection.set("scm:git:ssh://git@github.com/leavesCZY/Matisse.git")
                    }
                }
            }
        }
    }

}