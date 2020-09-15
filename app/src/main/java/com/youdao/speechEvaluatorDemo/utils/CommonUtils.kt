package com.youdao.speechEvaluatorDemo.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private var sLastClickTime: Long = 0L
private const val INTERVAL = 500 //500毫秒

fun isFastDoubleClick(): Boolean {
    return if (System.currentTimeMillis() - sLastClickTime < INTERVAL) {
        true
    } else {
        sLastClickTime = System.currentTimeMillis()
        false
    }
}

fun isPermissionGranted(@NonNull context: Context, @NonNull permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) ===
            PackageManager.PERMISSION_GRANTED
}

fun requestPermission(activity: Activity, permissions: Array<String>, requestCode: Int): Boolean {
    var hasPermission = true
    for (permission in permissions) {
        if (!isPermissionGranted(
                activity,
                permission
            )
        ) {
            hasPermission = false
            break
        }
    }
    if (!hasPermission) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
    return hasPermission
}