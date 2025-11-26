package com.xboard.ui.activity

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.github.kr328.clash.databinding.ActivityThemeSettingsBinding
import com.xboard.base.BaseActivity
import com.xboard.ex.showToast
import com.xboard.storage.MMKVManager

/**
 * 主题设置页面
 */
class ThemeSettingsActivity : BaseActivity<ActivityThemeSettingsBinding>() {

    override fun getViewBinding(): ActivityThemeSettingsBinding {
        return ActivityThemeSettingsBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 返回按钮
        binding.vBack.setOnClickListener {
            finish()
        }

        // 获取当前主题模式
        val currentTheme = MMKVManager.getThemeMode()

        // 浅色主题
//        binding.itemLightTheme.setOnClickListener {
//            setTheme(0)
//        }
//
//        // 深色主题
//        binding.itemDarkTheme.setOnClickListener {
//            setTheme(1)
//        }
//
//        // 跟随系统
//        binding.itemSystemTheme.setOnClickListener {
//            setTheme(2)
//        }

        // 显示当前选择
        updateThemeSelection(currentTheme)
    }

    override fun initData() {
        // 无需加载数据
    }

//    private fun setTheme(mode: Int) {
//        // 保存主题设置
//        MMKVManager.saveThemeMode(mode)
//
//        // 应用主题
//        when (mode) {
//            0 -> {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//                showToast("已切换为浅色主题")
//            }
//            1 -> {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//                showToast("已切换为深色主题")
//            }
//            2 -> {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
//                showToast("已切换为跟随系统主题")
//            }
//        }
//
//        // 更新 UI
//        updateThemeSelection(mode)
//    }

    private fun updateThemeSelection(mode: Int) {
        // 清除所有选中状态
        binding.ivLightCheck.alpha = 0.3f
        binding.ivDarkCheck.alpha = 0.3f
        binding.ivSystemCheck.alpha = 0.3f

        // 设置当前选中状态
        when (mode) {
            0 -> binding.ivLightCheck.alpha = 1f
            1 -> binding.ivDarkCheck.alpha = 1f
            2 -> binding.ivSystemCheck.alpha = 1f
        }
    }
}
