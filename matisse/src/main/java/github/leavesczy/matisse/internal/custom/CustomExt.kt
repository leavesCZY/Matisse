package github.leavesczy.matisse.internal.custom

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import github.leavesczy.matisse.ImageMimeTypePrefix
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.VideoMimeTypePrefix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


fun requestReadMediaPermissionCustom(
    mediaType: MediaType,
    applicationInfo: ApplicationInfo,
    checkPermissionResult: (permissions: Array<String>) -> Unit
) {
    val permissions =
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            buildList {
                add(READ_MEDIA_VISUAL_USER_SELECTED)
                if (mediaType.hasImage) {
                    add(element = READ_MEDIA_IMAGES)
                }
                if (mediaType.hasVideo) {
                    add(element = READ_MEDIA_VIDEO)
                }
            }.toTypedArray()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            buildList {
                if (mediaType.hasImage) {
                    add(element = READ_MEDIA_IMAGES)
                }
                if (mediaType.hasVideo) {
                    add(element = READ_MEDIA_VIDEO)
                }
            }.toTypedArray()
        } else {
            arrayOf(READ_EXTERNAL_STORAGE)
        }
    checkPermissionResult(permissions)
}

fun checkPermissionResultCustom(
    context: Context,
    api14Permission: String = "14",
    api13Permission: String = "13",
    api12Permission: String = "12",
    apiDenied: String = "denied",
    scope: CoroutineScope,
    onScopeIsNotActive: () -> Unit,
    requestReadMediaPermissionLauncher: ActivityResultLauncher<Array<String>>,
    onPermissionAllow: () -> Unit,
    permissions: Array<String>,
    onRequestDenied: () -> Unit
) : String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && (ContextCompat.checkSelfPermission(context, READ_MEDIA_IMAGES) == PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
            context,
            READ_MEDIA_VIDEO
        ) == PERMISSION_GRANTED)
    ) {
        // Android 13及以上完整照片和视频访问权限
        onPermissionAllow.invoke()
        return api13Permission
    } else if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        ContextCompat.checkSelfPermission(
            context,
            READ_MEDIA_VISUAL_USER_SELECTED
        ) == PERMISSION_GRANTED
    ) {
        // Android 14及以上部分照片和视频访问权限
        onPermissionAllow.invoke()
        return api14Permission
    } else if (ContextCompat.checkSelfPermission(
            context,
            READ_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED
    ) {
        // Android 12及以下完整本地读写访问权限
        onPermissionAllow.invoke()
        return api12Permission
    } else {
        // 无本地读写访问权限
        requestReadMediaPermissionLauncher.launch(permissions)
        if (!scope.isActive)
            onScopeIsNotActive.invoke()
        scope.launch {
            delay(300)
            onRequestDenied.invoke()
        }.start()
        return apiDenied
    }
}

private val MediaType.hasImage: Boolean
    get() {
        return when (this) {
            MediaType.ImageOnly, MediaType.ImageAndVideo -> {
                true
            }

            MediaType.VideoOnly -> {
                false
            }

            is MediaType.MultipleMimeType -> {
                mimeTypes.any {
                    it.startsWith(prefix = ImageMimeTypePrefix)
                }
            }
        }
    }

private val MediaType.hasVideo: Boolean
    get() {
        return when (this) {
            MediaType.VideoOnly, MediaType.ImageAndVideo -> {
                true
            }

            MediaType.ImageOnly -> {
                false
            }

            is MediaType.MultipleMimeType -> {
                mimeTypes.any {
                    it.startsWith(prefix = VideoMimeTypePrefix)
                }
            }
        }
    }

fun checkPermissionCustom(
    context: Context,
    api14Permission: String = "14",
    api13Permission: String = "13",
    api12Permission: String = "12",
    apiDenied: String = "denied",
): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && (ContextCompat.checkSelfPermission(context, READ_MEDIA_IMAGES) == PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
            context, READ_MEDIA_VIDEO
        ) == PERMISSION_GRANTED)
    ) {
        // Android 13及以上完整照片和视频访问权限
        return api13Permission
    } else if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        ContextCompat.checkSelfPermission(
            context,
            READ_MEDIA_VISUAL_USER_SELECTED
        ) == PERMISSION_GRANTED
    ) {
        // Android 14及以上部分照片和视频访问权限
        return api14Permission
    } else if (ContextCompat.checkSelfPermission(
            context,
            READ_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED
    ) {
        // Android 12及以下完整本地读写访问权限
        return api12Permission
    } else {
        // 无本地读写访问权限
        return apiDenied
    }
}