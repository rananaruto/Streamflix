package com.streamflix.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.data.model.MediaItem
import com.streamflix.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Favorites ViewModel - Manages favorites list
 */
class FavoritesViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    // UI State
    sealed class FavoritesUiState {
        object Loading : FavoritesUiState()
        data class Success(val favorites: List<MediaItem>) : FavoritesUiState()
        object Empty : FavoritesUiState()
    }

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    // Favorites count
    private val _favoritesCount = MutableLiveData<Int>()
    val favoritesCount: LiveData<Int> = _favoritesCount

    init {
        loadFavorites()
    }

    /**
     * Load favorites from repository
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            mediaRepository.getFavorites().collect { favorites ->
                _uiState.value = when {
                    favorites.isEmpty() -> FavoritesUiState.Empty
                    else -> FavoritesUiState.Success(favorites)
                }
            }
        }
    }

    /**
     * Remove item from favorites
     */
    fun removeFromFavorites(mediaId: String) {
        viewModelScope.launch {
            mediaRepository.removeFromFavorites(mediaId)
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

    /**
     * Clear all favorites
     */
    fun clearAllFavorites() {
        viewModelScope.launch {
            val favorites = mediaRepository.getFavoritesList()
            favorites.forEach { mediaRepository.removeFromFavorites(it.id) }
        }
    }

    /**
     * Refresh favorites
     */
    fun refresh() {
        loadFavorites()
    }
}
