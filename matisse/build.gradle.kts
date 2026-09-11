plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.compose)
    alias(libs.plugins.app.kotlin.parcelize)
    alias(libs.plugins.app.library.publish)
}

android {
    namespace = "github.leavesczy.matisse"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    compileOnly(libs.glide)
    compileOnly(libs.glide.compose)
    compileOnly(libs.coil.gif)
    compileOnly(libs.coil.video)
    compileOnly(libs.coil.compose)
}