package github.leavesczy.matisse.internal.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @Author: CZY
 * @Date: 2022/6/6 18:51
 * @Desc:
 */
internal object PermissionUtils {

    /**
     * 判断是否拥有指定权限
     */
    fun permissionGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            permissionGranted(context = context, permission = it)
        }
    }

    /**
     * 判断是否拥有指定权限
     */
    fun permissionGranted(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查应用是否声明了指定权限
     */
    suspend fun containsPermission(context: Context, permission: String): Boolean {
        return withContext(context = Dispatchers.Default) {
            try {
                val packageManager: PackageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_PERMISSIONS
                )
                val permissions = packageInfo.requestedPermissions
                if (!permissions.isNullOrEmpty()) {
                    return@withContext permissions.contains(permission)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return@withContext false
        }
    }

}