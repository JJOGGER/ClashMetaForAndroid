package com.xboard.base

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding
import com.github.kr328.clash.BaseActivity
import com.github.kr328.clash.BaseActivity.Event
import com.github.kr328.clash.R
import com.github.kr328.clash.core.bridge.ClashException
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.ui.DayNight
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.util.ActivityResultLifecycle
import com.xboard.ex.showToast
import com.xboard.widget.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Activity基类 - 处理沉浸式状态栏、通用生命周期等
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity(),
    CoroutineScope by MainScope(),
    Broadcasts.Observer {

    private var defer: suspend () -> Unit = {}
    private var deferRunning = false
    private val nextRequestKey = AtomicInteger(0)
    private var dayNight: DayNight = DayNight.Day
    protected var activityStarted: Boolean = false
    protected val clashRunning: Boolean
        get() = Remote.broadcasts.clashRunning
    protected val uiStore by lazy { UiStore(this) }
    protected lateinit var binding: VB
    protected val events = Channel<Event>(Channel.UNLIMITED)
    private val loadingDialog by lazy { LoadingDialog(this) }

    fun defer(operation: suspend () -> Unit) {
        this.defer = operation
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 绑定视图
        binding = getViewBinding()
        setContentView(binding.root)

        // 设置沉浸式状态栏
        setupImmersiveStatusBar()
        applyWindowInsetsToContent()

        // 初始化
        initView()
        initData()
        initListener()
    }

    suspend fun <I, O> startActivityForResult(
        contracts: ActivityResultContract<I, O>,
        input: I,
    ): O = withContext(Dispatchers.Main) {
        val requestKey = nextRequestKey.getAndIncrement().toString()

        ActivityResultLifecycle().use { lifecycle, start ->
            suspendCoroutine { c ->
                activityResultRegistry.register(requestKey, lifecycle, contracts) {
                    c.resume(it)
                }.apply { start() }.launch(input)
            }
        }
    }


    override fun onStart() {
        super.onStart()
        activityStarted = true
        Remote.broadcasts.addObserver(this)
        events.trySend(Event.ActivityStart)
    }

    override fun onStop() {
        super.onStop()
        activityStarted = false
        Remote.broadcasts.removeObserver(this)
        events.trySend(Event.ActivityStop)
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

    override fun finish() {
        if (deferRunning) return
        deferRunning = true

        launch {
            try {
                defer()
            } finally {
                withContext(NonCancellable) {
                    super.finish()
                }
            }
        }
    }
    /**
     * 设置沉浸式状态栏
     */
    private fun setupImmersiveStatusBar() {
        if (!shouldUseImmersiveStatusBar()) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = getStatusBarColor()
        window.navigationBarColor = getNavigationBarColor()

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isStatusBarDarkText()
            isAppearanceLightNavigationBars = isNavigationBarDarkText()
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }
    }

    /**
     * 处理内容区域与系统栏的间距，避免内容被状态栏/导航栏遮挡
     */
    private fun applyWindowInsetsToContent() {
        if (!shouldUseImmersiveStatusBar()) {
            return
        }

        val targetView = getWindowInsetsTargetView() ?: binding.root
        val initialPadding = intArrayOf(
            targetView.paddingLeft,
            targetView.paddingTop,
            targetView.paddingRight,
            targetView.paddingBottom
        )

        ViewCompat.setOnApplyWindowInsetsListener(targetView) { view, insets ->
            val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val topPadding = initialPadding[1] + if (shouldApplyStatusBarInsets()) statusInsets.top else 0
            val bottomPadding =
                initialPadding[3] + if (shouldApplyNavigationBarInsets()) navigationInsets.bottom else 0

            view.setPadding(
                initialPadding[0],
                topPadding,
                initialPadding[2],
                bottomPadding
            )

            insets
        }

        ViewCompat.requestApplyInsets(targetView)
    }

    /**
     * 获取状态栏颜色，子类可覆盖
     */
    protected open fun getStatusBarColor(): Int {
        return ContextCompat.getColor(this, R.color.bg_primary)
    }

    /**
     * 获取导航栏颜色，子类可覆盖
     */
    protected open fun getNavigationBarColor(): Int {
        return ContextCompat.getColor(this, R.color.bg_primary)
    }

    /**
     * 是否启用沉浸式状态栏，子类可覆盖
     */
    protected open fun shouldUseImmersiveStatusBar(): Boolean = true

    /**
     * 状态栏文字是否为深色，子类可覆盖
     */
    protected open fun isStatusBarDarkText(): Boolean = false

    /**
     * 导航栏图标是否为深色，子类可覆盖
     */
    protected open fun isNavigationBarDarkText(): Boolean = false

    /**
     * 是否需要为内容区域增加状态栏高度的内边距
     */
    protected open fun shouldApplyStatusBarInsets(): Boolean = true

    /**
     * 是否需要为内容区域增加导航栏高度的内边距
     */
    protected open fun shouldApplyNavigationBarInsets(): Boolean = false

    /**
     * 自定义需要应用 WindowInsets 的 View，默认使用根布局
     */
    protected open fun getWindowInsetsTargetView(): View? = null

    /**
     * 获取ViewBinding实例，子类必须实现
     */
    abstract fun getViewBinding(): VB

    /**
     * 初始化视图，子类可覆盖
     */
    protected open fun initView() {}

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
    protected fun showLoading(message: String) {
        loadingDialog.showLoading(message)
    }

    /**
     * 隐藏加载状态
     */
    protected fun hideLoading() {
        loadingDialog.hideLoading()
    }

    /**
     * 显示错误信息
     */
    protected open fun showError(message: String) {
        // 可由子类实现
        showToast(message)
    }

    /**
     * 显示成功信息
     */
    protected open fun showSuccess(message: String) {
        // 可由子类实现
    }


    override fun onProfileChanged() {
        events.trySend(Event.ProfileChanged)
    }

    override fun onProfileUpdateCompleted(uuid: UUID?) {
        events.trySend(Event.ProfileUpdateCompleted)
    }

    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        events.trySend(Event.ProfileUpdateFailed)
    }

    override fun onProfileLoaded() {
        events.trySend(Event.ProfileLoaded)
    }

    override fun onServiceRecreated() {
        events.trySend(Event.ServiceRecreated)
    }

    override fun onStarted() {
        events.trySend(Event.ClashStart)
    }

    override fun onStopped(cause: String?) {
        events.trySend(Event.ClashStop)

        if (cause != null && activityStarted) {

        }
    }
    enum class Event {
        ServiceRecreated,
        ActivityStart,
        ActivityStop,
        ClashStop,
        ClashStart,
        ProfileLoaded,
        ProfileChanged,
        ProfileUpdateCompleted,
        ProfileUpdateFailed,
    }
}
