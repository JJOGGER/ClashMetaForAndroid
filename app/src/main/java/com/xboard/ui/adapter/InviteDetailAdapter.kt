package com.xboard.ui.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.xboard.databinding.ItemInviteDetailBinding
import com.xboard.api.RetrofitClient
import com.xboard.ex.showToast
import com.xboard.model.InviteCode
import com.xboard.util.DomainFallbackManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.toString

/**
 * 邀请明细列表 Adapter
 */
class InviteDetailAdapter (val context: Context?): RecyclerView.Adapter<InviteDetailAdapter.InviteDetailViewHolder>() {

    private val inviteCodes = mutableListOf<InviteCode>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteDetailViewHolder {
        val binding =
            ItemInviteDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InviteDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InviteDetailViewHolder, position: Int) {
        holder.bind(inviteCodes[position])
    }

    override fun getItemCount(): Int = inviteCodes.size

    fun updateData(newCodes: List<InviteCode>?) {
        newCodes ?: return
        val diffResult = DiffUtil.calculateDiff(InviteCodeDiffCallback(inviteCodes, newCodes))
        inviteCodes.clear()
        inviteCodes.addAll(newCodes)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class InviteDetailViewHolder(private val binding: ItemInviteDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(code: InviteCode) {
            binding.tvInviteCode.text = "${code.code}"
            binding.tvInviteDate.text = formatDate(code.createdAt)
            binding.btnCopyCode.setOnClickListener {
                copyInviteLink(code)
            }
        }

        private fun copyInviteLink(code: InviteCode) {

            copyToClipboard("邀请链接", "${DomainFallbackManager.getCachedMainDomain()}/#/register?code=${code.code}")
        }

        private fun copyToClipboard(label: String, text: String) {
            val clipboard =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            showToast("$label 已复制到剪贴板")
        }
        private fun formatDate(dateString: String): String {
            return try {
                // 将 ISO 8601 格式转换为本地格式
                // 例如: "2025-01-01T00:00:00Z" -> "2025-01-01 00:00"
                val parts = dateString.split("T")
                if (parts.size >= 2) {
                    val date = parts[0]
                    val time = parts[1].split(":").take(2).joinToString(":")
                    "$date $time"
                } else {
                    dateString
                }
            } catch (e: Exception) {
                dateString
            }
        }
    }

    private class InviteCodeDiffCallback(
        private val oldList: List<InviteCode>,
        private val newList: List<InviteCode>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].code == newList[newItemPosition].code
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
