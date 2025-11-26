//package com.xboard.ui.adapter
//
//import android.graphics.Color
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.github.kr328.clash.databinding.ItemNodeBinding
//import com.github.kr328.clash.databinding.ItemNodeSelectionBinding
//import com.xboard.model.NodeItem
//
///**
// * 节点列表适配器
// */
//class NodeAdapter(
//    private val onNodeClick: (NodeItem) -> Unit
//) : ListAdapter<NodeItem, NodeAdapter.ViewHolder>(NodeDiffCallback()) {
//
//    private var selectedPosition = -1
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        return ViewHolder(
//            ItemNodeBinding.inflate(
//                LayoutInflater.from(parent.context),
//                parent,
//                false
//            )
//        )
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(getItem(position), position)
//    }
//
//    inner class ViewHolder(private val binding: ItemNodeSelectionBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(node: NodeItem, position: Int) {
//            binding.apply {
//                // 节点名称
//                tvNodeName.text = node.name
//
//                // 节点类型
//                tvNodeType.text = node.type
//
//                // 延迟信息
//                tvDelay.text = if (node.delay > 0) "${node.delay}ms" else "未测试"
//
//                // 状态指示器
//                ivStatus.setImageResource(
//                    if (node.alive) android.R.drawable.presence_online
//                    else android.R.drawable.presence_offline
//                )
//
//                // 选择状态背景
//                if (node.selected) {
//                    root.setBackgroundColor(Color.parseColor("#1F1F1F"))
//                    ivSelected.visibility = android.view.View.VISIBLE
//                    selectedPosition = position
//                } else {
//                    root.setBackgroundColor(Color.TRANSPARENT)
//                    ivSelected.visibility = android.view.View.GONE
//                }
//
//                // 点击事件
//                root.setOnClickListener {
//                    onNodeClick(node)
//                }
//            }
//        }
//    }
//
//    /**
//     * 更新选择状态
//     */
//    fun updateSelection(nodeName: String) {
//        val newPosition = currentList.indexOfFirst { it.name == nodeName }
//        if (newPosition != -1 && newPosition != selectedPosition) {
//            if (selectedPosition >= 0) {
//                notifyItemChanged(selectedPosition)
//            }
//            notifyItemChanged(newPosition)
//        }
//    }
//
//    class NodeDiffCallback : DiffUtil.ItemCallback<NodeItem>() {
//        override fun areItemsTheSame(oldItem: NodeItem, newItem: NodeItem) =
//            oldItem.name == newItem.name
//
//        override fun areContentsTheSame(oldItem: NodeItem, newItem: NodeItem) =
//            oldItem == newItem
//    }
//}
