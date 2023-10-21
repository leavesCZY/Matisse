@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
//        maven {
//            setUrl("https://maven.aliyun.com/repository/central")
//        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Matisse"
include(":app")
include(":matisse")