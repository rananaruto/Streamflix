package com.streamflix.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streamflix.data.model.HomeSection
import com.streamflix.data.model.MediaItem
import com.streamflix.databinding.ItemHomeSectionBinding

/**
 * Home Sections Adapter - Displays horizontal scrolling sections
 */
class HomeSectionsAdapter(
    private val onItemClick: (MediaItem) -> Unit
) : ListAdapter<HomeSection, HomeSectionsAdapter.SectionViewHolder>(SectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemHomeSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SectionViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SectionViewHolder(
        private val binding: ItemHomeSectionBinding,
        private val onItemClick: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var mediaAdapter: MediaPosterAdapter

        init {
            mediaAdapter = MediaPosterAdapter(onItemClick)
            binding.rvSectionItems.apply {
                layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = mediaAdapter
            }
        }

        fun bind(section: HomeSection) {
            binding.tvSectionTitle.text = section.title
            mediaAdapter.submitList(section.items)
            
            binding.btnSeeAll.setOnClickListener {
                // Navigate to see all
            }
        }
    }

    class SectionDiffCallback : DiffUtil.ItemCallback<HomeSection>() {
        override fun areItemsTheSame(oldItem: HomeSection, newItem: HomeSection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HomeSection, newItem: HomeSection): Boolean {
            return oldItem == newItem
        }
    }
}
