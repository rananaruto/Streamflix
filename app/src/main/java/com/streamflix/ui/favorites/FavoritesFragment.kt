package com.streamflix.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.streamflix.R
import com.streamflix.databinding.FragmentFavoritesBinding
import com.streamflix.ui.player.PlayerActivity
import com.streamflix.ui.viewmodel.FavoritesViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Favorites Fragment - Display and manage favorite content
 */
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FavoritesViewModel by viewModel()
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onItemClick = { mediaItem ->
                openPlayer(mediaItem)
            },
            onRemoveClick = { mediaItem ->
                viewModel.removeFromFavorites(mediaItem.id)
            }
        )
        
        binding.rvFavorites.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = favoritesAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnClearAll.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_history)
                .setMessage("Remove all favorites?")
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.clearAllFavorites()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is FavoritesViewModel.FavoritesUiState.Loading -> {
                            binding.shimmerLoading.visibility = View.VISIBLE
                            binding.shimmerLoading.startShimmer()
                            binding.rvFavorites.visibility = View.GONE
                            binding.emptyState.visibility = View.GONE
                        }
                        is FavoritesViewModel.FavoritesUiState.Success -> {
                            binding.shimmerLoading.stopShimmer()
                            binding.shimmerLoading.visibility = View.GONE
                            binding.rvFavorites.visibility = View.VISIBLE
                            binding.emptyState.visibility = View.GONE
                            favoritesAdapter.submitList(state.favorites)
                        }
                        is FavoritesViewModel.FavoritesUiState.Empty -> {
                            binding.shimmerLoading.stopShimmer()
                            binding.shimmerLoading.visibility = View.GONE
                            binding.rvFavorites.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun openPlayer(mediaItem: com.streamflix.data.model.MediaItem) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_MEDIA_ITEM, mediaItem)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
