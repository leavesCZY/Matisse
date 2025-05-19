package github.leavesczy.matisse.internal.custom

import android.annotation.SuppressLint
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import github.leavesczy.matisse.R


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
                text = stringResource(R.string.permission_bottom_button),
                fontSize = 20.sp,
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
                    text = String.format(
                        stringResource(R.string.permission_bottom_14),
                        stringResource(id = R.string.app_name)
                    ),
                    onClick
                )
            }

            "denied" -> {
                PermissionBottom(
                    text = String.format(
                        stringResource(R.string.permission_bottom_denied),
                        stringResource(id = R.string.app_name)
                    ),
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
                        text = stringResource(R.string.dialog_permission_photo_storage),
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(
                            stringResource(R.string.dialog_permission_above_12),
                            stringResource(id = R.string.app_name)
                        ),
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold

                    )
                }
            }
        }
    }
}




