package com.streamflix.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.streamflix.databinding.FragmentHomeBinding
import com.streamflix.ui.player.PlayerActivity
import com.streamflix.ui.settings.SettingsActivity
import com.streamflix.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Home Fragment - Main content screen
 * 
 * Features:
 * - Dynamic content sections from extensions
 * - Continue watching
 * - Pull-to-refresh
 * - Liquid Glass design
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModel()
    private lateinit var sectionsAdapter: HomeSectionsAdapter
    private lateinit var continueWatchingAdapter: MediaPosterAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupSwipeRefresh()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Continue watching adapter
        continueWatchingAdapter = MediaPosterAdapter { mediaItem ->
            openPlayer(mediaItem)
        }
        
        binding.rvContinueWatching.apply {
            layoutManager = LinearLayoutManager(
                context, 
                LinearLayoutManager.HORIZONTAL, 
                false
            )
            adapter = continueWatchingAdapter
        }

        // Sections adapter
        sectionsAdapter = HomeSectionsAdapter { mediaItem ->
            openPlayer(mediaItem)
        }
        
        binding.rvSections.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sectionsAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
        
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setupClickListeners() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        
        binding.btnAddExtensions.setOnClickListener {
            // Navigate to extensions
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is HomeViewModel.HomeUiState.Loading -> {
                                showLoading()
                            }
                            is HomeViewModel.HomeUiState.Success -> {
                                showContent(state)
                            }
                            is HomeViewModel.HomeUiState.Error -> {
                                showError(state.message)
                            }
                        }
                    }
                }
                
                launch {
                    viewModel.isRefreshing.collect { isRefreshing ->
                        binding.swipeRefresh.isRefreshing = isRefreshing
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.shimmerLoading.visibility = View.VISIBLE
        binding.shimmerLoading.startShimmer()
        binding.rvSections.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
    }

    private fun showContent(state: HomeViewModel.HomeUiState.Success) {
        binding.shimmerLoading.stopShimmer()
        binding.shimmerLoading.visibility = View.GONE
        
        if (state.sections.isEmpty()) {
            binding.rvSections.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvSections.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            sectionsAdapter.submitList(state.sections)
        }

        // Continue watching
        if (state.continueWatching.isNotEmpty()) {
            binding.sectionContinueWatching.visibility = View.VISIBLE
            continueWatchingAdapter.submitList(state.continueWatching)
        } else {
            binding.sectionContinueWatching.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.shimmerLoading.stopShimmer()
        binding.shimmerLoading.visibility = View.GONE
        // Show error snackbar or view
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
