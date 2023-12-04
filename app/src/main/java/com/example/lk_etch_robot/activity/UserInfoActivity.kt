package com.example.lk_etch_robot.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.util.BaseActivity
import com.example.lk_etch_robot.util.DisplayUtil
import com.example.lk_etch_robot.util.StatusBarUtils
import kotlinx.android.synthetic.main.activity_user_info.*
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream


class UserInfoActivity : BaseActivity() {
    companion object {
        fun actionStart(context: Context) {
            val intent = Intent(context, UserInfoActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        StatusBarUtils.setWindowStatusBarColor(this, R.color.white)
        DisplayUtil.hideNavBar(this@UserInfoActivity)
        setContentView(R.layout.activity_user_info)
        webView.loadUrl("file:///android_asset/index.html")
        ivUserInfoBack.setOnClickListener {
            finish()
        }
    }
}