package com.example.lk_etch_robot.util

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

object BaseData {
    /**
     * 获取年月日
     */

//    val locationManager = getSystemService(Activity.LOCATION_SERVICE) as LocationManager
//    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//    val calendar: Calendar = Calendar.getInstance()
//    location?.let { calendar.setTimeInMillis(it.getTime()) }
//    val df = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
//    var s = df.format(calendar.getTime())
//    LogUtil.e("TAG",s)
    @SuppressLint("MissingPermission")
    fun getDate():String{
//        val locationManager = MyApplication.context.getSystemService(Activity.LOCATION_SERVICE) as LocationManager
//        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//        val calendar: Calendar = Calendar.getInstance()
//        location?.let { calendar.setTimeInMillis(it.getTime()) }
//        val df = SimpleDateFormat("yyyyMMdd")
        var date = UtcToLocalTime.timeFormatChange().replace("-","")
        return date
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getYearTop():String{
        var year = getDate()
        return BinaryChange.tenToHex(year.substring(0,2).toInt())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getYearBotton():String{
        var year = getDate()
        return BinaryChange.tenToHex(year.substring(2,4).toInt())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getMonth():String{
        var month = BinaryChange.tenToHex(getDate().substring(4,6).toInt())
        var hexMonth = if (month.length<2) {
            "0$month"
        }else{
            month
        }
        return hexMonth.uppercase(Locale.getDefault())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDay():String{
        var day = BinaryChange.tenToHex(getDate().substring(6,8).toInt())
        var hexDay = if (day.length<2) {
            "0$day"
        }else{
            day
        }
        return hexDay.uppercase(Locale.getDefault())
    }

    /**
     * 获取校验码
     */
    open fun hexStringToBytes(hexString: String?): String? {
        var hexString = hexString
        if (hexString == null || hexString == "") {
            return null
        }
        hexString = hexString.trim { it <= ' ' }
        hexString = hexString.uppercase()
        val length = hexString.length / 2
        var ad = 0
        for (i in 0 until length) {
            val pos = i * 2
            ad += Integer.valueOf(hexString.substring(pos,pos+2),16)
        }
        var checkData = Integer.toHexString(ad)
        if (checkData.length > 2) {
            checkData = checkData.substring(checkData.length - 2, checkData.length)
        }
        if (checkData.length == 1) {
            checkData = "0$checkData"
        }
        return checkData.uppercase()
    }
}
