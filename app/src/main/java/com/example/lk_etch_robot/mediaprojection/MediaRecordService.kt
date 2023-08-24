package com.example.lk_etch_robot.mediaprojection

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection

class MediaRecordService(
    private val mWidth: Int,
    private val mHeight: Int,
    private val mBitRate: Int,
    private val mDpi: Int,
    private var mMediaProjection: MediaProjection?,
    private val mDstPath: String
) : Thread() {
    private var mMediaRecorder: MediaRecorder? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    override fun run() {
        try {
            initMediaRecorder()
            //在mediarecorder.prepare()方法后调用
            mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
                TAG + "-display", mWidth, mHeight, mDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mMediaRecorder!!.surface, null, null
            )
            mMediaRecorder!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 初始化MediaRecorder
     *
     * @return
     */
    fun initMediaRecorder() {
        mMediaRecorder = MediaRecorder()
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder!!.setOutputFile(mDstPath)
        mMediaRecorder!!.setVideoSize(mWidth, mHeight)
        mMediaRecorder!!.setVideoFrameRate(FRAME_RATE)
//        mMediaRecorder!!.setVideoEncodingBitRate(mBitRate)3 * 1920 * 1080
        mMediaRecorder!!.setVideoEncodingBitRate(3 * 1920 * 1080)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        try {
            mMediaRecorder!!.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay!!.release()
            mVirtualDisplay = null
        }
        if (mMediaRecorder != null) {
            mMediaRecorder!!.setOnErrorListener(null)
            mMediaProjection!!.stop()
            mMediaRecorder!!.reset()
            mMediaRecorder!!.release()
        }
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    companion object {
        private const val TAG = "MediaRecordService"
        private const val FRAME_RATE = 30 // 60 fps
    }
}