@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}
val composeCompilerVersion:String by properties
android {
    namespace = "github.leavesczy.matisse"
    compileSdk = 33
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.majorVersion
        freeCompilerArgs = arrayListOf(
            *freeCompilerArgs.toTypedArray(),
            *arrayListOf(
                "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            ).toTypedArray()
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
    }
}

dependencies {
    val composeUIVersion:String by properties
    val junitVersion:String by properties
    val junitExtVersion:String by properties
    val espressoVersion:String by properties
    val appcompatVersion:String by properties
    val activityComposeVersion:String by properties
    val composeBomVersion:String by properties
    val composeMaterial3Version:String by properties

    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    implementation("androidx.activity:activity-compose:activityComposeVersion")
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.material3:material3:$composeMaterial3Version")
    implementation("androidx.compose.material:material-icons-extended:$composeUIVersion")
}