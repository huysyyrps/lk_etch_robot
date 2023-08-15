package com.example.lk_etch_robot.util

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.lk_etch_robot.activity.MainActivity
import com.permissionx.guolindev.PermissionX
import java.util.ArrayList

object PermissionRequest {
    /**
    权限申请
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun requestPermission(activity: MainActivity): Boolean {
        var permissionTag = false
        val requestList = ArrayList<String>()
        requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        requestList.add(Manifest.permission.ACCESS_WIFI_STATE)
        requestList.add(Manifest.permission.CAMERA)
        if (requestList.isNotEmpty()) {
            PermissionX.init(activity)
                .permissions(requestList)
                .onExplainRequestReason { scope, deniedList ->
                    val message = "需要您同意以下权限才能正常使用"
                    scope.showRequestReasonDialog(deniedList, message, "同意", "取消")
                }
                .request { allGranted, _, deniedList ->
                    if (allGranted) {
                        Log.e("TAG", "所有申请的权限都已通过")
                        permissionTag = true
                    } else {
                        Log.e("TAG", "您拒绝了如下权限：$deniedList")
                        activity.finish()
                    }
                }
        }
        return permissionTag
    }
}