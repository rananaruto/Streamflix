package com.streamflix.ui.extensions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.streamflix.R
import com.streamflix.databinding.ItemExtensionBinding
import com.streamflix.extension.model.LoadedExtension

/**
 * Extensions Adapter
 */
class ExtensionsAdapter(
    private val onToggleEnable: (LoadedExtension, Boolean) -> Unit,
    private val onDelete: (LoadedExtension) -> Unit
) : ListAdapter<LoadedExtension, ExtensionsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExtensionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onToggleEnable, onDelete)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemExtensionBinding,
        private val onToggleEnable: (LoadedExtension, Boolean) -> Unit,
        private val onDelete: (LoadedExtension) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(extension: LoadedExtension) {
            binding.apply {
                tvName.text = extension.manifest.name
                tvVersion.text = "v${extension.manifest.version} by ${extension.manifest.author}"
                tvDescription.text = extension.manifest.description
                
                ivIcon.load(extension.manifest.icon) {
                    crossfade(true)
                    placeholder(R.drawable.ic_extension)
                    error(R.drawable.ic_extension)
                }
                
                switchEnable.isChecked = extension.isEnabled
                switchEnable.setOnCheckedChangeListener { _, isChecked ->
                    onToggleEnable(extension, isChecked)
                }
                
                btnDelete.setOnClickListener { onDelete(extension) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LoadedExtension>() {
        override fun areItemsTheSame(oldItem: LoadedExtension, newItem: LoadedExtension): Boolean {
            return oldItem.manifest.id == newItem.manifest.id
        }

        override fun areContentsTheSame(oldItem: LoadedExtension, newItem: LoadedExtension): Boolean {
            return oldItem == newItem
        }
    }
}
