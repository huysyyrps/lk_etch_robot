package com.example.lk_etch_robot.util

import android.annotation.SuppressLint
import android.app.Activity
import android.location.LocationManager
import com.example.lk_etch_robot.MyApplication
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object UtcToLocalTime {
    @SuppressLint("MissingPermission")
    fun timeFormatChange():String{
        val locationManager = MyApplication.context.getSystemService(Activity.LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val calendar: Calendar = Calendar.getInstance()
        location?.let { calendar.timeInMillis = it.getTime() }
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        var dateTime = df.format(calendar.getTime())
        var date = utc2Local(dateTime).split(" ")[0]
        return date
    }

    fun utc2Local(utcTime: String?): String {
        val utcFormater =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss") //UTC时间格式
        utcFormater.timeZone = TimeZone.getTimeZone("UTC")
        var gpsUTCDate: Date? = null
        try {
            gpsUTCDate = utcFormater.parse(utcTime)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        val localFormater =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss") //当地时间格式
        localFormater.timeZone = TimeZone.getDefault()
        return localFormater.format(gpsUTCDate!!.time)
    }
}