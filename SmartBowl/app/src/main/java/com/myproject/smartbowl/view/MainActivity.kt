package com.myproject.smartbowl.view

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import com.myproject.smartbowl.adpter.MainViewPagerAdapter
import com.myproject.smartbowl.R
import com.myproject.smartbowl.animation.ZoomOutPageTransformer
import com.myproject.smartbowl.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = MainViewPagerAdapter(this)
        binding.viewPager.setPageTransformer(ZoomOutPageTransformer())

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.navigationView.menu.getItem(position).isChecked = true
            }
        })

        binding.navigationView.setOnItemSelectedListener(this)




    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.CTRLFragment -> {
                binding.viewPager.currentItem = 0
                true
            }
            R.id.webViewFragment -> {
                binding.viewPager.currentItem = 1
                true
            }

            else -> {
                false
            }
        }
    }

}