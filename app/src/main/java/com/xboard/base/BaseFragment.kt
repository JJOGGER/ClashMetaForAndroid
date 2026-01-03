package com.xboard.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.github.kr328.clash.util.ActivityResultLifecycle
import com.xboard.event.ThemeChangedEvent
import com.xboard.ui.viewmodel.ThemeViewModel
import com.xboard.utils.ThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Fragment基类 - 处理通用生命周期、ViewBinding等
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private val nextRequestKey = AtomicInteger(0)
    protected lateinit var binding: VB
    private var isViewCreated = false
    
    /**
     * 主题 ViewModel，子类可以使用
     */
    protected val themeViewModel = ThemeViewModel.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 加载主题设置
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
        
        if (!isViewCreated) {
            isViewCreated = true
            initView()
            initData()
            initListener()
        }
    }
    
    override fun onStart() {
        super.onStart()
        // 注册 EventBus 监听主题变化事件
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        // 重新加载主题设置，确保从其他页面返回时主题正确
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
    }
    
    override fun onStop() {
        super.onStop()
        // 注销 EventBus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
    
    /**
     * 监听主题变化事件
     * 子类可以覆盖此方法实现自定义的主题更新逻辑
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onThemeChanged(event: ThemeChangedEvent) {
        // 重新加载主题设置
        ThemeHelper.loadThemeSettings(themeViewModel, resources)
    }

    /**
     * 获取ViewBinding实例，子类必须实现
     */
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /**
     * 初始化视图，子类可覆盖
     */
    protected open fun initView() {}

    suspend fun <I, O> startActivityForResult(
        contracts: ActivityResultContract<I, O>,
        input: I,
    ): O = withContext(Dispatchers.Main) {
        val requestKey = nextRequestKey.getAndIncrement().toString()

        ActivityResultLifecycle().use { lifecycle, start ->
            suspendCoroutine { c ->
                activity?.activityResultRegistry?.register(requestKey, lifecycle, contracts) {
                    c.resume(it)
                }.apply { start() }?.launch(input)
            }
        }
    }
    /**
     * 初始化数据，子类可覆盖
     */
    protected open fun initData() {}

    /**
     * 初始化监听器，子类可覆盖
     */
    protected open fun initListener() {}

    /**
     * 显示加载状态
     */
    protected open fun showLoading() {
        // 可由子类实现
    }

    /**
     * 隐藏加载状态
     */
    protected open fun hideLoading() {
        // 可由子类实现
    }

    /**
     * 显示错误信息
     */
    protected open fun showError(message: String) {
        // 可由子类实现
    }

    /**
     * 显示成功信息
     */
    protected open fun showSuccess(message: String) {
        // 可由子类实现
    }

    /**
     * Fragment是否可见（用于懒加载）
     */
    protected open fun onFragmentVisible() {
        // 可由子类实现
    }

    /**
     * Fragment是否不可见
     */
    protected open fun onFragmentInvisible() {
        // 可由子类实现
    }
}
