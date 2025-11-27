package com.xboard.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import com.github.kr328.clash.R
import com.github.kr328.clash.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.xboard.base.BaseActivity
import com.xboard.ui.adapter.MainPagerAdapter

/**
 * 主页（首页）
 * 使用 TabLayout + ViewPager2 实现3个页面的缓存
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var pagerAdapter: MainPagerAdapter

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher =
                registerForActivityResult(
                    RequestPermission()
                ) { isGranted: Boolean ->
                }
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setupViewPager()
        setupTabLayout()
        setupTestButton()
    }

    override fun initData() {
        // 默认显示加速页面
        binding.viewPager.currentItem = 0
    }

    /**
     * 设置 ViewPager2
     */
    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.apply {
            adapter = pagerAdapter
            // 设置离屏页面限制为3，实现缓存所有页面
            offscreenPageLimit = 3
            // 禁用预加载，提高性能
            isUserInputEnabled = true
        }
    }

    /**
     * 设置 TabLayout
     */
    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.setIcon(when (position) {
                MainPagerAdapter.PAGE_ACCELERATE -> R.drawable.ic_accelerate
                MainPagerAdapter.PAGE_BUY ->  R.drawable.ic_buy
                MainPagerAdapter.PAGE_MINE -> R.drawable.ic_mine
                else -> R.drawable.ic_accelerate
            })
        }.attach()
    }

    /**
     * 设置测试按钮
     *
     * 点击按钮跳转到 Clash 的 MainActivity（点此启动所在的页面）
     */
    private fun setupTestButton() {
        binding.btnTestLaunch.setOnClickListener {
            // 跳转到 Clash 的 MainActivity
            val intent = Intent(this, com.github.kr328.clash.MainActivity::class.java)
            startActivity(intent)
        }
    }
}
