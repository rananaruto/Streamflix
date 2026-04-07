package com.streamflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.extension.ExtensionRepository
import com.streamflix.extension.model.LoadedExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Extensions ViewModel - Manages extension installation and configuration
 */
class ExtensionsViewModel(
    private val extensionRepository: ExtensionRepository
) : ViewModel() {

    // UI State
    sealed class ExtensionsUiState {
        object Loading : ExtensionsUiState()
        data class Success(val extensions: List<LoadedExtension>) : ExtensionsUiState()
        data class Error(val message: String) : ExtensionsUiState()
    }

    private val _uiState = MutableStateFlow<ExtensionsUiState>(ExtensionsUiState.Loading)
    val uiState: StateFlow<ExtensionsUiState> = _uiState.asStateFlow()

    // Installation state
    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    init {
        loadExtensions()
    }

    /**
     * Load all extensions
     */
    fun loadExtensions() {
        viewModelScope.launch {
            _uiState.value = ExtensionsUiState.Loading
            try {
                // Load built-in extensions first
                extensionRepository.loadBuiltInExtensions()
                
                val extensions = extensionRepository.getAllExtensions()
                _uiState.value = ExtensionsUiState.Success(extensions)
            } catch (e: Exception) {
                Timber.e(e, "Error loading extensions")
                _uiState.value = ExtensionsUiState.Error(e.message ?: "Failed to load extensions")
            }
        }
    }

    /**
     * Install extension from GitHub
     */
    fun installFromGitHub(repoUrl: String) {
        viewModelScope.launch {
            _isInstalling.value = true
            try {
                extensionRepository.installFromGitHub(repoUrl)
                    .onSuccess {
                        loadExtensions()
                    }
                    .onFailure { error ->
                        _uiState.value = ExtensionsUiState.Error(
                            error.message ?: "Installation failed"
                        )
                    }
            } finally {
                _isInstalling.value = false
            }
        }
    }

    /**
     * Install extension from local file
     */
    fun installFromLocal(filePath: String) {
        viewModelScope.launch {
            _isInstalling.value = true
            try {
                extensionRepository.installFromLocal(filePath)
                    .onSuccess {
                        loadExtensions()
                    }
                    .onFailure { error ->
                        _uiState.value = ExtensionsUiState.Error(
                            error.message ?: "Installation failed"
                        )
                    }
            } finally {
                _isInstalling.value = false
            }
        }
    }

    /**
     * Uninstall extension
     */
    fun uninstallExtension(extensionId: String) {
        viewModelScope.launch {
            extensionRepository.uninstallExtension(extensionId)
            loadExtensions()
        }
    }

    /**
     * Refresh extensions
     */
    fun refresh() {
        loadExtensions()
    }

    /**
     * Clear all extensions
     */
    fun clearAllExtensions() {
        viewModelScope.launch {
            extensionRepository.clearAll()
            loadExtensions()
        }
    }
}
