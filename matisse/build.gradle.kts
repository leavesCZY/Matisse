import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.compose)
    alias(libs.plugins.app.kotlin.parcelize)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "github.leavesczy.matisse"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    compileOnly(libs.coil.compose)
    compileOnly(libs.glide.compose)
}

val matisseVersion = "2.3.1"

val signingKeyId = properties["signing.keyId"]?.toString()

if (signingKeyId != null) {
    mavenPublishing {
        publishToMavenCentral()
        signAllPublications()
        configure(platform = AndroidSingleVariantLibrary())
        coordinates(
            groupId = "io.github.leavesczy",
            artifactId = "matisse",
            version = matisseVersion
        )
        pom {
            name = "Matisse"
            description =
                "An Android Image and Video Selection Framework Implemented with Jetpack Compose"
            inceptionYear = "2025"
            url = "https://github.com/leavesCZY/Matisse"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "leavesCZY"
                    name = "leavesCZY"
                    url = "https://github.com/leavesCZY"
                }
            }
            scm {
                url = "https://github.com/leavesCZY/Matisse"
                connection = "scm:git:git://github.com/leavesCZY/Matisse.git"
                developerConnection = "scm:git:ssh://git@github.com/leavesCZY/Matisse.git"
            }
        }
    }
}