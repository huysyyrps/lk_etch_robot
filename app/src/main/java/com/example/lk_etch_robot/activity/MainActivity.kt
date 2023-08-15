package com.example.lk_etch_robot.activity

import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.util.BaseActivity
import com.example.lk_etch_robot.util.LogUtil
import com.example.lk_etch_robot.util.PermissionRequest
import com.example.lk_etch_robot.util.showToast
import com.skydroid.fpvlibrary.serial.SerialPortConnection
import com.skydroid.fpvlibrary.serial.SerialPortControl
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException


class MainActivity : BaseActivity(), View.OnClickListener {
    //视频渲染
    private lateinit var mFPVVideoClient: FPVVideoClient
    //usb连接实例
    private lateinit var mSerialPortConnection: SerialPortConnection
    //FPV控制
    private lateinit var mSerialPortControl: SerialPortControl
    private val mainHanlder = Handler(Looper.getMainLooper())
    private var isSetTime = false
    private var exitTime: Long = 0

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PermissionRequest.requestPermission(this)
        rbCamera.setOnClickListener(this)
        rbVideo.setOnClickListener(this)
        rbVideoClose.setOnClickListener(this)
        rbSetting.setOnClickListener(this)
        rbBack.setOnClickListener(this)

        fPVVideoView.init()
        initConnection()
    }

    private fun initConnection() {
        //渲染视频相关
        mFPVVideoClient = FPVVideoClient()

        //硬件串口实例
        mSerialPortConnection = SerialPortConnection.newBuilder("/dev/ttyHS0", 4000000)
            .flags(1 shl 13)
            .build()
        mSerialPortConnection.setDelegate(object : SerialPortConnection.Delegate {
            override fun received(bytes: ByteArray, size: Int) {
                if (mFPVVideoClient != null) {
                    mFPVVideoClient.received(bytes, size)
                }
            }

            override fun connect() {
                if (mFPVVideoClient != null) {
                    mFPVVideoClient.startPlayback()
                }
            }
        })
        try {
            //打开串口
            mSerialPortConnection.openConnection()
            LogUtil.e("TAG", "连接成功")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mSerialPortControl = SerialPortControl(mSerialPortConnection)

        mFPVVideoClient.setDelegate(object : FPVVideoClient.Delegate {
            override fun onStopRecordListener(fileName: String) {
                //停止录像回调
            }

            override fun onSnapshotListener(fileName: String) {
                //拍照回调
            }

            //视频相关
            override fun renderI420(frame: ByteArray, width: Int, height: Int) {
                if (!isSetTime && mSerialPortControl != null) {
                    isSetTime = true
                    //设置相机时间
                    mSerialPortControl.setTime(System.currentTimeMillis())
                }
                fPVVideoView.renderI420(frame, width, height)
            }

            override fun setVideoSize(picWidth: Int, picHeight: Int) {
                fPVVideoView.setVideoSize(picWidth, picHeight, mainHanlder)
            }

            override fun resetView() {
                fPVVideoView.resetView(mainHanlder)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mSerialPortConnection != null) {
            try {
                mSerialPortConnection.closeConnection()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
//        mSerialPortConnection = null
        if (mFPVVideoClient != null) {
            mFPVVideoClient.stopPlayback()
        }
//        mFPVVideoClient = null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.rbCamera -> {
                if (PermissionRequest.requestPermission(this)) {
                    var saveImageState = mFPVVideoClient.captureSnapshot(null, null)
                    if (saveImageState) {
                        R.string.save_success.showToast(this)
                    } else {
                        R.string.save_faile.showToast(this)
                    }
                }
            }
            R.id.rbVideo -> {
                if (PermissionRequest.requestPermission(this)) {
                    var startVidepState = mFPVVideoClient.startRecord(null, null)//开始录像
                    if (startVidepState) {
                        R.string.start_video.showToast(this)
                    }
                    rbVideo.visibility = View.GONE
                    rbVideoClose.visibility = View.VISIBLE
                }
            }
            R.id.rbVideoClose->{
                rbVideo.visibility = View.VISIBLE
                rbVideoClose.visibility = View.GONE
                mFPVVideoClient.stopRecord()//结束录像
                R.string.save_success.showToast(this)
            }
            R.id.rbBack -> {
                if((System.currentTimeMillis()-exitTime) > 2000){
                    "再按一次退出程序".showToast(this)
                    exitTime = System.currentTimeMillis();
                } else {
                    finish()
                }
            }
        }
    }

}