package com.streamflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.data.model.MediaItem
import com.streamflix.data.model.MediaType
import com.streamflix.data.repository.MediaRepository
import com.streamflix.extension.ExtensionRepository
import com.streamflix.extension.model.ExtensionConverters.toMediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Categories ViewModel - Manages category browsing
 */
class CategoriesViewModel(
    private val extensionRepository: ExtensionRepository
) : ViewModel() {

    // Category data
    data class Category(
        val id: String,
        val name: String,
        val icon: String? = null
    )

    // UI State
    sealed class CategoriesUiState {
        object Loading : CategoriesUiState()
        data class Success(
            val categories: List<Category>,
            val itemsByCategory: Map<String, List<MediaItem>>
        ) : CategoriesUiState()
        data class Error(val message: String) : CategoriesUiState()
    }

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * Load categories from extensions
     */
    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = CategoriesUiState.Loading
            
            try {
                val categories = mutableListOf<Category>()
                val itemsByCategory = mutableMapOf<String, List<MediaItem>>()

                extensionRepository.getEnabledExtensions().forEach { loadedExt ->
                    loadedExt.manifest.categories.forEach { categoryName ->
                        val categoryId = "${loadedExt.manifest.id}_$categoryName"
                        categories.add(
                            Category(
                                id = categoryId,
                                name = categoryName,
                                icon = null
                            )
                        )
                    }
                }

                // Add default categories
                val defaultCategories = listOf(
                    Category("movies", "Movies", "ic_movie"),
                    Category("series", "TV Series", "ic_tv"),
                    Category("anime", "Anime", "ic_animation"),
                    Category("documentary", "Documentaries", "ic_documentary")
                )
                categories.addAll(0, defaultCategories)

                _uiState.value = CategoriesUiState.Success(categories, itemsByCategory)

            } catch (e: Exception) {
                Timber.e(e, "Error loading categories")
                _uiState.value = CategoriesUiState.Error(e.message ?: "Failed to load categories")
            }
        }
    }

    /**
     * Load items for a specific category
     */
    fun loadCategoryItems(categoryId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is CategoriesUiState.Success) return@launch

            try {
                val items = mutableListOf<MediaItem>()

                extensionRepository.getEnabledExtensions().forEach { loadedExt ->
                    try {
                        val homeResponse = loadedExt.api.getMainPage()
                        homeResponse.sections.forEach { section ->
                            val sectionItems = section.items.map { extItem ->
                                extItem.toMediaItem(loadedExt.manifest.id, loadedExt.manifest.name)
                            }
                            items.addAll(sectionItems)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error loading from extension: ${loadedExt.manifest.name}")
                    }
                }

                // Filter items by category
                val filteredItems = when (categoryId) {
                    "movies" -> items.filter { it.type == MediaType.MOVIE }
                    "series" -> items.filter { it.type == MediaType.SERIES }
                    "anime" -> items.filter { it.type == MediaType.ANIME }
                    "documentary" -> items.filter { it.type == MediaType.DOCUMENTARY }
                    else -> items.filter { it.category == categoryId }
                }

                val updatedMap = currentState.itemsByCategory.toMutableMap()
                updatedMap[categoryId] = filteredItems

                _uiState.value = currentState.copy(itemsByCategory = updatedMap)

            } catch (e: Exception) {
                Timber.e(e, "Error loading category items")
            }
        }
    }

    /**
     * Refresh categories
     */
    fun refresh() {
        loadCategories()
    }
}
