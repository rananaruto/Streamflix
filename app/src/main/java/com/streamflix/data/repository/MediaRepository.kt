package com.streamflix.data.repository

import com.streamflix.data.local.MediaDao
import com.streamflix.data.model.MediaItem
import com.streamflix.data.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Media Repository - Central data management for media items
 * 
 * Handles:
 * - Favorites management
 * - Watch history tracking
 * - Search operations
 * - Caching from extensions
 */
class MediaRepository(
    private val mediaDao: MediaDao
) {
    // ==================== Favorites ====================

    fun getFavorites(): Flow<List<MediaItem>> = mediaDao.getFavorites()

    suspend fun getFavoritesList(): List<MediaItem> = mediaDao.getFavoritesList()

    fun getFavoritesCount(): Flow<Int> = mediaDao.getFavoritesCount()

    suspend fun addToFavorites(mediaItem: MediaItem) {
        val existing = mediaDao.getById(mediaItem.id)
        if (existing != null) {
            mediaDao.setFavorite(mediaItem.id, true, System.currentTimeMillis())
        } else {
            mediaDao.insert(mediaItem.copy(
                isFavorite = true,
                addedToFavoritesAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun removeFromFavorites(mediaId: String) {
        mediaDao.setFavorite(mediaId, false, null)
    }

    fun isFavorite(mediaId: String): Flow<Boolean> = mediaDao.isFavorite(mediaId)

    suspend fun toggleFavorite(mediaItem: MediaItem): Boolean {
        val isCurrentlyFavorite = mediaDao.isFavorite(mediaItem.id).first()
        if (isCurrentlyFavorite) {
            removeFromFavorites(mediaItem.id)
        } else {
            addToFavorites(mediaItem)
        }
        return !isCurrentlyFavorite
    }

    // ==================== Watch History ====================

    fun getWatchHistory(): Flow<List<MediaItem>> = mediaDao.getWatchHistory()

    fun getWatchHistoryLimited(limit: Int): Flow<List<MediaItem>> = 
        mediaDao.getWatchHistoryLimited(limit)

    fun getContinueWatching(): Flow<List<MediaItem>> = mediaDao.getContinueWatching()

    suspend fun updateWatchProgress(
        mediaId: String,
        position: Long,
        duration: Long
    ) {
        val progress = if (duration > 0) position.toFloat() / duration else 0f
        mediaDao.updateWatchProgress(
            id = mediaId,
            position = position,
            duration = duration,
            progress = progress,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun getWatchPosition(mediaId: String): Long {
        return mediaDao.getWatchPosition(mediaId) ?: 0L
    }

    suspend fun clearWatchHistory() {
        mediaDao.clearWatchHistory()
    }

    // ==================== Search ====================

    fun search(query: String): Flow<List<MediaItem>> = mediaDao.search(query)

    // ==================== Cache Management ====================

    suspend fun cacheMediaItems(items: List<MediaItem>) {
        mediaDao.insertAll(items)
    }

    suspend fun getCachedItem(id: String): MediaItem? = mediaDao.getById(id)

    fun getByType(type: MediaType): Flow<List<MediaItem>> = mediaDao.getByType(type)

    fun getByExtension(extensionId: String): Flow<List<MediaItem>> = 
        mediaDao.getByExtension(extensionId)

    // ==================== Statistics ====================

    fun getTotalCount(): Flow<Int> = mediaDao.getTotalCount()

    fun getTotalWatchTime(): Flow<Long?> = mediaDao.getTotalWatchTime()
}
