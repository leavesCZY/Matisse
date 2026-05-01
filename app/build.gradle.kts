plugins {
    alias(libs.plugins.app.android.application)
    alias(libs.plugins.app.android.compose)
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
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)
    implementation(libs.glide.compose)
    implementation(project(":matisse"))
}