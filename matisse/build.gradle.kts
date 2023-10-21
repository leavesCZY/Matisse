@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "github.leavesczy.matisse"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += setOf(
            "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = false
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "io.github.leavesczy"
            artifactId = "matisse"
            version = "0.0.1-test03"
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name = "Matisse"
                description = "A Image/Video Selector Implemented with Jetpack Compose"
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
                        email = properties.getting("ossrh.email")
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
                username = properties["ossrh.username"].toString()
                password = properties["ossrh.password"].toString()
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}

dependencies {
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.espresso)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material3)
}