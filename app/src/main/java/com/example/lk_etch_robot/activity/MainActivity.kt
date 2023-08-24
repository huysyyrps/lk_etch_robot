package com.example.lk_etch_robot.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.RequiresApi
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.dialog.MainDialog
import com.example.lk_etch_robot.dialog.SettingDialogCallBack
import com.example.lk_etch_robot.mediaprojection.CaptureImage
import com.example.lk_etch_robot.mediaprojection.MyMediaRecorder
import com.example.lk_etch_robot.mediaprojection.RecordVideo
import com.example.lk_etch_robot.util.*
import com.example.lk_etch_robot.util.BinaryChange.toBytes
import com.skydroid.fpvlibrary.serial.SerialPortConnection
import com.skydroid.fpvlibrary.serial.SerialPortControl
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val timer = Timer()
    private lateinit var mediaManager: MediaProjectionManager
    private var mMediaProjection: MediaProjection? = null

    //保护电量
    var protectElectQuantity = 0

    //强制切换备用电源电量
    var changeElectQuantity = 0

    //保护电流
    var protectCurrent = 0.0F

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

    /**s
     * 数传
     */
    private fun initData() {
        //硬件串口实例
        mServiceConnection = SerialPortConnection.newBuilder("/dev/ttyHS1", 921600).flags(1 shl 13).build()
        mServiceConnection.setDelegate(object : SerialPortConnection.Delegate {
            override fun received(bytes: ByteArray, size: Int) {
                var stringData = ByteDataChange.ByteToString(bytes)
//                LogUtil.e("TAG", stringData)
                //在设备上电后1S周期向遥控器接收端发送包含遥控器通讯帧率的数据包
                if (stringData.startsWith("B101") && stringData.length == 10) {
//                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 8)) == stringData.subSequence(8, 10)) {
                    var s = ByteArray(4)
                    s[0] = 0xA1.toByte()
                    s[1] = 0x01
                    s[2] = 0x01
                    s[3] = 0xA3.toByte()
                    mServiceConnection.sendData(s)
//                    }
                }
                if (stringData.startsWith("B103") && stringData.length == 22) {
                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 20)) == stringData.subSequence(20, 22)) {
                        //保护电量
                        protectElectQuantity = Integer.valueOf(stringData.substring(6, 8), 16)
                        //强制切换备用电源电量
                        changeElectQuantity = Integer.valueOf(stringData.substring(8, 10), 16)
                        //保护电流
                        protectCurrent = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(10, 18), 16))
                        //定时读取
                        timer.scheduleAtFixedRate(0, 1000) {
                            var s = ByteArray(4)
                            s[0] = 0xA1.toByte()
                            s[1] = 0x02
                            s[2] = 0x07
                            s[3] = 0xAA.toByte()
                            mServiceConnection.sendData(s)
                        }
                    }
                }
                if (stringData.startsWith("B102") && stringData.length == 28) {
                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 26)) == stringData.subSequence(26, 28)) {
                        //当前工作电源
                        var currentSupply = Integer.valueOf(stringData.substring(6, 8), 16)
                        //当前主电源电量
                        var mainElectQuantity = Integer.valueOf(stringData.substring(8, 10), 16)
                        //当前备用电源电量
                        var electQuantity = Integer.valueOf(stringData.substring(10, 12), 16)
                        //当前工作电流
                        var current = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(12, 20), 16))
                        //当前照明状态
                        var lightState = Integer.valueOf(stringData.substring(20, 22), 16)
                        //当前抬升位置
                        var height = Integer.valueOf(stringData.substring(22, 24), 16)
                        CoroutineScope(Dispatchers.Main).launch {
                            if(currentSupply==1){
                                tvCurrentSupply.text = "主电源"
                            }else if(currentSupply==0){
                                tvCurrentSupply.text = "备用电源"
                            }

                            tvMainElectQuantity.text = "$mainElectQuantity"
                            tvElectQuantity.text = "$electQuantity"
                            tvCurrent.text = "$current"
                            tvLightState.text = "$lightState"
//                            tvHeight.text = "$height"
                            bv_battery.BatteryView()
                            bv_battery.setProgress(height.toInt())
//                            bv_battery.setProgress(30)
                        }
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
                mediaManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                if (mMediaProjection == null) {
                    val captureIntent: Intent = mediaManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, Constant.TAG_ONE)
                } else {
                    mMediaProjection?.let {
                        CaptureImage().captureImages(this@MainActivity, "image", it)
                    }
                }
            }
            R.id.rbVideo -> {
                mediaManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                if (mMediaProjection == null) {
                    //存在录屏授权的Activity
                    val captureIntent: Intent = mediaManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, Constant.TAG_TWO)
                } else {
                    mMediaProjection?.let { RecordVideo().startRecord(mMediaProjection!!) }
                    rbVideo.visibility = View.GONE
                    rbVideoClose.visibility = View.VISIBLE
                }
            }
            R.id.rbVideoClose -> {
                RecordVideo().stopRecore()
                rbVideo.visibility = View.VISIBLE
                rbVideoClose.visibility = View.GONE
                R.string.save_success.showToast(this)
            }
            R.id.rbSetting -> {
                MainDialog().SettingDialog(this@MainActivity, protectElectQuantity, changeElectQuantity, protectCurrent,
                    object : SettingDialogCallBack {
                        override fun callBack(protectElectQuantity: String, changeElectQuantity: String, protectCurrent: String, power:String) {
                            if (mSerialPortConnection.isConnection) {
                                var hexProtectElectQuantity = ""
                                var hexChangeElectQuantity = ""
                                var hexProtectCurrent = ""
                                hexProtectElectQuantity = Integer.toHexString(protectElectQuantity.toInt())
                                hexChangeElectQuantity = Integer.toHexString(changeElectQuantity.toInt())
                                hexProtectCurrent = BinaryChange.singleToHex(protectCurrent.toFloat()).toString()
                                var data = "A10303$hexProtectElectQuantity$hexChangeElectQuantity$hexProtectCurrent$power"
                                data = "$data${ByteDataChange.HexStringToBytes(data)}"
                                var arrayData = toBytes(data)
                                mServiceConnection.sendData(
                                    arrayData
                                )
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
                Constant.TAG_TWO -> {
                    mMediaProjection = data?.let { mediaManager.getMediaProjection(resultCode, it) }
                    mMediaProjection?.let { RecordVideo().startRecord(mMediaProjection!!) }
                    rbVideo.visibility = View.GONE
                    rbVideoClose.visibility = View.VISIBLE
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