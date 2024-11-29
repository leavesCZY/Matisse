plugins {
    alias(libs.plugins.matisse.android.application)
    alias(libs.plugins.matisse.android.compose)
}

android {
    namespace = "github.leavesczy.matisse.samples"
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.glide.compose)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)

    implementation(libs.coil3.compose)
    implementation(libs.coil3.gif)
    implementation(libs.coil3.video)

    implementation(project(":matisse"))
//    implementation(libs.matisse)
}