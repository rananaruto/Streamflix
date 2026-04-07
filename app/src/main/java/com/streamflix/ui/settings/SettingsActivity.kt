package com.streamflix.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.streamflix.R
import com.streamflix.data.model.ThemeMode
import com.streamflix.databinding.ActivitySettingsBinding
import com.streamflix.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Settings Activity - App settings and preferences
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
    }

    private fun setupClickListeners() {
        // Theme selection
        binding.settingTheme.setOnClickListener {
            showThemeDialog()
        }
        
        // Liquid Glass toggle
        binding.switchLiquidGlass.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setUseLiquidGlass(isChecked)
        }
        
        // Auto-play toggle
        binding.switchAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoPlayNext(isChecked)
        }
        
        // Video quality
        binding.settingVideoQuality.setOnClickListener {
            showQualityDialog()
        }
        
        // Subtitle language
        binding.settingSubtitleLanguage.setOnClickListener {
            showSubtitleLanguageDialog()
        }
        
        // Clear data
        binding.settingClearData.setOnClickListener {
            showClearDataDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    // Update UI
                    binding.tvThemeValue.text = when (settings.themeMode) {
                        ThemeMode.LIGHT -> getString(R.string.theme_light)
                        ThemeMode.DARK -> getString(R.string.theme_dark)
                        ThemeMode.SYSTEM -> getString(R.string.theme_system)
                    }
                    
                    binding.switchLiquidGlass.isChecked = settings.useLiquidGlass
                    binding.switchAutoPlay.isChecked = settings.autoPlayNext
                    binding.tvVideoQualityValue.text = settings.defaultVideoQuality
                    binding.tvSubtitleLanguageValue.text = settings.subtitleLanguage
                }
            }
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.theme)
            .setItems(themes) { _, which ->
                val mode = when (which) {
                    0 -> ThemeMode.LIGHT
                    1 -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
                viewModel.setThemeMode(mode)
            }
            .show()
    }

    private fun showQualityDialog() {
        val qualities = arrayOf("Auto", "1080p", "720p", "480p", "360p")
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.default_quality)
            .setItems(qualities) { _, which ->
                viewModel.setDefaultVideoQuality(qualities[which])
            }
            .show()
    }

    private fun showSubtitleLanguageDialog() {
        val languages = arrayOf("English", "Spanish", "French", "German", "Auto")
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.subtitle_language)
            .setItems(languages) { _, which ->
                viewModel.setSubtitleLanguage(languages[which])
            }
            .show()
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_data)
            .setMessage(R.string.clear_data_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.clearAllData()
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
