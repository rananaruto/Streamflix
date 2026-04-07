package com.streamflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Profile ViewModel - Manages user profile data and statistics
 */
class ProfileViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    // Profile statistics
    data class ProfileStats(
        val favoritesCount: Int = 0,
        val watchHistoryCount: Int = 0,
        val totalWatchTime: Long = 0,
        val totalMediaCount: Int = 0
    )

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    init {
        loadStats()
    }

    /**
     * Load profile statistics
     */
    private fun loadStats() {
        viewModelScope.launch {
            combine(
                mediaRepository.getFavoritesCount(),
                mediaRepository.getTotalWatchTime(),
                mediaRepository.getTotalCount()
            ) { favorites, watchTime, totalCount ->
                ProfileStats(
                    favoritesCount = favorites,
                    watchHistoryCount = 0, // Will be updated separately
                    totalWatchTime = watchTime ?: 0,
                    totalMediaCount = totalCount
                )
            }.collect { stats ->
                _stats.value = stats
            }
        }
    }

    /**
     * Format watch time for display
     */
    fun formatWatchTime(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 24 -> {
                val days = hours / 24
                val remainingHours = hours % 24
                "${days}d ${remainingHours}h"
            }
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    /**
     * Refresh statistics
     */
    fun refresh() {
        loadStats()
    }
}
