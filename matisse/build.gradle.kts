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
    compileOnly(libs.coil.compose)
    compileOnly(libs.glide.compose)
}