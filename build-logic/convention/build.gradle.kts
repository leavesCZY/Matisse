import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "github.leavesczy.matisse.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.kotlin.gradle)
    compileOnly(libs.maven.publish.gradle)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.app.android.application.get().pluginId
            implementationClass = "ApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.app.android.library.get().pluginId
            implementationClass = "LibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.app.android.compose.get().pluginId
            implementationClass = "ComposeConventionPlugin"
        }
        register("kotlinParcelize") {
            id = libs.plugins.app.kotlin.parcelize.get().pluginId
            implementationClass = "ParcelizeConventionPlugin"
        }
        register("libraryPublish") {
            id = libs.plugins.app.library.publish.get().pluginId
            implementationClass = "AndroidLibraryPublishConventionPlugin"
        }
    }
}