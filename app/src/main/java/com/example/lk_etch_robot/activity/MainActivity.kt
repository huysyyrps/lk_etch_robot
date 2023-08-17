package com.example.lk_etch_robot.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.RequiresApi
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.util.*
import com.example.lk_etch_robot.dialog.MainDialog
import com.example.lk_etch_robot.dialog.SettingDialogCallBack
import com.example.lk_etch_robot.mediaprojection.CaptureImage
import com.skydroid.fpvlibrary.serial.SerialPortConnection
import com.skydroid.fpvlibrary.serial.SerialPortControl
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class MainActivity : BaseActivity(), View.OnClickListener {
    //视频渲染
    private lateinit var mFPVVideoClient: FPVVideoClient
    //usb连接实例
    private lateinit var mSerialPortConnection: SerialPortConnection
    //FPV控制
    private lateinit var mSerialPortControl: SerialPortControl
    //硬件串口连接实例（数传）
    private lateinit var mServiceConnection: SerialPortConnection
    private val mainHanlder = Handler(Looper.getMainLooper())
    private var isSetTime = false
    private var exitTime: Long = 0
    private var protectVoltage: String = "0V"
    private var protectCurrent: String = "0A"
    val timer = Timer()
    private lateinit var mediaManager: MediaProjectionManager
    private var mMediaProjection: MediaProjection? = null

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
        initVideo()
        initData()
    }

    /**
     * 视频相关
     */
    private fun initVideo() {
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

    /**
     * 数传
     */
    private fun initData() {
        //硬件串口实例
        mServiceConnection = SerialPortConnection.newBuilder("/dev/ttyHS1", 921600)
            .flags(1 shl 13)
            .build()
        mServiceConnection.setDelegate(object : SerialPortConnection.Delegate {
            override fun received(bytes: ByteArray, size: Int) {
                var stringData = ByteDataChange.ByteToString(bytes)
                LogUtil.e("TAG", stringData)
                //在设备上电后1S周期向遥控器接收端发送包含遥控器通讯帧率的数据包
                if (stringData.startsWith("AE01") && stringData.length == 8) {
                    if (ByteDataChange.HexStringToBytes(
                            stringData.substring(
                                0,
                                6
                            )
                        ) == stringData.subSequence(6, 8)
                    ) {
                        //主机接受后判断帧率信息正常通信帧率在50以上，根据帧率大小确定通讯是否安全，然后向主机发送握手命令
                        if (Integer.valueOf(stringData.substring(4, 6), 16) > 50) {
                            mServiceConnection.sendData("BE0101C0".toByteArray())
                        }
                    }
                }
                if (stringData.startsWith("AE03") && stringData.length == 14) {
                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 12)) == stringData.subSequence(12, 14)) {
                        var s = Integer.valueOf(stringData.substring(6, 8) + stringData.substring(4, 6), 16)
                        protectVoltage = "${s / 1000}A"
                        var s1 = Integer.valueOf(stringData.substring(10, 12) + stringData.substring(8, 10), 16)
                        protectCurrent = "${s1 / 1000}V"
                        //定时读取
                        timer.scheduleAtFixedRate(0, 1000) {
                            mServiceConnection.sendData("BE0201C1".toByteArray())
                        }
                    }
                }
                if (stringData.startsWith("AE02") && stringData.length == 22) {
                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 20)) == stringData.subSequence(20, 22)) {
                        var voltage = Integer.valueOf(stringData.substring(6, 8) + stringData.substring(4, 6), 16)
                        var current = Integer.valueOf(stringData.substring(10, 12) + stringData.substring(8, 10), 16)
                        var height = Integer.valueOf(stringData.substring(14, 15) + stringData.substring(12, 14), 16)
                        var lightState = Integer.valueOf(stringData.substring(16, 18), 16)
                        var rate = Integer.valueOf(stringData.substring(18, 20), 16)
                        tvVoltage.text = "${voltage / 1000}V"
                        tvCurrent.text = "${current / 1000}V"
                    }
                }
            }

            override fun connect() {
                LogUtil.e("TAG", "数传连接成功")
            }
        })
        try {
            //打开串口
            mServiceConnection.openConnection()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.rbCamera -> {
//                if (PermissionRequest.requestPermission(this)) {
//                    var saveImageState = mFPVVideoClient.captureSnapshot(null, null)
//                    if (saveImageState) {
//                        R.string.save_success.showToast(this)
//                    } else {
//                        R.string.save_faile.showToast(this)
//                    }
//                }
                mediaManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                if (mMediaProjection == null) {
                    val captureIntent: Intent = mediaManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, Constant.TAG_ONE)
                } else {
                    mMediaProjection?.let {
                        CaptureImage().captureImages(
                            this@MainActivity,
                            "image",
                            it
                        )
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
            R.id.rbVideoClose -> {
                rbVideo.visibility = View.VISIBLE
                rbVideoClose.visibility = View.GONE
                mFPVVideoClient.stopRecord()//结束录像
                R.string.save_success.showToast(this)
            }
            R.id.rbSetting -> {
                MainDialog().SettingDialog(this@MainActivity, protectVoltage, protectCurrent,
                    object : SettingDialogCallBack {
                        override fun callBack(protectVoltage: String, protectCurrent: String) {
                            if (mSerialPortConnection.isConnection) {
                                var hexProtectVoltage = ""
                                var hexProtectCurrent = ""
                                if (protectVoltage.endsWith("V")) {
                                    hexProtectVoltage =
                                        (protectVoltage.substring(0, protectVoltage.length - 1)
                                            .toInt() * 1000).toString(16)
                                }
                                if (protectCurrent.endsWith("A")) {
                                    hexProtectCurrent =
                                        (protectCurrent.substring(0, protectCurrent.length - 1)
                                            .toInt() * 1000).toString(16)
                                }
                                if (hexProtectVoltage.length==4){
                                    hexProtectVoltage = hexProtectVoltage.substring(2,4)+hexProtectVoltage.substring(0,2)
                                }
                                if (hexProtectCurrent.length==4){
                                    hexProtectCurrent = hexProtectCurrent.substring(2,4)+hexProtectCurrent.substring(0,2)
                                }

                                var data = "BE03$hexProtectVoltage$hexProtectCurrent"
                                mServiceConnection.sendData("$data${ByteDataChange.HexStringToBytes(data)}".toByteArray())
                            }
                        }
                    })
            }
            R.id.rbBack -> {
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    "再按一次退出程序".showToast(this)
                    exitTime = System.currentTimeMillis();
                } else {
                    finish()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constant.TAG_ONE -> {
                    mMediaProjection = data?.let { mediaManager.getMediaProjection(resultCode, it) }
                    mMediaProjection?.let { CaptureImage().captureImages(this, "image", it) }
                }
            }
        }
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
        if (mFPVVideoClient != null) {
            mFPVVideoClient.stopPlayback()
        }
        if (mServiceConnection != null) {
            try {
                mServiceConnection.closeConnection()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        timer.cancel()
    }

}