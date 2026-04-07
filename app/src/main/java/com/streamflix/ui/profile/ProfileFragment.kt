package com.streamflix.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.streamflix.databinding.FragmentProfileBinding
import com.streamflix.ui.extensions.ExtensionManagerActivity
import com.streamflix.ui.history.HistoryActivity
import com.streamflix.ui.settings.SettingsActivity
import com.streamflix.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Profile Fragment - User profile and statistics
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ProfileViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            // Open edit profile
        }
        
        binding.menuSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        
        binding.menuExtensions.setOnClickListener {
            startActivity(Intent(requireContext(), ExtensionManagerActivity::class.java))
        }
        
        binding.menuHistory.setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stats.collect { stats ->
                    binding.tvFavoritesCount.text = stats.favoritesCount.toString()
                    binding.tvWatchTime.text = viewModel.formatWatchTime(stats.totalWatchTime)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
