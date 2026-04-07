package com.streamflix.ui.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streamflix.R
import com.streamflix.databinding.ItemCategoryBinding
import com.streamflix.ui.viewmodel.CategoriesViewModel

/**
 * Categories Adapter
 */
class CategoriesAdapter(
    private val onCategoryClick: (CategoriesViewModel.Category) -> Unit
) : ListAdapter<CategoriesViewModel.Category, CategoriesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onCategoryClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCategoryBinding,
        private val onCategoryClick: (CategoriesViewModel.Category) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoriesViewModel.Category) {
            binding.tvCategoryName.text = category.name
            
            // Set gradient based on category
            val gradientRes = when (category.id) {
                "movies" -> R.drawable.category_gradient_movies
                "series" -> R.drawable.category_gradient_series
                "anime" -> R.drawable.category_gradient_anime
                "documentary" -> R.drawable.category_gradient_documentary
                else -> R.drawable.category_gradient_action
            }
            binding.categoryGradient.setBackgroundResource(gradientRes)
            
            // Set icon
            val iconRes = when (category.id) {
                "movies" -> R.drawable.ic_movie
                "series" -> R.drawable.ic_tv
                "anime" -> R.drawable.ic_animation
                "documentary" -> R.drawable.ic_documentary
                else -> R.drawable.ic_category
            }
            binding.ivCategoryIcon.setImageResource(iconRes)
            
            binding.cardCategory.setOnClickListener { onCategoryClick(category) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoriesViewModel.Category>() {
        override fun areItemsTheSame(
            oldItem: CategoriesViewModel.Category,
            newItem: CategoriesViewModel.Category
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: CategoriesViewModel.Category,
            newItem: CategoriesViewModel.Category
        ): Boolean {
            return oldItem == newItem
        }
    }
}
