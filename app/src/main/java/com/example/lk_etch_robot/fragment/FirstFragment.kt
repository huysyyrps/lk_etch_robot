package com.example.lk_etch_robot.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.lk_etch_robot.R
import kotlinx.android.synthetic.main.fragment_first.*

class FirstFragment(var tabName:String?="") : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Glide.with(requireActivity())
//            .asGif()
//            .load("file:///android_asset/first_step.gif")
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(ivFirst)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}