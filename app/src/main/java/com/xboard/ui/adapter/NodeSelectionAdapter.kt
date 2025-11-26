package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemNodeSelectionBinding
import com.xboard.model.ServerGroupNode

class NodeSelectionAdapter(
    private val onItemClick: (ServerGroupNode) -> Unit
) : RecyclerView.Adapter<NodeSelectionAdapter.NodeViewHolder>() {

    private val nodes = mutableListOf<ServerGroupNode>()

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

    fun submitList(newNodes: List<ServerGroupNode>) {
        val diff = DiffUtil.calculateDiff(NodeDiffCallback(nodes, newNodes))
        nodes.clear()
        nodes.addAll(newNodes)
        diff.dispatchUpdatesTo(this)
    }

    inner class NodeViewHolder(
        private val binding: ItemNodeSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(node: ServerGroupNode) {
            binding.tvNodeName.text = node.name
            binding.tvNodeLatency.text = "42ms"
            binding.tvNodeTags.text = if (node.tags.isNotEmpty()) {
                node.tags.joinToString(" · ")
            } else {
                ""
            }
            binding.tvNodeStatus.text = if (node.isOnline == 1) "在线" else "离线"

            binding.root.setOnClickListener {
                onItemClick(node)
            }
        }
    }

    private class NodeDiffCallback(
        private val oldList: List<ServerGroupNode>,
        private val newList: List<ServerGroupNode>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
