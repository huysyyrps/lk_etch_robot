package com.example.lk_etch_robot

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent

class MyApplication : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
//        //软件更新库
//        UpdateAppUtils.init(context)
//        //初始化BLE库
//        BleManager.initialize(this)
//        //开启BLE库日志打印
//        Logger.enableLog(true)
//        startService(Intent(this, MediaService::class.java))
    }
}