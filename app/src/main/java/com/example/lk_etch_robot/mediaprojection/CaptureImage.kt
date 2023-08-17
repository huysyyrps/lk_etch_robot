package com.example.lk_etch_robot.mediaprojection

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.example.lk_etch_robot.MyApplication
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.activity.MainActivity
import com.example.lk_etch_robot.util.BitmapSave
import com.example.lk_etch_robot.util.Constant
import com.example.lk_etch_robot.util.showToast
import java.nio.ByteBuffer

class CaptureImage {
    private var mMediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var isGot: Boolean = false
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant")
    fun captureImages(activity: MainActivity, tag:String, mMediaProjection:MediaProjection){
        Handler(Looper.myLooper()!!).postDelayed(object : Runnable {
            override fun run() {
                //配置ImageReader
                val dm = activity.resources.displayMetrics
                imageReader = ImageReader.newInstance(
                    dm.widthPixels, dm.heightPixels,
                    PixelFormat.RGBA_8888, 1
                ).apply {
                    setOnImageAvailableListener({
                        //这里页面帧发生变化时就会回调一次，我们只需要获取一张图片，加个标记位，避免重复
                        if (!isGot) {
                            isGot = true
                            //这里就可以保存图片了
                            savePicTask(it,activity,tag)
                        }
                    }, null)

                    //把内容投射到ImageReader 的surface
                    mMediaProjection?.createVirtualDisplay(
                        "image", dm.widthPixels, dm.heightPixels, dm.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null
                    )
                }
            }
        }, 400)
    }

    /**
     * 保存图片
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun savePicTask(reader: ImageReader, activity: MainActivity, tag: String) {
        var image: Image? = null
        try {
            //获取捕获的照片数据
            image = reader.acquireLatestImage()
            val width = image.width
            val height = image.height
            //拿到所有的 Plane 数组
            val planes = image.planes
            val plane = planes[0]

            val buffer: ByteBuffer = plane.buffer
            //相邻像素样本之间的距离，因为RGBA，所以间距是4个字节
            val pixelStride = plane.pixelStride
            //每行的宽度
            val rowStride = plane.rowStride
            //因为内存对齐问题，每个buffer 宽度不同，所以通过pixelStride * width 得到大概的宽度，
            //然后通过 rowStride 去减，得到大概的内存偏移量，不过一般都是对齐的。
            val rowPadding = rowStride - pixelStride * width
            // 创建具体的bitmap大小，由于rowPadding是RGBA 4个通道的，所以也要除以pixelStride，得到实际的宽
            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            if (tag=="image"){
                var saveTag = BitmapSave.saveBitmap(Constant.SAVE_IMAGE_PATH,activity,bitmap)
                if (saveTag)
                    MyApplication.context.resources.getString(R.string.save_success).showToast(activity)
                else
                    MyApplication.context.resources.getString(R.string.save_faile).showToast(activity)
            }
            mMediaProjection?.stop()
            imageReader?.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            //记得关闭 image
            try {
                image?.close()
            } catch (e: Exception) {
            }
        }
    }
}