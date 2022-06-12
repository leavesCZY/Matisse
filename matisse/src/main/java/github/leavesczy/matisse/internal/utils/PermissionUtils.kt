package github.leavesczy.matisse.internal.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * @Author: CZY
 * @Date: 2022/6/6 18:51
 * @Desc:
 */
internal object PermissionUtils {

    /**
     * 检查是否已授权指定权限
     */
    fun checkSelfPermission(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查应用的 Manifest 文件是否声明了指定权限
     */
    fun containsPermission(context: Context, permission: String): Boolean {
        val packageManager: PackageManager = context.packageManager
        try {
            val packageInfo = packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions
            if (!permissions.isNullOrEmpty()) {
                return permissions.contains(permission)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

}