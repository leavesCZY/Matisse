package github.leavesczy.matisse.internal.custom

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat


@Composable
private fun PermissionBottom(text: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 30.dp)
            .fillMaxWidth()
            .background(
                Color(0xFF2F2F2F),
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White
        )
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                text = "Update Permission",
                fontSize = 10.sp,
                color = LocalContentColor.current
            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun PermissionAbout(
    innerPadding: PaddingValues,
    requestPermission: () -> Unit,
    showPermissionDialog: Boolean,
    permissionState: String,
    onClick: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
) {
    LaunchedEffect(key1 = Unit) {
        requestPermission.invoke()
    }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(innerPadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        when (permissionState) {
            "14" -> {
                PermissionBottom(
                    text = "\"BotaniQ\" is currently allowed to access selected photos. For full functionality, consider allowing access to all photos.",
                    onClick
                )
            }

            "denied" -> {
                PermissionBottom(
                    text = "Please allow \"BotaniQ\" to access and select your photos to enhance your user experience.",
                    onClick
                )
            }
        }
    }


    if (showPermissionDialog) {
        Dialog(
            onDismissRequest = onDismissPermissionDialog,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Photo Storage Permission",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "\"BotaniQ\" would like to access your Photos for saving and uploading photos to the app. This app will not automatically upload any of your photos or videos to the cloud, nor will it access content you have not selected. You won't be able to select photos for editing if you deny Photos permission.",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold

                    )
                }
            }
        }
    }
}

fun checkPermission(
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




