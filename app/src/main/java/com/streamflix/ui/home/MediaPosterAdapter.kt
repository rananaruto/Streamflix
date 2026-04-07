package com.streamflix.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.streamflix.R
import com.streamflix.data.model.MediaItem
import com.streamflix.databinding.ItemMediaPosterBinding

/**
 * Media Poster Adapter - Displays movie/series posters in a grid/horizontal list
 */
class MediaPosterAdapter(
    private val onItemClick: (MediaItem) -> Unit,
    private val onFavoriteClick: ((MediaItem) -> Unit)? = null
) : ListAdapter<MediaItem, MediaPosterAdapter.PosterViewHolder>(PosterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val binding = ItemMediaPosterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PosterViewHolder(binding, onItemClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PosterViewHolder(
        private val binding: ItemMediaPosterBinding,
        private val onItemClick: (MediaItem) -> Unit,
        private val onFavoriteClick: ((MediaItem) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaItem) {
            binding.apply {
                // Load poster image
                ivPoster.load(mediaItem.posterUrl) {
                    crossfade(true)
                    placeholder(R.drawable.shimmer_background)
                    error(R.drawable.shimmer_background)
                }

                // Set title
                tvTitle.text = mediaItem.title

                // Rating
                if (mediaItem.rating != null) {
                    badgeRating.visibility = View.VISIBLE
                    tvRating.text = String.format("%.1f", mediaItem.rating)
                } else {
                    badgeRating.visibility = View.GONE
                }

                // Watch progress
                if (mediaItem.watchProgress > 0 && mediaItem.watchProgress < 0.95f) {
                    progressWatch.visibility = View.VISIBLE
                    progressWatch.progress = (mediaItem.watchProgress * 100).toInt()
                } else {
                    progressWatch.visibility = View.GONE
                }

                // Favorite button
                btnFavorite.setImageResource(
                    if (mediaItem.isFavorite) R.drawable.ic_favorite
                    else R.drawable.ic_favorite_border
                )

                // Click listeners
                cardPoster.setOnClickListener { onItemClick(mediaItem) }
                btnFavorite.setOnClickListener { onFavoriteClick?.invoke(mediaItem) }
            }
        }
    }

    class PosterDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }
}
