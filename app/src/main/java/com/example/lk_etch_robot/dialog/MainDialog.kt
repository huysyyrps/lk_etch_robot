package com.example.lk_etch_robot.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.service.autofill.SaveCallback
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.activity.MainActivity
import com.example.lk_etch_robot.util.showToast
import com.warkiz.tickseekbar.OnSeekChangeListener
import com.warkiz.tickseekbar.SeekParams
import com.warkiz.tickseekbar.TickSeekBar
import kotlinx.android.synthetic.main.dialog_battery.*
import kotlinx.android.synthetic.main.dialog_setting.*
import kotlinx.android.synthetic.main.dialog_setting.view.*
import kotlinx.android.synthetic.main.dialog_wifi.view.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class MainDialog {
    /**
     * 初始化重新扫描扫描dialog
     */
    private lateinit var dialog: MaterialDialog

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission", "ResourceType")
    fun SettingDialog(
        activity: MainActivity,
        pipeDiameter: Int,
        protectElectQuantity: Int,
        changeElectQuantity: Int,
        protectCurrent: Float,
        settingDialogCallBack: SettingDialogCallBack
    ) {
        dialog = MaterialDialog(activity)
            .cancelable(true)
            .show {
                customView(
                    //自定义弹窗
                    viewRes = R.layout.dialog_setting,//自定义文件
                    dialogWrapContent = true,    //让自定义宽度生效
                    scrollable = true,            //让自定义宽高生效
                    noVerticalPadding = true,   //让自定义高度生效
                )
                cornerRadius(16f)
            }
        dialog.etPipeDiameter.setText("$pipeDiameter")
        dialog.tvProtectElectQuantity.setText("$protectElectQuantity")
        dialog.tvChangeElectQuantity.setText("$changeElectQuantity")
        dialog.tvProtectCurrent.setText("$protectCurrent")


//        dialog.btnFormCancel.setOnClickListener {
//            dialog.dismiss()
//            settingDialogCallBack.dimiss()
//        }
        dialog.btnFormSure.setOnClickListener {
            if (dialog.etPipeDiameter.text.toString().trim { it <= ' ' } == "") {
                "表面曲率".showToast(activity)
                return@setOnClickListener
            }
            if (dialog.tvProtectElectQuantity.text.toString().trim { it <= ' ' } == "") {
                "保护电量不能为空".showToast(activity)
                return@setOnClickListener
            }
            if (dialog.tvChangeElectQuantity.text.toString().trim { it <= ' ' } == "") {
                "强制切换备用电源电量不能为空".showToast(activity)
                return@setOnClickListener
            }
            if (dialog.tvProtectCurrent.text.toString().trim { it <= ' ' } == "") {
                "保护电流不能为空".showToast(activity)
                return@setOnClickListener
            }

            if (dialog.tvProtectElectQuantity.text.toString().toInt() < 20 || dialog.tvProtectElectQuantity.text.toString().toInt() > 90) {
                "保护电量范围20~90".showToast(activity)
                return@setOnClickListener
            }
            if (dialog.tvChangeElectQuantity.text.toString().toInt() < 10 || dialog.tvChangeElectQuantity.text.toString()
                    .toInt() > dialog.tvProtectElectQuantity.text.toString().toInt()
            ) {
                "强制切换备用电源电量范围10~${dialog.tvProtectElectQuantity.text}".showToast(activity)
                return@setOnClickListener
            }
            if (dialog.tvProtectCurrent.text.toString().toFloat() < 2.0 || dialog.tvProtectCurrent.text.toString().toFloat() > 6.0) {
                "保护电流范围2.0~6.0".showToast(activity)
                return@setOnClickListener
            }

            var power = "00"
//            when (dialog.tabLayout.selectedTabPosition) {
//                0 -> {
//                    power = "00"
//                }
//                1 -> {
//                    power = "01"
//                }
//            }

            settingDialogCallBack.callBack(
                dialog.etPipeDiameter.text.toString(),
                dialog.tvProtectElectQuantity.text.toString(),
                dialog.tvChangeElectQuantity.text.toString(),
                dialog.tvProtectCurrent.text.toString(),
                power
            )
            dialog.dismiss()
        }
    }

    fun SettingDialog1(
        activity: MainActivity,
        pipeDiameter: Int,
        protectElectQuantity: Int,
        changeElectQuantity: Int,
        protectCurrent: Float,
        settingDialogCallBack: SettingDialogCallBack
    ) {
        val builder = AlertDialog.Builder(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_setting, null)
        builder.setView(view)
        val dialog = builder.create()
        view.etPipeDiameter.setText("$pipeDiameter")
        view.tvProtectElectQuantity.text = "$protectElectQuantity"
        view.sbProtectElectQuantity.progress = protectElectQuantity
        view.tvChangeElectQuantity.text = "$changeElectQuantity"
        view.sbChangeElectQuantity.progress = changeElectQuantity
        view.tvProtectCurrent.text = "$protectCurrent"
        view.sbProtectCurrent.setProgress(protectCurrent)

        view.sbProtectElectQuantity.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                view.tvProtectElectQuantity.text = "$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        view.sbChangeElectQuantity.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                view.tvChangeElectQuantity.text = "$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
//        view.sbProtectCurrent.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                view.tvProtectCurrent.text = "$progress"
//            }
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//            }
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//            }
//
//        })
        view.sbProtectCurrent.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                val decimalFormat = DecimalFormat("#.0")
                val formattedNumber: String = decimalFormat.format(seekParams.progressFloat/25+2)
                view.tvProtectCurrent.text = formattedNumber
            }

            override fun onStartTrackingTouch(seekBar: TickSeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: TickSeekBar) {
            }
        }

        view.ivClose.setOnClickListener {
            dialog.dismiss()
            settingDialogCallBack.dimiss()
        }
        view.btnFormSure.setOnClickListener {
            if (view.etPipeDiameter.text.toString().trim { it <= ' ' } == "") {
                "表面曲率不能为空".showToast(activity)
                return@setOnClickListener
            }
            if (view.tvProtectElectQuantity.text.toString().trim { it <= ' ' } == "") {
                "保护电量不能为空".showToast(activity)
                return@setOnClickListener
            }
            if (view.tvChangeElectQuantity.text.toString().trim { it <= ' ' } == "") {
                "强制切换备用电源电量不能为空".showToast(activity)
                return@setOnClickListener
            }
            if (view.tvProtectCurrent.text.toString().trim { it <= ' ' } == "") {
                "保护电流不能为空".showToast(activity)
                return@setOnClickListener
            }

            if (view.tvProtectElectQuantity.text.toString().toInt() < 20 || view.tvProtectElectQuantity.text.toString().toInt() > 90) {
                "保护电量范围20~90".showToast(activity)
                return@setOnClickListener
            }
            if (view.tvChangeElectQuantity.text.toString().toInt() < 10 || view.tvChangeElectQuantity.text.toString()
                    .toInt() > view.tvProtectElectQuantity.text.toString().toInt()
            ) {
                "强制切换备用电源电量范围10~${view.tvProtectElectQuantity.text}".showToast(activity)
                return@setOnClickListener
            }
            if (view.tvProtectCurrent.text.toString().toFloat() < 2.0 || view.tvProtectCurrent.text.toString().toFloat() > 6.0) {
                "保护电流范围2.0~6.0".showToast(activity)
                return@setOnClickListener
            }

            var power = "00"
            power = if (view.swOffOn.isChecked) {
                "00"
            } else {
                "01"
            }



            settingDialogCallBack.callBack(
                view.etPipeDiameter.text.toString(),
                view.tvProtectElectQuantity.text.toString(),
                view.tvChangeElectQuantity.text.toString(),
                view.tvProtectCurrent.text.toString(),
                power
            )
            dialog.dismiss()
        }
        dialog.show()
    }


    fun showBatteryDialog(activity: MainActivity, title: String) {
        dialog = MaterialDialog(activity)
            .cancelable(true)
            .show {
                customView(    //自定义弹窗
                    viewRes = R.layout.dialog_battery,//自定义文件
                    dialogWrapContent = true,    //让自定义宽度生效
                    scrollable = true,            //让自定义宽高生效
                    noVerticalPadding = true    //让自定义高度生效
                )
                cornerRadius(16f)
            }
        dialog.tvBattery.text = "$title${activity.resources.getString(R.string.battery_show_title)}"
        dialog.btnSaveCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.btnSaveSure.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun showWifiDialog(activity: Activity, param: SaveDialogCallBack){
        val builder = AlertDialog.Builder(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_wifi, null)
        builder.setView(view)
        val dialog = builder.create()
        view.btnWifiCancel.setOnClickListener {
            dialog.dismiss()
        }
        view.btnWifiSure.setOnClickListener {
            dialog.dismiss()
            param.callBack("")
        }
        dialog.show()
    }

    /**
     * 获取当前时间,用来给文件夹命名
     */
    private fun getNowDate(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return format.format(Date()) + ".mp4"
    }
}