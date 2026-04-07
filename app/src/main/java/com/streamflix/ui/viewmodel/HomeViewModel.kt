package com.streamflix.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.data.model.HomeSection
import com.streamflix.data.model.MediaItem
import com.streamflix.data.repository.MediaRepository
import com.streamflix.extension.ExtensionRepository
import com.streamflix.extension.model.ExtensionConverters.toMediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Home ViewModel - Manages home screen data and state
 * 
 * Handles:
 * - Loading content from extensions
 * - Organizing content into sections
 * - Managing loading states
 * - Error handling
 */
class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val extensionRepository: ExtensionRepository
) : ViewModel() {

    // UI State
    sealed class HomeUiState {
        object Loading : HomeUiState()
        data class Success(
            val sections: List<HomeSection>,
            val continueWatching: List<MediaItem>,
            val favorites: List<MediaItem>
        ) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Continue watching
    private val _continueWatching = MutableLiveData<List<MediaItem>>()
    val continueWatching: LiveData<List<MediaItem>> = _continueWatching

    init {
        loadHomeData()
        observeContinueWatching()
    }

    /**
     * Load all home screen data
     */
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            try {
                val sections = mutableListOf<HomeSection>()
                val extensions = extensionRepository.getEnabledExtensions()

                if (extensions.isEmpty()) {
                    // Load built-in extensions if none enabled
                    extensionRepository.loadBuiltInExtensions()
                }

                // Fetch content from each enabled extension
                extensionRepository.getEnabledExtensions().forEach { loadedExt ->
                    try {
                        val homeResponse = loadedExt.api.getMainPage()
                        
                        homeResponse.sections.forEach { extSection ->
                            val mediaItems = extSection.items.map { extItem ->
                                extItem.toMediaItem(loadedExt.manifest.id, loadedExt.manifest.name)
                            }
                            
                            sections.add(
                                HomeSection(
                                    id = "${loadedExt.manifest.id}_${extSection.id}",
                                    title = extSection.title,
                                    items = mediaItems,
                                    type = when (extSection.layout) {
                                        "horizontal" -> com.streamflix.data.model.SectionType.HORIZONTAL_LIST
                                        "carousel" -> com.streamflix.data.model.SectionType.CAROUSEL
                                        else -> com.streamflix.data.model.SectionType.GRID
                                    }
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error loading from extension: ${loadedExt.manifest.name}")
                    }
                }

                // Cache the loaded items
                sections.forEach { section ->
                    mediaRepository.cacheMediaItems(section.items)
                }

                val continueWatchingItems = mediaRepository.getContinueWatching()
                val favoritesItems = mediaRepository.getFavorites()

                _uiState.value = HomeUiState.Success(
                    sections = sections,
                    continueWatching = continueWatchingItems.first(),
                    favorites = favoritesItems.first()
                )

            } catch (e: Exception) {
                Timber.e(e, "Error loading home data")
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Refresh home data (pull-to-refresh)
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadHomeData()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Observe continue watching items
     */
    private fun observeContinueWatching() {
        viewModelScope.launch {
            mediaRepository.getContinueWatching().collect { items ->
                _continueWatching.value = items
            }
        }
    }

    /**
     * Toggle favorite status
     */
    fun toggleFavorite(mediaItem: MediaItem) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(mediaItem)
        }
    }
}
