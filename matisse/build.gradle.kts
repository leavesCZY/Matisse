plugins {
    alias(libs.plugins.matisse.android.library)
    alias(libs.plugins.matisse.android.compose)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "github.leavesczy.matisse"
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    compileOnly(libs.glide.compose)
    compileOnly(libs.coil.compose)
    compileOnly(libs.coil3.compose)
}

val signingKeyId = properties["signing.keyId"]?.toString()
val signingPassword = properties["signing.password"]?.toString()
val signingSecretKeyRingFile = properties["signing.secretKeyRingFile"]?.toString()
val mavenCentralUserName = properties["mavenCentral.username"]?.toString()
val mavenCentralPassword = properties["mavenCentral.password"]?.toString()
val mavenCentralEmail = properties["mavenCentral.email"]?.toString()
val matisseVersion = "2.1.4"

if (signingKeyId != null
    && signingPassword != null
    && signingSecretKeyRingFile != null
    && mavenCentralUserName != null
    && mavenCentralPassword != null
    && mavenCentralEmail != null
) {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "io.github.leavesczy"
                artifactId = "matisse"
                version = matisseVersion
                afterEvaluate {
                    from(components["release"])
                }
                pom {
                    name = "Matisse"
                    description =
                        "An Android Image and Video Selection Framework Implemented with Jetpack Compose"
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
                            email = mavenCentralEmail
                        }
                    }
                    scm {
                        url = "https://github.com/leavesCZY/Matisse"
                        connection = "https://github.com/leavesCZY/Matisse"
                        developerConnection = "https://github.com/leavesCZY/Matisse"
                    }
                }
            }
        }
        repositories {
            maven {
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = mavenCentralUserName
                    password = mavenCentralPassword
                }
            }
        }
    }
    signing {
        sign(publishing.publications["release"])
    }
} else {
    publishing {
        publications {
            create<MavenPublication>("release") {
                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
}