plugins {
    alias(libs.plugins.android.application).apply(apply = false)
    alias(libs.plugins.android.library).apply(apply = false)
    alias(libs.plugins.kotlin.compose).apply(apply = false)
    alias(libs.plugins.kotlin.parcelize).apply(apply = false)
    alias(libs.plugins.maven.publish).apply(apply = false)
}