import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "github.leavesczy.matisse.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.kotlin.gradle)
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
    }
}