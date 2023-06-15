@file:Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://mirrors.cloud.tencent.com/gradle/")
        }
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
    }
    plugins {
        val agpVersion: String by settings
        val kotlinVersion: String by settings
        id("com.android.application") version(agpVersion) apply(false)
        id("com.android.library") version(agpVersion) apply(false)
        kotlin("android") version(kotlinVersion) apply(false)
        kotlin("kapt") version(kotlinVersion) apply(false)
        id("org.jetbrains.kotlin.plugin.parcelize") version(kotlinVersion) apply(false)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
    }
}

rootProject.name = "Matisse"

include(":app")
include(":matisse")