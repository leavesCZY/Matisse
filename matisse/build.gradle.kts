import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.matisse.android.library)
    alias(libs.plugins.matisse.android.compose)
    alias(libs.plugins.maven.publish)
    id("maven-publish")
    id("signing")
}

val signingKeyId = properties["signing.keyId"]?.toString()

android {
    namespace = "github.leavesczy.matisse"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    compileOnly(libs.coil.compose)
    compileOnly(libs.glide.compose)
}

val matisseVersion = "2.2.2"

if (signingKeyId == null) {
    publishing {
        publications {
            create<MavenPublication>("release") {
                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
} else {
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