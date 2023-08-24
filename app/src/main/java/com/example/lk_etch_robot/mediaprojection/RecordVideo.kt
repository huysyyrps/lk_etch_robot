package com.example.lk_etch_robot.mediaprojection

import android.media.projection.MediaProjection
import android.os.Environment
import com.example.lk_etch_robot.util.Constant
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class RecordVideo {
    var mediaRecord: MediaRecordService? = null
    fun startRecord(mMediaProjection: MediaProjection) {
        val dir = Environment.getExternalStorageDirectory ().absolutePath + "/" + Constant.SAVE_VIDEO_PATH + "/" //图片保存的文件夹名
        val file = File(dir)
        //如果不存在  就mkdirs()创建此文件夹
        if (!file.exists()) {
            file.mkdirs()
        }
        //将要保存的图片文件
        val mFile = File(dir + getNowDate())
       var mediaRecord = MediaRecordService(2400, 1080,
           6000000, 1, mMediaProjection, mFile.absolutePath)
        mediaRecord.start()
    }
    fun stopRecore(){
        mediaRecord?.release();
    }

    /**
     * 获取当前时间,用来给文件夹命名
     */
    private fun getNowDate(): String? {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return format.format(Date()) + ".mp4"
    }
}