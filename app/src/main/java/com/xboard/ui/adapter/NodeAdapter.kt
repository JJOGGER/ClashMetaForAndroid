package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.R
import com.github.kr328.clash.databinding.ItemNodeSelectionBinding
import com.xboard.model.Server

class NodeAdapter(
    private val onItemClick: (Server) -> Unit
) : RecyclerView.Adapter<NodeAdapter.NodeViewHolder>() {

    private val nodes = mutableListOf<Server>()
    private var selectedNodeName: String? = ""
    private val delayMap = mutableMapOf<String, Int>() // Server.name -> delay (ms)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val binding = ItemNodeSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        holder.bind(nodes[position])
    }

    override fun getItemCount(): Int = nodes.size

    fun submitList(newNodes: List<Server>, selected: String? = "") {
        val diff = DiffUtil.calculateDiff(NodeDiffCallback(nodes, newNodes))
        nodes.clear()
        nodes.addAll(newNodes)

        // 更新选中节点名称
        if (selectedNodeName != selected) {
            selectedNodeName = selected
            diff.dispatchUpdatesTo(this)
        } else {
            diff.dispatchUpdatesTo(this)
        }
    }

    fun setSelectedNode(nodeName: String?) {
        nodeName ?: return
        val oldSelectedName = selectedNodeName
        selectedNodeName = nodeName

        // 只刷新改变的两个节点
        nodes.indexOfFirst { it.name == oldSelectedName }.takeIf { it >= 0 }?.let {
            notifyItemChanged(it)
        }
        nodes.indexOfFirst { it.name == nodeName }.takeIf { it >= 0 }?.let {
            notifyItemChanged(it)
        }
    }

    /**
     * 更新延迟信息
     * @param delays Map<Server.name, delay in ms>
     */
    fun updateDelays(delays: Map<String, Int>) {
        delayMap.clear()
        delayMap.putAll(delays)
        notifyDataSetChanged() // 刷新所有项以显示延迟
    }

    inner class NodeViewHolder(
        private val binding: ItemNodeSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(node: Server) {
            binding.tvNodeName.text = node.name
            binding.tvNodeTags.text = node.name
            
            // 显示延迟信息（从 delayMap 获取）
//            val delay = delayMap[node.name] ?: 0
//            if (delay > 0) {
//                binding.tvNodeTags.text = "${node.name} • ${delay}ms"
//                // 根据延迟显示信号强度
//                // <50ms → 极好（绿色）
//                // <100ms → 很好（浅绿）
//                // <200ms → 良好（黄色）
//                // <500ms → 一般（橙色）
//                binding.ivSignal.visibility = if (delay < 100) {
//                    View.VISIBLE
//                } else {
//                    View.GONE
//                }
//            } else {
                binding.ivSignal.visibility = View.GONE
//            }
            val displayName = node.name.ifEmpty { node.name }
            if (displayName.contains("香港")) {
                binding.ivIcon.setImageResource(R.drawable.ico_hk)
            } else if (displayName.contains("日本")) {
                binding.ivIcon.setImageResource(R.drawable.ico_jp)
            } else if (displayName.contains("美国")) {
                binding.ivIcon.setImageResource(R.drawable.ico_us)
            } else if (displayName.contains("新加坡")) {
                binding.ivIcon.setImageResource(R.drawable.ico_sg)
            } else if (displayName.contains("韩国")) {
                binding.ivIcon.setImageResource(R.drawable.ico_ko)
            } else if (displayName.contains("荷兰")) {
                binding.ivIcon.setImageResource(R.drawable.ico_helan)
            } else if (displayName.contains("瑞典")) {
                binding.ivIcon.setImageResource(R.drawable.ico_ruidian)
            } else {
                binding.ivIcon.setImageResource(R.drawable.ico_unknown)
            }
            // 显示选中状态：比较节点名称与 selectedNodeName
            val isSelected = node.name == selectedNodeName
            binding.root.isSelected = isSelected
            binding.vChecked.isSelected = isSelected
            // 设置点击事件
            binding.root.setOnClickListener {
                onItemClick(node)
            }
        }
    }

    private class NodeDiffCallback(
        private val oldList: List<Server>,
        private val newList: List<Server>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].name == newList[newItemPosition].name
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
