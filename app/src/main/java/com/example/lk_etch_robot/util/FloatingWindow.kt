package com.example.lk_etch_robot.util

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import android.widget.ImageView
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.activity.FPVVideoClient
import com.example.lk_etch_robot.activity.MainActivity
import com.example.lk_etch_robot.view.BaseGLHttpVideoSurface
import com.skydroid.fpvlibrary.serial.SerialPortConnection
import com.skydroid.fpvlibrary.serial.SerialPortControl
import com.skydroid.fpvlibrary.widget.GLHttpVideoSurface
import java.io.IOException


class FloatingWindow : Service() {
    var floatView: ViewGroup? = null
    var LAYOUT_TYPE = 0
    var floatWindowLayoutParam: WindowManager.LayoutParams? = null
    var windowManager: WindowManager? = null
    var gLHttpVideoSurface: GLHttpVideoSurface? = null
    var imageView: ImageView? = null
    //视频渲染
    private lateinit var mFPVVideoClient: FPVVideoClient
    //usb连接实例
    private lateinit var mSerialPortConnection: SerialPortConnection
    //FPV控制
    private lateinit var mSerialPortControl: SerialPortControl
    private val mainHanlder = Handler(Looper.getMainLooper())
    private var isSetTime = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        val metrics: DisplayMetrics = applicationContext.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val inflater = baseContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatView = inflater.inflate(R.layout.activity_main_floating, null) as ViewGroup?
        gLHttpVideoSurface = floatView!!.findViewById(R.id.fPVVideoViewFloating)
        imageView = floatView!!.findViewById(R.id.image)
        gLHttpVideoSurface?.init()
        initVideo()

        LAYOUT_TYPE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_TOAST
        }
        floatWindowLayoutParam = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
            580,
            375,
            LAYOUT_TYPE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        floatWindowLayoutParam!!.gravity = Gravity.LEFT or Gravity.TOP
//        floatWindowLayoutParam!!.x = 0
//        floatWindowLayoutParam!!.y = 0
        windowManager!!.addView(floatView, floatWindowLayoutParam)

        imageView?.setOnClickListener(View.OnClickListener {
            stopSelf()
            windowManager!!.removeView(floatView)
            val backToHome = Intent(this@FloatingWindow, MainActivity::class.java)
            backToHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            backToHome.putExtra("tag","back")
            startActivity(backToHome)
            LogUtil.e("TAG","111")
        })

        floatView?.setOnTouchListener(object : OnTouchListener {
            val floatWindowLayoutUpdateParam: WindowManager.LayoutParams = floatWindowLayoutParam as WindowManager.LayoutParams
            var x = 0.0
            var y = 0.0
            var px = 0.0
            var py = 0.0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = floatWindowLayoutUpdateParam.x.toDouble()
                        y = floatWindowLayoutUpdateParam.y.toDouble()
                        px = event.rawX.toDouble()
                        py = event.rawY.toDouble()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        floatWindowLayoutUpdateParam.x = (x + event.rawX - px).toInt()
                        floatWindowLayoutUpdateParam.y = (y + event.rawY - py).toInt()
                        windowManager!!.updateViewLayout(floatView, floatWindowLayoutUpdateParam)
                    }
                }
                return false
            }
        })
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
                gLHttpVideoSurface?.renderI420(frame, width, height)
            }

            override fun setVideoSize(picWidth: Int, picHeight: Int) {
                gLHttpVideoSurface?.setVideoSize(picWidth, picHeight, mainHanlder)
            }

            override fun resetView() {
                gLHttpVideoSurface?.resetView(mainHanlder)
            }
        })

//        var address = getIp().connectIp
//        LogUtil.e("TAG","${address}")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
        windowManager?.removeView(floatView)
        if (mSerialPortConnection != null) {
            try {
                mSerialPortConnection?.closeConnection()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (mFPVVideoClient != null) {
            mFPVVideoClient?.stopPlayback()
        }
    }
}
