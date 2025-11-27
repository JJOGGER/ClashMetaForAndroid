package com.xboard.ui.activity

import com.github.kr328.clash.R
import com.github.kr328.clash.databinding.ActivityShareBinding
import com.xboard.base.BaseActivity
import com.xboard.ui.fragment.ShareFragment

/**
 *    author : jogger
 *    date   : 2025/11/27
 *    desc   :
 */
class ShareActivity : BaseActivity<ActivityShareBinding>() {
    override fun getViewBinding(): ActivityShareBinding {
       return ActivityShareBinding.inflate(layoutInflater)
    }

    override fun initView() {
        super.initView()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fl_share, ShareFragment())
            .commitAllowingStateLoss()
    }
}