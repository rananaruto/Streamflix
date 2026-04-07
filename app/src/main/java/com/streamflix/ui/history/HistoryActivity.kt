package com.streamflix.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.streamflix.R
import com.streamflix.databinding.ActivityHistoryBinding
import com.streamflix.ui.player.PlayerActivity
import com.streamflix.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * History Activity - Watch history
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: HistoryViewModel by viewModel()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.watch_history)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { mediaItem ->
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_MEDIA_ITEM, mediaItem)
                }
                startActivity(intent)
            },
            onRemoveClick = { mediaItem ->
                viewModel.removeFromHistory(mediaItem.id)
            }
        )
        
        binding.rvHistory.apply {
            layoutManager = GridLayoutManager(this@HistoryActivity, 3)
            adapter = historyAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.clearHistory()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HistoryViewModel.HistoryUiState.Loading -> {
                            binding.shimmerLoading.visibility = View.VISIBLE
                            binding.shimmerLoading.startShimmer()
                            binding.rvHistory.visibility = View.GONE
                            binding.emptyState.visibility = View.GONE
                        }
                        is HistoryViewModel.HistoryUiState.Success -> {
                            binding.shimmerLoading.stopShimmer()
                            binding.shimmerLoading.visibility = View.GONE
                            if (state.history.isEmpty()) {
                                binding.rvHistory.visibility = View.GONE
                                binding.emptyState.visibility = View.VISIBLE
                            } else {
                                binding.rvHistory.visibility = View.VISIBLE
                                binding.emptyState.visibility = View.GONE
                                historyAdapter.submitList(state.history)
                            }
                        }
                        is HistoryViewModel.HistoryUiState.Empty -> {
                            binding.shimmerLoading.stopShimmer()
                            binding.shimmerLoading.visibility = View.GONE
                            binding.rvHistory.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
