package com.xboard.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.xboard.ui.fragment.AccelerateFragment
import com.xboard.ui.fragment.BuyFragment
import com.xboard.ui.fragment.MineFragment

/**
 * 主页 ViewPager2 适配器
 * 缓存3个页面：加速、购买、我的
 */
class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    companion object {
        const val PAGE_COUNT = 3
        const val PAGE_ACCELERATE = 0
        const val PAGE_BUY = 1
        const val PAGE_MINE = 2
    }

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            PAGE_ACCELERATE -> AccelerateFragment()
            PAGE_BUY -> BuyFragment()
            PAGE_MINE -> MineFragment()
            else -> AccelerateFragment()
        }
    }
}
