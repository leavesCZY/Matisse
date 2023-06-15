@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

val composeCompilerVersion:String by properties


android {
    namespace = "github.leavesczy.matisse.samples"
    compileSdk = 33
    defaultConfig {
        applicationId = "github.leavesczy.matisse.samples"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    signingConfigs {
        create("release") {
            keyAlias = "leavesCZY"
            keyPassword = "123456"
            storeFile = file(rootDir.absolutePath + File.separator + "key.jks")
            storePassword = "123456"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = false
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
                "-Xopt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            ).toTypedArray()
        )
    }
    packaging {
        resources {
            excludes += "META-INF/*.md"
            excludes += "META-INF/*.version"
            excludes += "META-INF/*.properties"
            excludes += "META-INF/CHANGES"
            excludes += "META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
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
    val lifecycleRuntimeVersion:String by properties
    val junitExtVersion:String by properties
    val espressoVersion:String by properties
    val appcompatVersion:String by properties
    val activityComposeVersion:String by properties
    val coilVersion:String by properties
    val glideVersion:String by properties
    val glideComposeVersion:String by properties
    val composeBomVersion:String by properties
    val composeMaterial3Version:String by properties
    val accompanistVersion:String by properties

    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")

    implementation("androidx.compose.ui:ui:$composeUIVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeUIVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeUIVersion")
    implementation("androidx.compose.material3:material3:$composeMaterial3Version")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeUIVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeUIVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeUIVersion")

    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleRuntimeVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))


    implementation("io.coil-kt:coil-gif:$coilVersion")
    implementation("io.coil-kt:coil-video:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")

    implementation("com.github.bumptech.glide:glide:$glideVersion")
    kapt("com.github.bumptech.glide:compiler:$glideVersion")
    implementation("com.github.bumptech.glide:compose:$glideComposeVersion")
    implementation(project(":matisse"))
}