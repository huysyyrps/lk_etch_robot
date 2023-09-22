package com.example.lk_etch_robot.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import androidx.annotation.RequiresApi
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.dialog.MainDialog
import com.example.lk_etch_robot.dialog.SettingDialogCallBack
import com.example.lk_etch_robot.util.*
import com.example.lk_etch_robot.util.BinaryChange.toBytes
import com.example.lk_etch_robot.util.mediaprojection.MediaUtil
import com.skydroid.fpvlibrary.serial.SerialPortConnection
import com.skydroid.fpvlibrary.serial.SerialPortControl
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.system.exitProcess


class MainActivity : BaseActivity(), View.OnClickListener {
    //视频渲染
    private var mFPVVideoClient: FPVVideoClient? = null

    //usb连接实例
    private var mSerialPortConnection: SerialPortConnection? = null

    //FPV控制
    private lateinit var mSerialPortControl: SerialPortControl
    private var dialog: AlertDialog? = null
    //硬件串口连接实例（数传）
    private var mServiceConnection: SerialPortConnection? = null
    private val mainHanlder = Handler(Looper.getMainLooper())
    private var isSetTime = false
    private var exitTime: Long = 0
    val timer = Timer()
    private lateinit var mediaManager: MediaProjectionManager
    private var mMediaProjection: MediaProjection? = null
    var startTime = 0L
    var sendState = true
    //保护电量
    var protectElectQuantity = 0
    //强制切换备用电源电量
    var changeElectQuantity = 0
    //保护电流
    var protectCurrent = 0.0F

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action === KeyEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - exitTime > 2000) {
                "再按一次退出程序".showToast(this)
                exitTime = System.currentTimeMillis()
            } else {
                finish()
                exitProcess(0)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

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
        rbAlbum.setOnClickListener(this)

        var tag = intent.getStringExtra("tag")
        if (!tag.isNullOrEmpty()){
            fPVVideoView.init()
            initVideo()
            initData()
        }else{
            if (checkOverlayDisplayPermission()) {
                startService(Intent(this@MainActivity, FloatingWindow::class.java))
                finish()
            }else{
//                var intent = Intent()
//                intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION;
//                startActivity(intent);
                requestOverlayDisplayPermission()
            }
        }
//        if (isMyServiceRunning()) {
//            stopService(Intent(this@MainActivity, FloatingWindow::class.java))
//        }
//        if (checkOverlayDisplayPermission()) {
//            startService(Intent(this@MainActivity, FloatingWindow::class.java))
//            finish()
//        }else{
//            requestOverlayDisplayPermission()
//        }
    }

    private fun requestOverlayDisplayPermission() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle("Screen Overlay Permission Needed")
        builder.setMessage("Enable 'Display over other apps' from System Settings.")
        builder.setPositiveButton("Open Settings", DialogInterface.OnClickListener { dialog, which ->
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, RESULT_OK)
        })
        dialog = builder.create()
        dialog?.show()
    }

    private fun isMyServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (FloatingWindowGFG::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkOverlayDisplayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
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
        mSerialPortConnection?.setDelegate(object : SerialPortConnection.Delegate {
            override fun received(bytes: ByteArray, size: Int) {
                if (mFPVVideoClient != null) {
                    mFPVVideoClient?.received(bytes, size)
                }
            }

            override fun connect() {
                if (mFPVVideoClient != null) {
                    mFPVVideoClient?.startPlayback()
                }
            }
        })
        try {
            //打开串口
            mSerialPortConnection?.openConnection()
            LogUtil.e("TAG", "连接成功")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mSerialPortControl = SerialPortControl(mSerialPortConnection)
        mFPVVideoClient?.setDelegate(object : FPVVideoClient.Delegate {
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

        var address = getIp().connectIp
        LogUtil.e("TAG","${address}")
    }

    /**s
     * 数传
     */
    private fun initData() {
        //硬件串口实例
        mServiceConnection = SerialPortConnection.newBuilder("/dev/ttyHS1", 921600).flags(1 shl 13).build()
        mServiceConnection?.setDelegate(object : SerialPortConnection.Delegate {
            override fun received(bytes: ByteArray, size: Int) {
                var stringData = ByteDataChange.ByteToString(bytes)
//                LogUtil.e("TAG", stringData)
                //在设备上电后1S周期向遥控器接收端发送包含遥控器通讯帧率的数据包
                if (stringData.startsWith("B101") && stringData.length == 10) {
                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 8)) == stringData.subSequence(8, 10)) {
                        var s = ByteArray(4)
                        s[0] = 0xA1.toByte()
                        s[1] = 0x01
                        s[2] = 0x01
                        s[3] = 0xA3.toByte()
                        mServiceConnection?.sendData(s)
                    }
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
                            mServiceConnection?.sendData(s)
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
                        if (current<0){
                            current=0.0F
                        }
                        val df = DecimalFormat("0.00")

                        var currentFormat = df.format(current)
                        //当前照明状态
                        var lightState = Integer.valueOf(stringData.substring(20, 22), 16)
                        //当前抬升位置
                        var height = Integer.valueOf(stringData.substring(22, 24), 16)
                        CoroutineScope(Dispatchers.Main).launch {
                            if (currentSupply == 1) {
                                tvCurrentSupply.text = "主电源"
                            } else if (currentSupply == 0) {
                                tvCurrentSupply.text = "备用电源"
                            }

                            bvMainElectQuantity.BatteryView()
                            bvElectQuantity.BatteryView()
                            bvMainElectQuantity.setProgress(mainElectQuantity.toInt(),protectElectQuantity)
                            bvElectQuantity.setProgress(electQuantity.toInt(),protectElectQuantity)
                            tvCurrent.text = "$currentFormat"
                            if (lightState==0){
                                tvLightState.text = "关闭"
                            }else if (lightState==1){
                                tvLightState.text = "开启"
                            }
                            heightView.HeightView()
                            heightView.setHeight(height)
                            tvHeight.text = "抬升高度$height"
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
            mServiceConnection?.openConnection()
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
                        MediaUtil.captureImages(this@MainActivity, it,"main")
                    }
                }
            }
            R.id.rbVideo -> {
                mediaManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                if (mMediaProjection == null) {
                    val captureIntent: Intent = mediaManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, Constant.TAG_TWO)
                } else {
                    mMediaProjection?.let {
                        MediaUtil.startMedia(this@MainActivity, it,"main")
                        rbVideo.visibility = View.GONE
                        rbVideoClose.visibility = View.VISIBLE
                    }
                }
            }
            R.id.rbVideoClose -> {
                MediaUtil.stopMedia()
                rbVideo.visibility = View.VISIBLE
                rbVideoClose.visibility = View.GONE
            }
            R.id.rbSetting -> {
                MainDialog().SettingDialog(this@MainActivity, protectElectQuantity, changeElectQuantity, protectCurrent,
                    object : SettingDialogCallBack {
                        override fun callBack(protectElectQuantityBack: String, changeElectQuantityBack: String, protectCurrentBack: String, power: String) {
                            sendState = true
                            if (mSerialPortConnection?.isConnection == true) {
                                var hexProtectElectQuantity = ""
                                var hexChangeElectQuantity = ""
                                var hexProtectCurrent = ""
                                hexProtectElectQuantity = Integer.toHexString(protectElectQuantityBack.toInt())
                                hexChangeElectQuantity = Integer.toHexString(changeElectQuantityBack.toInt())
                                hexProtectCurrent = BinaryChange.singleToHex(protectCurrentBack.toFloat()).toString()
                                var data = "A10303$hexProtectElectQuantity$hexChangeElectQuantity$hexProtectCurrent$power"
                                data = "$data${ByteDataChange.HexStringToBytes(data)}"
                                var arrayData = toBytes(data)
                                mServiceConnection?.sendData(
                                    arrayData
                                )
                                startTime = System.currentTimeMillis()
                                Thread.sleep(1200)
                            }
                            CoroutineScope(Dispatchers.Main).launch  {
                                while (sendState){
                                    if (System.currentTimeMillis()-startTime>=1000L) {
                                        if (protectElectQuantity.toString() == protectElectQuantityBack
                                            && changeElectQuantity.toString() == changeElectQuantityBack
                                            && protectCurrent.toString() == protectCurrentBack.toFloat().toString()
                                        ) {
                                            LogUtil.e("TAG", "设置正确")
                                            sendState = false
                                        } else {
                                            LogUtil.e("TAG", "设置错误重新发起")
                                            if (mSerialPortConnection?.isConnection == true) {
                                                var hexProtectElectQuantity = ""
                                                var hexChangeElectQuantity = ""
                                                var hexProtectCurrent = ""
                                                hexProtectElectQuantity = Integer.toHexString(protectElectQuantityBack.toInt())
                                                hexChangeElectQuantity = Integer.toHexString(changeElectQuantityBack.toInt())
                                                hexProtectCurrent = BinaryChange.singleToHex(protectCurrentBack.toFloat()).toString()
                                                var data = "A10303$hexProtectElectQuantity$hexChangeElectQuantity$hexProtectCurrent$power"
                                                data = "$data${ByteDataChange.HexStringToBytes(data)}"
                                                var arrayData = toBytes(data)
                                                mServiceConnection?.sendData(
                                                    arrayData
                                                )
                                                startTime = System.currentTimeMillis()
                                                Thread.sleep(1200)
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    })
            }
            R.id.rbAlbum -> {
                MainUi.showPopupMenu(rbAlbum, "Desc", this)
            }
            R.id.rbBack -> {
//                if ((System.currentTimeMillis() - exitTime) > 2000) {
//                    "再按一次退出程序".showToast(this)
//                    exitTime = System.currentTimeMillis();
//                } else {
//                    finish()
//                }
                if (checkOverlayDisplayPermission()) {
//            startService(Intent(this@MainActivity, FloatingWindowGFG::class.java))
                    startService(Intent(this@MainActivity, FloatingWindow::class.java))
                    finish()
                }else{
                    var intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION;
                    startActivity(intent);
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
                    mMediaProjection?.let { MediaUtil.captureImages(this, it,"main") }
                }
                Constant.TAG_TWO -> {
                    mMediaProjection = data?.let { mediaManager.getMediaProjection(resultCode, it) }
                    mMediaProjection?.let { MediaUtil.startMedia(this, it,"main") }
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
                mSerialPortConnection?.closeConnection()
                mSerialPortConnection = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (mFPVVideoClient != null) {
            mFPVVideoClient?.stopPlayback()
            mFPVVideoClient = null
        }
        if (mServiceConnection != null) {
            try {
                mServiceConnection?.closeConnection()
                mServiceConnection = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        timer.cancel()
    }

}