package com.streamflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.data.model.MediaItem
import com.streamflix.extension.ExtensionRepository
import com.streamflix.extension.model.ExtensionConverters.toMediaItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Search ViewModel - Manages search functionality
 * 
 * Features:
 * - Debounced search queries
 * - Multi-extension search
 * - Search history
 * - Loading states
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val extensionRepository: ExtensionRepository
) : ViewModel() {

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // UI State
    sealed class SearchUiState {
        object Idle : SearchUiState()
        object Loading : SearchUiState()
        data class Success(
            val results: List<MediaItem>,
            val hasMore: Boolean = false
        ) : SearchUiState()
        data class Error(val message: String) : SearchUiState()
        object Empty : SearchUiState()
    }

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Search history
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // Current page for pagination
    private var currentPage = 1
    private var currentQuery = ""

    init {
        // Setup debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(500) // Wait 500ms after typing stops
                .filter { it.length >= 2 } // Minimum 2 characters
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    /**
     * Update search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _uiState.value = SearchUiState.Idle
        }
    }

    /**
     * Perform search across all enabled extensions
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            currentQuery = query
            currentPage = 1

            try {
                val allResults = mutableListOf<MediaItem>()
                var hasMoreResults = false

                extensionRepository.getEnabledExtensions().forEach { loadedExt ->
                    try {
                        val response = loadedExt.api.search(query, currentPage)
                        val items = response.items.map { extItem ->
                            extItem.toMediaItem(loadedExt.manifest.id, loadedExt.manifest.name)
                        }
                        allResults.addAll(items)
                        hasMoreResults = hasMoreResults || response.hasMore
                    } catch (e: Exception) {
                        Timber.e(e, "Search error in extension: ${loadedExt.manifest.name}")
                    }
                }

                // Add to search history
                addToHistory(query)

                _uiState.value = when {
                    allResults.isEmpty() -> SearchUiState.Empty
                    else -> SearchUiState.Success(allResults, hasMoreResults)
                }

            } catch (e: Exception) {
                Timber.e(e, "Search error")
                _uiState.value = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    /**
     * Load more results (pagination)
     */
    fun loadMore() {
        if (currentQuery.isEmpty()) return

        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is SearchUiState.Success) return@launch

            currentPage++

            try {
                val moreResults = mutableListOf<MediaItem>()
                var hasMoreResults = false

                extensionRepository.getEnabledExtensions().forEach { loadedExt ->
                    try {
                        val response = loadedExt.api.search(currentQuery, currentPage)
                        val items = response.items.map { extItem ->
                            extItem.toMediaItem(loadedExt.manifest.id, loadedExt.manifest.name)
                        }
                        moreResults.addAll(items)
                        hasMoreResults = hasMoreResults || response.hasMore
                    } catch (e: Exception) {
                        Timber.e(e, "Load more error in extension: ${loadedExt.manifest.name}")
                    }
                }

                _uiState.value = currentState.copy(
                    results = currentState.results + moreResults,
                    hasMore = hasMoreResults
                )

            } catch (e: Exception) {
                Timber.e(e, "Load more error")
            }
        }
    }

    /**
     * Clear search
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = SearchUiState.Idle
        currentQuery = ""
        currentPage = 1
    }

    /**
     * Add query to search history
     */
    private fun addToHistory(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(query) // Remove if exists
        current.add(0, query) // Add to front
        _searchHistory.value = current.take(10) // Keep only 10
    }

    /**
     * Clear search history
     */
    fun clearHistory() {
        _searchHistory.value = emptyList()
    }

    /**
     * Remove item from history
     */
    fun removeFromHistory(query: String) {
        _searchHistory.value = _searchHistory.value.filter { it != query }
    }
}
