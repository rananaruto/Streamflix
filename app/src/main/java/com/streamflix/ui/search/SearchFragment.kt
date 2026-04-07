package com.streamflix.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.streamflix.databinding.FragmentSearchBinding
import com.streamflix.ui.player.PlayerActivity
import com.streamflix.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Search Fragment - Search functionality with debounced queries
 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModel()
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupSearchInput()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Search results
        searchResultsAdapter = SearchResultsAdapter { mediaItem ->
            openPlayer(mediaItem)
        }
        
        binding.rvSearchResults.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = searchResultsAdapter
        }

        // Search history
        searchHistoryAdapter = SearchHistoryAdapter(
            onItemClick = { query ->
                binding.etSearch.setText(query)
                viewModel.setSearchQuery(query)
            },
            onRemoveClick = { query ->
                viewModel.removeFromHistory(query)
            }
        )
        
        binding.rvSearchHistory.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = searchHistoryAdapter
        }
    }

    private fun setupSearchInput() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupClickListeners() {
        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is SearchViewModel.SearchUiState.Idle -> {
                                showHistory()
                            }
                            is SearchViewModel.SearchUiState.Loading -> {
                                showLoading()
                            }
                            is SearchViewModel.SearchUiState.Success -> {
                                showResults(state)
                            }
                            is SearchViewModel.SearchUiState.Error -> {
                                showError(state.message)
                            }
                            is SearchViewModel.SearchUiState.Empty -> {
                                showEmpty()
                            }
                        }
                    }
                }
                
                launch {
                    viewModel.searchHistory.collect { history ->
                        searchHistoryAdapter.submitList(history)
                    }
                }
            }
        }
    }

    private fun showHistory() {
        binding.searchHistoryContainer.visibility = View.VISIBLE
        binding.rvSearchResults.visibility = View.GONE
        binding.shimmerLoading.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
    }

    private fun showLoading() {
        binding.searchHistoryContainer.visibility = View.GONE
        binding.rvSearchResults.visibility = View.GONE
        binding.shimmerLoading.visibility = View.VISIBLE
        binding.shimmerLoading.startShimmer()
        binding.emptyState.visibility = View.GONE
    }

    private fun showResults(state: SearchViewModel.SearchUiState.Success) {
        binding.shimmerLoading.stopShimmer()
        binding.shimmerLoading.visibility = View.GONE
        binding.searchHistoryContainer.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.rvSearchResults.visibility = View.VISIBLE
        searchResultsAdapter.submitList(state.results)
    }

    private fun showEmpty() {
        binding.shimmerLoading.stopShimmer()
        binding.shimmerLoading.visibility = View.GONE
        binding.searchHistoryContainer.visibility = View.GONE
        binding.rvSearchResults.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.shimmerLoading.stopShimmer()
        binding.shimmerLoading.visibility = View.GONE
        // Show error
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
