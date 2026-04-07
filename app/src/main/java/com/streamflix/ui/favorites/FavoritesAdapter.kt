package com.streamflix.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.streamflix.R
import com.streamflix.data.model.MediaItem
import com.streamflix.databinding.ItemMediaPosterBinding

/**
 * Favorites Adapter
 */
class FavoritesAdapter(
    private val onItemClick: (MediaItem) -> Unit,
    private val onRemoveClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, FavoritesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaPosterBinding.inflate(
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
        private val binding: ItemMediaPosterBinding,
        private val onItemClick: (MediaItem) -> Unit,
        private val onRemoveClick: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaItem) {
            binding.apply {
                ivPoster.load(mediaItem.posterUrl) {
                    crossfade(true)
                    placeholder(R.drawable.shimmer_background)
                    error(R.drawable.shimmer_background)
                }
                
                tvTitle.text = mediaItem.title
                
                if (mediaItem.rating != null) {
                    badgeRating.visibility = android.view.View.VISIBLE
                    tvRating.text = String.format("%.1f", mediaItem.rating)
                } else {
                    badgeRating.visibility = android.view.View.GONE
                }
                
                btnFavorite.setImageResource(R.drawable.ic_favorite)
                
                cardPoster.setOnClickListener { onItemClick(mediaItem) }
                btnFavorite.setOnClickListener { onRemoveClick(mediaItem) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }
}
