package com.example.lk_etch_robot.activity

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
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
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.dialog.MainDialog
import com.example.lk_etch_robot.dialog.SettingDialogCallBack
import com.example.lk_etch_robot.util.*
import com.example.lk_etch_robot.util.BinaryChange.toBytes
import com.example.lk_etch_robot.util.mediaprojection.MediaUtil
import com.example.lk_etch_robot.util.popup.CustomBubbleAttachPopup
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
    //管径
    var pipeDiameter = 0
    //保护电流
    var protectCurrent = 0.0F
    //主备电源切换
    var currentSupplyState = 2
    //是否显示弹窗
    var showDialog = true
    var oldHeight = 0
    var oldIndex = 0

    companion object{
        fun actionStart(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
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
        rbSmall.setOnClickListener(this)
        icBack.setOnClickListener(this)
        rbAlbum.setOnClickListener(this)
        ivMenu.setOnClickListener(this)
        rbHelp.setOnClickListener(this)
        tvDistance.setOnClickListener(this)
        frameLayout.setOnClickListener(this)
        linLiftingState.setOnClickListener(this)
        baseElectricity.setOnClickListener(this)
        linElectQuantity.setOnClickListener(this)

        val tag = intent.getStringExtra("tag")
        if (!tag.isNullOrEmpty()){
            fPVVideoView.init()
            initVideo()
            initData()
        }else{
            if (checkOverlayDisplayPermission()) {
                startService(Intent(this@MainActivity, FloatingWindow::class.java))
                finish()
            }else{
                requestOverlayDisplayPermission()
            }
        }
        DisplayUtil.hideNavBar(this)

        val lt = LayoutTransition()
        lt.disableTransitionType(LayoutTransition.DISAPPEARING)
        linHeight.layoutTransition = lt

        getWindow().getAttributes().systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE

    }

    private fun requestOverlayDisplayPermission() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle("Screen Overlay Permission Needed")
        builder.setMessage("Enable 'Display over other apps' from System Settings.")
        builder.setPositiveButton("Open Settings") { _, _ ->
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, RESULT_OK)
        }
        dialog = builder.create()
        dialog?.show()
    }

    @SuppressLint("ObsoleteSdkInt")
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
                if (!isSetTime) {
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

//        val address = getIp().connectIp
//        LogUtil.e("TAG", address)
    }

    /**
     * 数传
     */
    private fun initData() {
        //硬件串口实例921600  115200
        mServiceConnection = SerialPortConnection.newBuilder("/dev/ttyHS1", 921600).flags(1 shl 13).build()
        mServiceConnection?.setDelegate(object : SerialPortConnection.Delegate {
            @RequiresApi(Build.VERSION_CODES.O)
            @SuppressLint("SetTextI18n")
            override fun received(bytes: ByteArray, size: Int) {
                val stringData = ByteDataChange.ByteToString(bytes)
                LogUtil.e("TAG", stringData)
                //在设备上电后1S周期向遥控器接收端发送包含遥控器通讯帧率的数据包
                if (stringData.startsWith("B101") && stringData.length == 10) {
                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 8)) == stringData.subSequence(8, 10)) {
                        val s = ByteArray(4)
                        s[0] = 0xA1.toByte()
                        s[1] = 0x01
                        s[2] = 0x01
                        s[3] = 0xA3.toByte()
                        mServiceConnection?.sendData(s)
                    }
                }
                if (stringData.startsWith("B103") && stringData.length == 26) {
                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 24)) == stringData.subSequence(24, 26)) {
                        //保护电量
                        protectElectQuantity = Integer.valueOf(stringData.substring(6, 8), 16)
                        //强制切换备用电源电量
                        changeElectQuantity = Integer.valueOf(stringData.substring(8, 10), 16)
                        //管径
                        pipeDiameter = Integer.valueOf(stringData.substring(10, 14), 16)
                        //保护电流
                        protectCurrent = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(14, 22), 16))
                        //定时读取
                        timer.scheduleAtFixedRate(0, 1000) {
                            var date = "${BaseData.getYearTop()}${BaseData.getYearBotton()}${BaseData.getMonth()}${BaseData.getDay()}"
                            var data = "A10207AA${date}"
                            data = "$data${ByteDataChange.HexStringToBytes(data)}"
                            val arrayData = toBytes(data)
                            mServiceConnection?.sendData(arrayData)
                        }
                    }
                }
                if (stringData.startsWith("B102") && stringData.length == 38) {
                    LogUtil.e("TAG",stringData)
                    if (ByteDataChange.HexStringToBytes(stringData.substring(0, 36)) == stringData.subSequence(36, 38)) {
                        //当前工作电源
                        val currentSupply = Integer.valueOf(stringData.substring(6, 8), 16)
                        //当前主电源电量
                        val mainElectQuantity = Integer.valueOf(stringData.substring(8, 10), 16)
                        //当前备用电源电量
                        val electQuantity = Integer.valueOf(stringData.substring(10, 12), 16)
                        //当前工作电流
                        var current = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(12, 20), 16))
                        if (current<0){
                            current=0.0F
                        }
                        val df = DecimalFormat("0.00")
                        val currentFormat = df.format(current)
                        //当前照明状态
                        val lightState = Integer.valueOf(stringData.substring(20, 22), 16)
                        //抬升标识
                        val liftingState = Integer.valueOf(stringData.substring(22, 24), 16)
                        //当前抬升位置
                        var height = Integer.valueOf(stringData.substring(24, 26), 16)

                        //编码器数字
                        val intBits = java.lang.Long.valueOf(stringData.substring(26, 34), 16).toInt()
                        val floatValue = java.lang.Float.intBitsToFloat(intBits)
                        val distanceFormat = df.format(floatValue)
                        CoroutineScope(Dispatchers.Main).launch {
                            if(currentSupplyState!=currentSupply){
                                if (currentSupply == 1) {
                                    tvCurrentSupply.text = resources.getString(R.string.main_supply)
                                } else if (currentSupply == 0) {
                                    tvCurrentSupply.text = resources.getString(R.string.other_supply)
                                }
                                currentSupplyState = currentSupply
                            }
                            baseElectricity.baseElectricity()
                            baseElectricity.setElectricity1(mainElectQuantity)
                            verticalBattery.baseElectricity()
                            verticalBattery.setElectricity1(electQuantity)
                            tvElectQuantity.text = "$electQuantity"
                            tvCurrent.text = currentFormat
                            if (lightState==0){
                                ivLightOpen.visibility = View.GONE
                                ivLightClose.visibility = View.VISIBLE
                            }else if (lightState==1){
                                ivLightOpen.visibility = View.VISIBLE
                                ivLightClose.visibility = View.GONE
                            }
                            if (liftingState==0){
                                tvLiftingState.text = resources.getString(R.string.movement)
                            }else if (liftingState==1){
                                tvLiftingState.text = resources.getString(R.string.auto)
                            }

                            if (oldHeight!=height){
                                linHeight.visibility = View.VISIBLE
                                oldIndex = 0
                                oldHeight = height
                            }else{
                                oldIndex++
                                if (oldIndex>3){
                                    linHeight.visibility = View.GONE
                                    oldIndex = 0
                                    oldHeight = height
                                }
                            }
                            val layoutParams: ViewGroup.LayoutParams = ivHeight.layoutParams
                            var dpValue = 305*height/100
                            val density = resources.displayMetrics.density
                            val pxValue = (dpValue * density + 0.5f)
                            layoutParams.height = pxValue.toInt()
                            ivHeight.layoutParams = layoutParams
                            tvHeight.text = "抬升高度$height"
                            tvDistance.text = "距离：$distanceFormat"

                            if (showDialog){
                                if(currentSupply == 1){
                                    if(mainElectQuantity.toInt()<protectElectQuantity){
                                        MainDialog().showBatteryDialog(this@MainActivity, resources.getString(R.string.main_supply))
                                    }
                                }
                                if(currentSupply == 0){
                                    if(electQuantity.toInt()<protectElectQuantity){
                                        MainDialog().showBatteryDialog(this@MainActivity, resources.getString(R.string.other_supply))
                                    }
                                }
                                showDialog = false
                            }
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

        if (mServiceConnection?.isConnection == true){
            tvNoConnection.visibility = View.INVISIBLE
            linConnection.visibility = View.VISIBLE
        }else{
            tvNoConnection.visibility = View.VISIBLE
            linConnection.visibility = View.INVISIBLE
        }
    }

    @Deprecated("Deprecated in Java")
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
                MainDialog().SettingDialog1(this, pipeDiameter, protectElectQuantity, changeElectQuantity, protectCurrent,
                    object : SettingDialogCallBack {
                        override fun dimiss() {
                            DisplayUtil.hideNavBar(this@MainActivity)
                        }

                        override fun callBack(
                            pipeDiameter: String,
                            protectElectQuantity: String,
                            changeElectQuantity: String,
                            protectCurrent: String,
                            power: String
                        ) {
                            sendState = true
                            sendSettingData(pipeDiameter, protectElectQuantity, changeElectQuantity, protectCurrent, power)
                            CoroutineScope(Dispatchers.Main).launch {
                                while (sendState) {
                                    if (System.currentTimeMillis() - startTime >= 1000L) {
                                        if (this@MainActivity.pipeDiameter.toString() == pipeDiameter
                                            && this@MainActivity.changeElectQuantity.toString() == changeElectQuantity
                                            && this@MainActivity.changeElectQuantity.toString() == changeElectQuantity
                                            && this@MainActivity.protectCurrent.toString() == protectCurrent.toFloat().toString()
                                        ) {
                                            LogUtil.e("TAG", "设置正确")
                                            sendState = false
                                        } else {
                                            LogUtil.e("TAG", "设置错误重新发起")
                                            sendSettingData(
                                                pipeDiameter,
                                                protectElectQuantity,
                                                changeElectQuantity,
                                                protectCurrent,
                                                power
                                            )
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
            R.id.rbHelp->{
                UserInfoActivity.actionStart(this@MainActivity)
            }
            R.id.rbSmall -> {
                if (checkOverlayDisplayPermission()) {
                    startService(Intent(this@MainActivity, FloatingWindow::class.java))
                    finish()
                }else{
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                    startActivity(intent)
                }
            }
            R.id.ivMenu ->{
                radioGroup.visibility = View.VISIBLE
                ivMenu.visibility = View.GONE
            }
            R.id.frameLayout->{
                radioGroup.visibility = View.GONE
                ivMenu.visibility = View.VISIBLE
            }
            R.id.icBack->{
                finish()
            }
            R.id.tvDistance->{
                showPopup(tvDistance,"运行距离")
            }
            R.id.linLiftingState->{
                showPopup(linLiftingState,"升降模式")
            }
            R.id.baseElectricity->{
                showPopup(baseElectricity,"主电源电量")
            }
            R.id.linElectQuantity->{
                showPopup(linElectQuantity,"备用电源电量")
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun showPopup(view: View, title: String){
        com.lxj.xpopup.XPopup.Builder(this)
            .hasShadowBg(false)
            .isTouchThrough(true)
            .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
            .atView(view)
            .isCenterHorizontal(true)
            .hasShadowBg(false) // 去掉半透明背景
            .isClickThrough(true)
            .hasNavigationBar(false)//状态栏是否显示
            .animationDuration(300)//动画时长
            .isRequestFocus(false)//是否抢占焦点
            .asCustom(CustomBubbleAttachPopup(this,title))
            .show()
        DisplayUtil().hideBottomUIMenu(this)
    }

    fun sendSettingData(pipeDiameterBack: String, protectElectQuantityBack: String, changeElectQuantityBack: String, protectCurrentBack: String, power: String) {
        if (mSerialPortConnection?.isConnection == true) {
            val hexPipeDiameter: String = BinaryChange.addZeroForNum(Integer.toHexString(pipeDiameterBack.toInt()),4) .toString()
            val hexProtectElectQuantity: String = BinaryChange.addZeroForNum(Integer.toHexString(protectElectQuantityBack.toInt()),2).toString()
            val hexChangeElectQuantity: String = BinaryChange.addZeroForNum(Integer.toHexString(changeElectQuantityBack.toInt()),2).toString()
            val hexProtectCurrent: String = BinaryChange.singleToHex(protectCurrentBack.toFloat()).toString()
            var data = "A10303$hexProtectElectQuantity$hexChangeElectQuantity$hexPipeDiameter$hexProtectCurrent$power"
            data = "$data${ByteDataChange.HexStringToBytes(data)}"
            val arrayData = toBytes(data)
            mServiceConnection?.sendData(
                arrayData
            )
            startTime = System.currentTimeMillis()
            Thread.sleep(1200)
        }
    }

    @Deprecated("Deprecated in Java")
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
                    DisplayUtil.hideNavBar(this)
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
