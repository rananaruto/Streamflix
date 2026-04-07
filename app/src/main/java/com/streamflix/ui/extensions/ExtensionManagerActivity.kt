package com.streamflix.ui.extensions

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.streamflix.R
import com.streamflix.databinding.ActivityExtensionManagerBinding
import com.streamflix.ui.viewmodel.ExtensionsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Extension Manager Activity - Install and manage extensions
 */
class ExtensionManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExtensionManagerBinding
    private val viewModel: ExtensionsViewModel by viewModel()
    private lateinit var extensionsAdapter: ExtensionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtensionManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.extensions)
    }

    private fun setupRecyclerView() {
        extensionsAdapter = ExtensionsAdapter(
            onToggleEnable = { extension, enabled ->
                // Toggle extension
            },
            onDelete = { extension ->
                showDeleteDialog(extension.manifest.id)
            }
        )
        
        binding.rvExtensions.apply {
            layoutManager = LinearLayoutManager(this@ExtensionManagerActivity)
            adapter = extensionsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddExtension.setOnClickListener {
            showAddExtensionDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is ExtensionsViewModel.ExtensionsUiState.Loading -> {
                                binding.progressLoading.visibility = View.VISIBLE
                                binding.rvExtensions.visibility = View.GONE
                                binding.emptyState.visibility = View.GONE
                            }
                            is ExtensionsViewModel.ExtensionsUiState.Success -> {
                                binding.progressLoading.visibility = View.GONE
                                if (state.extensions.isEmpty()) {
                                    binding.rvExtensions.visibility = View.GONE
                                    binding.emptyState.visibility = View.VISIBLE
                                } else {
                                    binding.rvExtensions.visibility = View.VISIBLE
                                    binding.emptyState.visibility = View.GONE
                                    extensionsAdapter.submitList(state.extensions)
                                }
                            }
                            is ExtensionsViewModel.ExtensionsUiState.Error -> {
                                binding.progressLoading.visibility = View.GONE
                                Toast.makeText(this@ExtensionManagerActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                
                launch {
                    viewModel.isInstalling.collect { isInstalling ->
                        binding.fabAddExtension.isEnabled = !isInstalling
                    }
                }
            }
        }
    }

    private fun showAddExtensionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_extension, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.et_extension_url)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.install_extension)
            .setView(dialogView)
            .setPositiveButton(R.string.install_extension) { _, _ ->
                val url = editText.text?.toString()?.trim()
                if (!url.isNullOrEmpty()) {
                    viewModel.installFromGitHub(url)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog(extensionId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.extension_uninstall)
            .setMessage("Are you sure you want to uninstall this extension?")
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.uninstallExtension(extensionId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
