package com.xboard.ui.dialog

import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import com.xboard.R
import com.xboard.databinding.DialogConfirmBinding
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView

/**
 * 通用确认对话框
 */
class ConfirmDialog : BaseBottomSheetDialog<DialogConfirmBinding>() {

    private var title: String = ""
    private var message: String = ""
    private var positiveButtonText: String = "确定"
    private var negativeButtonText: String = "取消"
    private var onPositiveClick: (() -> Unit)? = null
    private var onNegativeClick: (() -> Unit)? = null
    private var showNegativeButton: Boolean = true

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogConfirmBinding {
        return DialogConfirmBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.tvTitle.text = title
        binding.tvMessage.text = processHyperlinks(message, binding.tvMessage)
        binding.tvMessage.movementMethod = LinkMovementMethod.getInstance()
        binding.btnPositive.text = positiveButtonText
        binding.btnNegative.text = negativeButtonText
        
        if (!showNegativeButton) {
            binding.btnNegative.visibility = android.view.View.GONE
        }
    }

    override fun initListener() {
        binding.btnPositive.setOnClickListener {
            onPositiveClick?.invoke()
            dismiss()
        }
        
        binding.btnNegative.setOnClickListener {
            onNegativeClick?.invoke()
            dismiss()
        }
    }

    private fun processHyperlinks(text: String, textView: TextView): CharSequence {
        val spannableString = SpannableStringBuilder(text)
        val context = requireContext()
        
        // 首先处理 Markdown 链接格式: [text](url)
        val markdownUrlRegex = Regex("""\[(.*?)\]\((.*?)\)""")
        val markdownMatches = markdownUrlRegex.findAll(text).toList()
        
        // 从后往前处理避免索引偏移问题
        for (match in markdownMatches.reversed()) {
            val linkText = match.groupValues[1]
            val url = match.groupValues[2].replace("\\", "") // 移除转义字符
            val startIndex = match.range.first
            val endIndex = match.range.last + 1
            
            // 替换原始文本为显示文本
            spannableString.replace(startIndex, endIndex, linkText)
            
            // 计算新的范围
            val newEndIndex = startIndex + linkText.length
            
            // 添加点击事件
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 忽略错误
                        }
                    }
                },
                startIndex,
                newEndIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            // 添加下划线
            spannableString.setSpan(
                UnderlineSpan(),
                startIndex,
                newEndIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        // 然后处理普通的 URL 链接
        // 优化正则表达式，排除结尾的常见标点符号
        val plainUrlRegex = Regex("""https?://[^\s\])>"/]+""")
        val plainUrls = plainUrlRegex.findAll(spannableString.toString())
        
        for (result in plainUrls) {
            val url = result.value
            val startIndex = result.range.first
            val endIndex = result.range.last + 1
            
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 忽略错误
                        }
                    }
                },
                startIndex,
                endIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            spannableString.setSpan(
                UnderlineSpan(),
                startIndex,
                endIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        return spannableString
    }

    companion object {
        fun newInstance(
            title: String,
            message: String,
            positiveButtonText: String = "确定",
            negativeButtonText: String = "取消",
            showNegativeButton: Boolean = true,
            onPositiveClick: (() -> Unit)? = null,
            onNegativeClick: (() -> Unit)? = null
        ): ConfirmDialog {
            return ConfirmDialog().apply {
                this.title = title
                this.message = message
                this.positiveButtonText = positiveButtonText
                this.negativeButtonText = negativeButtonText
                this.showNegativeButton = showNegativeButton
                this.onPositiveClick = onPositiveClick
                this.onNegativeClick = onNegativeClick
            }
        }
    }
}