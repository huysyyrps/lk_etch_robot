package com.example.lk_etch_robot.dialog

import android.annotation.SuppressLint
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.activity.MainActivity
import com.example.lk_etch_robot.util.showToast
import com.example.lk_etch_robot.view.IPSectionFilter
import kotlinx.android.synthetic.main.dialog_setting.*


class MainDialog {
    /**
     * 初始化重新扫描扫描dialog
     */
    private lateinit var dialog: MaterialDialog

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    fun SettingDialog(
        activity: MainActivity,
        protectElectQuantity: Int,
        changeElectQuantity: Int,
        protectCurrent: Float,
        settingDialogCallBack: SettingDialogCallBack
    ) {
        dialog = MaterialDialog(activity)
            .cancelable(true)
            .show {
                customView(    //自定义弹窗
                    viewRes = R.layout.dialog_setting,//自定义文件
                    dialogWrapContent = true,    //让自定义宽度生效
                    scrollable = true,            //让自定义宽高生效
                    noVerticalPadding = true    //让自定义高度生效
                )
                cornerRadius(16f)
            }
        dialog.etProtectElectQuantity.setText("$protectElectQuantity")
        dialog.etChangeElectQuantity.setText("$changeElectQuantity")
        dialog.etProtectCurrent.setText("$protectCurrent")


        dialog.btnFormCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.btnFormSure.setOnClickListener {
            if (dialog.etProtectElectQuantity.text.toString().trim { it <= ' ' } == "") {
                "保护电量不能为空".showToast(activity)
                return@setOnClickListener
            }
            if (dialog.etChangeElectQuantity.text.toString().trim { it <= ' ' } == "") {
                "强制切换备用电源电量不能为空".showToast(activity)
                return@setOnClickListener
            }
            if (dialog.etProtectCurrent.text.toString().trim { it <= ' ' } == "") {
                "保护电流不能为空".showToast(activity)
                return@setOnClickListener
            }


            if (dialog.etProtectElectQuantity.text.toString().toInt()<20||dialog.etProtectElectQuantity.text.toString().toInt()>90) {
                "保护电量范围20~90".showToast(activity)
                return@setOnClickListener
            }
            if (dialog.etChangeElectQuantity.text.toString().toInt()<10|| dialog.etChangeElectQuantity.text.toString().toInt()>dialog.etProtectElectQuantity.text.toString().toInt()) {
                "强制切换备用电源电量范围10~${dialog.etProtectElectQuantity.text}".showToast(activity)
                return@setOnClickListener
            }

            if (dialog.etProtectCurrent.text.toString().toFloat()<2.0|| dialog.etProtectCurrent.text.toString().toFloat()>6.0) {
                "保护电流范围2.0~6.0".showToast(activity)
                return@setOnClickListener
            }


            var power = "00"
            when (dialog.tabLayout.selectedTabPosition) {
                0 -> {
                    power = "00"
                }
                1 -> {
                    power = "01"
                }
            }

            settingDialogCallBack.callBack(
                dialog.etProtectElectQuantity.text.toString(),
                dialog.etChangeElectQuantity.text.toString(),
                dialog.etProtectCurrent.text.toString(),
                power
            )
            dialog.dismiss()
        }
    }
}