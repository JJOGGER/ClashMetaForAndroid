package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.databinding.ItemServerBinding
import com.xboard.model.Server

/**
 * 服务器列表适配器
 */
class ServerAdapter : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    private val servers = mutableListOf<Server>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val binding = ItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(servers[position])
    }

    override fun getItemCount(): Int = servers.size

    fun updateData(newServers: List<Server>) {
        val diffResult = DiffUtil.calculateDiff(ServerDiffCallback(servers, newServers))
        servers.clear()
        servers.addAll(newServers)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ServerViewHolder(private val binding: ItemServerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(server: Server) {
            binding.tvServerName.text = server.name
            binding.tvServerAddress.text = "${server.host}:${server.port}"
            binding.tvServerInfo.text = buildString {
                if (!server.cipher.isNullOrEmpty()) {
                    append("${server.cipher} • ")
                }
                if (!server.protocol.isNullOrEmpty()) {
                    append("${server.protocol} • ")
                }
                append("${server.id}")
            }
        }
    }

    private class ServerDiffCallback(
        private val oldList: List<Server>,
        private val newList: List<Server>
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
