package com.streamflix.ui.categories

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.streamflix.databinding.FragmentCategoriesBinding
import com.streamflix.ui.player.PlayerActivity
import com.streamflix.ui.viewmodel.CategoriesViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Categories Fragment - Browse content by category
 */
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CategoriesViewModel by viewModel()
    private lateinit var categoriesAdapter: CategoriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter { category ->
            // Navigate to category detail
        }
        
        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = categoriesAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CategoriesViewModel.CategoriesUiState.Loading -> {
                            binding.shimmerLoading.visibility = View.VISIBLE
                            binding.shimmerLoading.startShimmer()
                            binding.rvCategories.visibility = View.GONE
                        }
                        is CategoriesViewModel.CategoriesUiState.Success -> {
                            binding.shimmerLoading.stopShimmer()
                            binding.shimmerLoading.visibility = View.GONE
                            binding.rvCategories.visibility = View.VISIBLE
                            categoriesAdapter.submitList(state.categories)
                        }
                        is CategoriesViewModel.CategoriesUiState.Error -> {
                            binding.shimmerLoading.stopShimmer()
                            binding.shimmerLoading.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
