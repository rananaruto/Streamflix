package com.streamflix.ui.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.StreamFlixApplication
import com.streamflix.data.model.AppSettings
import com.streamflix.data.model.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.*

/**
 * Settings ViewModel - Manages app settings
 */
class SettingsViewModel(
    private val context: Context
) : ViewModel() {

    private val dataStore = context.dataStore

    // Settings keys
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_LIQUID_GLASS = booleanPreferencesKey("use_liquid_glass")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
    }

    // Settings State
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Load settings from DataStore
     */
    private fun loadSettings() {
        viewModelScope.launch {
            dataStore.data.map { prefs ->
                AppSettings(
                    themeMode = ThemeMode.valueOf(
                        prefs[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                    ),
                    useLiquidGlass = prefs[PreferencesKeys.USE_LIQUID_GLASS] != false,
                    autoPlayNext = prefs[PreferencesKeys.AUTO_PLAY_NEXT] != false,
                    defaultVideoQuality = prefs[PreferencesKeys.DEFAULT_VIDEO_QUALITY] ?: "Auto",
                    subtitleLanguage = prefs[PreferencesKeys.SUBTITLE_LANGUAGE] ?: "en",
                    downloadQuality = prefs[PreferencesKeys.DOWNLOAD_QUALITY] ?: "720p"
                )
            }.collect { settings ->
                _settings.value = settings
                applyTheme(settings.themeMode)
            }
        }
    }

    /**
     * Save settings to DataStore
     */
    private suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = settings.themeMode.name
            prefs[PreferencesKeys.USE_LIQUID_GLASS] = settings.useLiquidGlass
            prefs[PreferencesKeys.AUTO_PLAY_NEXT] = settings.autoPlayNext
            prefs[PreferencesKeys.DEFAULT_VIDEO_QUALITY] = settings.defaultVideoQuality
            prefs[PreferencesKeys.SUBTITLE_LANGUAGE] = settings.subtitleLanguage
            prefs[PreferencesKeys.DOWNLOAD_QUALITY] = settings.downloadQuality
        }
    }

    /**
     * Apply theme mode
     */
    private fun applyTheme(themeMode: ThemeMode) {
        val mode = when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // ==================== Setting Updates ====================

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(themeMode = mode)
            _settings.value = newSettings
            saveSettings(newSettings)
            applyTheme(mode)
        }
    }

    fun setUseLiquidGlass(use: Boolean) {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(useLiquidGlass = use)
            _settings.value = newSettings
            saveSettings(newSettings)
        }
    }

    fun setAutoPlayNext(autoPlay: Boolean) {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(autoPlayNext = autoPlay)
            _settings.value = newSettings
            saveSettings(newSettings)
        }
    }

    fun setDefaultVideoQuality(quality: String) {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(defaultVideoQuality = quality)
            _settings.value = newSettings
            saveSettings(newSettings)
        }
    }

    fun setSubtitleLanguage(language: String) {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(subtitleLanguage = language)
            _settings.value = newSettings
            saveSettings(newSettings)
        }
    }

    fun setDownloadQuality(quality: String) {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(downloadQuality = quality)
            _settings.value = newSettings
            saveSettings(newSettings)
        }
    }

    /**
     * Clear all app data
     */
    fun clearAllData() {
        viewModelScope.launch {
            // Clear DataStore
            dataStore.edit { it.clear() }
            // Reset to defaults
            _settings.value = AppSettings()
        }
    }
}
