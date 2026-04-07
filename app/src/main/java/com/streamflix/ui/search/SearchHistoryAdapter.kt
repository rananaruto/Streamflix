package com.streamflix.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streamflix.databinding.ItemSearchHistoryBinding

/**
 * Search History Adapter
 */
class SearchHistoryAdapter(
    private val onItemClick: (String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : ListAdapter<String, SearchHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onRemoveClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSearchHistoryBinding,
        private val onItemClick: (String) -> Unit,
        private val onRemoveClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(query: String) {
            binding.tvQuery.text = query
            binding.root.setOnClickListener { onItemClick(query) }
            binding.btnRemove.setOnClickListener { onRemoveClick(query) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
