package com.myproject.smartbowl.adpter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.myproject.smartbowl.fragment.CTRLFragment
import com.myproject.smartbowl.fragment.WebViewFragment

class MainViewPagerAdapter(fragment: FragmentActivity): FragmentStateAdapter(fragment) {
    var fragmentList: MutableList<Fragment> = ArrayList()

    init {
        fragmentList.add(CTRLFragment())
        fragmentList.add(WebViewFragment())
    }

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]

}