package com.example.lk_etch_robot.activity

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.service.autofill.SaveCallback
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.lk_etch_robot.R
import com.example.lk_etch_robot.dialog.MainDialog
import com.example.lk_etch_robot.dialog.SaveDialogCallBack
import com.example.lk_etch_robot.fragment.FirstFragment
import com.example.lk_etch_robot.fragment.SecondFragment
import com.example.lk_etch_robot.fragment.ThirdFragment
import com.example.lk_etch_robot.util.DisplayUtil
import com.example.lk_etch_robot.util.StatusBarUtils
import com.example.lk_etch_robot.util.TransFormer
import kotlinx.android.synthetic.main.activity_guide.*
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException


class GuideActivity : AppCompatActivity(), View.OnClickListener {
    val childFragments = arrayListOf<Fragment>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        StatusBarUtils.setWindowStatusBarColor(this, R.color.white)
        setContentView(R.layout.activity_guide)
        tvJump.setOnClickListener(this)
        initViewPage()
        DisplayUtil.hideNavBar(this)
    }

    private fun initViewPage() {
        childFragments.add(FirstFragment("第一个页面"))
        childFragments.add(SecondFragment("第二个页面"))
        childFragments.add(ThirdFragment("第三个页面"))
//        childFragments.add(FourFragment("第四个页面"))
//        //设置adapter
        viewpager.adapter = fragmentAdapter
        //设置viewpage的滑动方向
        viewpager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        //禁止滑动
        // viewpager.isUserInputEnabled = false
        //设置显示的页面，0：是第一页
        //viewpager.currentItem = 1
        //设置缓存页
        viewpager.offscreenPageLimit = childFragments.size
        //设置viewPage2切换效果
        //viewpager.setPageTransformer(TransFormer())
//
//        //一屏多个fragment
////        val recyclerView:RecyclerView = viewpager.getChildAt(0) as RecyclerView
////        val padding = resources.getDimensionPixelOffset(R.dimen.app_icon_size) + resources.getDimensionPixelOffset(R.dimen.app_icon_size)
////        recyclerView.setPadding(padding, 0, padding, 0)
////        recyclerView.clipToPadding = false
//
        //设置滑动时fragment之间的间距
        // viewpager.setPageTransformer( MarginPageTransformer(100))
        //同时设置多个动画
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(100))
        compositePageTransformer.addTransformer(TransFormer())
        viewpager.setPageTransformer(compositePageTransformer)

        //设置选中事件
        viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) {
                    tvFirstTitle.text = "设备开机与操作"
                    tvStep.text = "1/3"
                    tvJump.text = resources.getString(R.string.jump)
                } else if (position == 1) {
                    tvFirstTitle.text = "选择机器人型号"
                    tvStep.text = "2/3"
                    tvJump.text = resources.getString(R.string.jump)
                } else if (position == 2) {
                    tvFirstTitle.text = "测厚仪与遥控器连接"
                    tvStep.text = "3/3"
                    tvJump.text = resources.getString(R.string.small)
                } else if (position == 3) {
                    tvFirstTitle.text = "检查链接状态"
                    tvStep.text = "4/4"
                    tvJump.text = "最小化"
                }
            }

        })
    }

    /**
     * viewPager adapter
     */
    private val fragmentAdapter: FragmentStateAdapter by lazy {
        object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return childFragments.size
            }

            override fun createFragment(position: Int): Fragment {
                return childFragments[position]
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tvJump -> {
//                viewpager.currentItem = 3
                var wifiSatae =this.isWifiApOpen()
                if (!wifiSatae) {
//                    var intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
//                    startActivity(intent)
//                    "请下滑菜单栏开启热点".showToast(this)
                    MainDialog().showWifiDialog(this, object : SaveDialogCallBack {
                        override fun callBack(name: String) {
                            val intent = Intent()
                            intent.addCategory(Intent.CATEGORY_DEFAULT)
                            intent.action = "android.intent.action.MAIN"
                            val cn = ComponentName(
                                "com.android.settings",
                                "com.android.settings.Settings\$TetherSettingsActivity"
                            )
                            intent.component = cn
                            startActivity(intent)
                        }

                    })
                }else{
                    MainActivity.actionStart(this)
                    finish()
                }
            }
        }
    }

    fun Activity.isWifiApOpen(): Boolean {
        try {
            val manager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
            //通过放射获取 getWifiApState()方法
            val method = manager.javaClass.getDeclaredMethod("getWifiApState")
            //调用getWifiApState() ，获取返回值
            val state = method.invoke(manager) as Int
            //通过放射获取 WIFI_AP的开启状态属性
            val field: Field = manager.javaClass.getDeclaredField("WIFI_AP_STATE_ENABLED")
            //获取属性值
            val value = field.get(manager) as Int
            //判断是否开启
            return state == value
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }
        return false
    }
}