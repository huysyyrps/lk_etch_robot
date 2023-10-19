package com.example.lk_etch_robot.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.lk_etch_robot.R
import kotlinx.android.synthetic.main.activity_guide.*
import kotlinx.android.synthetic.main.fragment_first.*
import kotlinx.android.synthetic.main.fragment_second.*

class SecondFragment(var tabName:String?="") : Fragment(),View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_second, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        linTwoWheelsRobot.setOnClickListener(this)
        linFourWheelsRobot.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.linTwoWheelsRobot->{
                requireActivity().viewpager.currentItem = 2
            }
            R.id.linFourWheelsRobot->{
                requireActivity().viewpager.currentItem = 2
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}