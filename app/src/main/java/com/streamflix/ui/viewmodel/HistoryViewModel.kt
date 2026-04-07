package com.streamflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.data.model.MediaItem
import com.streamflix.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * History ViewModel - Manages watch history
 */
class HistoryViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    // UI State
    sealed class HistoryUiState {
        object Loading : HistoryUiState()
        data class Success(
            val history: List<MediaItem>,
            val continueWatching: List<MediaItem>
        ) : HistoryUiState()
        object Empty : HistoryUiState()
    }

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // Total watch time
    private val _totalWatchTime = MutableStateFlow<Long>(0)
    val totalWatchTime: StateFlow<Long> = _totalWatchTime.asStateFlow()

    init {
        loadHistory()
        observeTotalWatchTime()
    }

    /**
     * Load watch history
     */
    private fun loadHistory() {
        viewModelScope.launch {
            mediaRepository.getWatchHistory().collect { history ->
                val continueWatching = mediaRepository.getContinueWatching().first()
                
                _uiState.value = when {
                    history.isEmpty() -> HistoryUiState.Empty
                    else -> HistoryUiState.Success(history, continueWatching)
                }
            }
        }
    }

    /**
     * Observe total watch time
     */
    private fun observeTotalWatchTime() {
        viewModelScope.launch {
            mediaRepository.getTotalWatchTime().collect { time ->
                _totalWatchTime.value = time ?: 0
            }
        }
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        viewModelScope.launch {
            mediaRepository.clearWatchHistory()
        }
    }

    /**
     * Remove item from history
     */
    fun removeFromHistory(mediaId: String) {
        viewModelScope.launch {
            // Reset watch progress to remove from history
            mediaRepository.updateWatchProgress(mediaId, 0, 0)
        }
    }

    /**
     * Refresh history
     */
    fun refresh() {
        loadHistory()
    }

    /**
     * Format watch time for display
     */
    fun formatWatchTime(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }
}
