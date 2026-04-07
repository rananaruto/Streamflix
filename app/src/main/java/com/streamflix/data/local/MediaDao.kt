package com.streamflix.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.streamflix.data.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Media DAO - Data Access Object for Media Items
 * 
 * Provides CRUD operations for media items including:
 * - Favorites management
 * - Watch progress tracking
 * - Search and filtering
 */
@Dao
interface MediaDao {

    // ==================== Basic CRUD ====================

    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: String): MediaItem?

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun getByIdFlow(id: String): Flow<MediaItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItem>)

    @Update
    suspend fun update(item: MediaItem)

    @Delete
    suspend fun delete(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM media_items")
    suspend fun deleteAll()

    // ==================== Favorites ====================

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY addedToFavoritesAt DESC")
    fun getFavorites(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY addedToFavoritesAt DESC")
    suspend fun getFavoritesList(): List<MediaItem>

    @Query("SELECT COUNT(*) FROM media_items WHERE isFavorite = 1")
    fun getFavoritesCount(): Flow<Int>

    @Query("UPDATE media_items SET isFavorite = :isFavorite, addedToFavoritesAt = :timestamp WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, timestamp: Long?)

    @Query("SELECT EXISTS(SELECT 1 FROM media_items WHERE id = :id AND isFavorite = 1)")
    fun isFavorite(id: String): Flow<Boolean>

    // ==================== Watch History ====================

    @Query("SELECT * FROM media_items WHERE lastWatchedAt IS NOT NULL ORDER BY lastWatchedAt DESC")
    fun getWatchHistory(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE lastWatchedAt IS NOT NULL ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getWatchHistoryLimited(limit: Int): Flow<List<MediaItem>>

    @Query("""
        UPDATE media_items 
        SET lastWatchedPosition = :position, 
            totalDuration = :duration,
            watchProgress = :progress,
            lastWatchedAt = :timestamp
        WHERE id = :id
    """)
    suspend fun updateWatchProgress(
        id: String,
        position: Long,
        duration: Long,
        progress: Float,
        timestamp: Long
    )

    @Query("SELECT lastWatchedPosition FROM media_items WHERE id = :id")
    suspend fun getWatchPosition(id: String): Long?

    @Query("SELECT watchProgress FROM media_items WHERE id = :id")
    suspend fun getWatchProgress(id: String): Float?

    @Query("DELETE FROM media_items WHERE lastWatchedAt IS NOT NULL")
    suspend fun clearWatchHistory()

    // ==================== Search & Filter ====================

    @Query("""
        SELECT * FROM media_items 
        WHERE title LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun search(query: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY createdAt DESC")
    fun getByType(type: com.streamflix.data.model.MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE extensionId = :extensionId ORDER BY createdAt DESC")
    fun getByExtension(extensionId: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<MediaItem>>

    // ==================== Continue Watching ====================

    @Query("""
        SELECT * FROM media_items 
        WHERE watchProgress > 0.05 
        AND watchProgress < 0.95 
        AND lastWatchedAt IS NOT NULL
        ORDER BY lastWatchedAt DESC 
        LIMIT 10
    """)
    fun getContinueWatching(): Flow<List<MediaItem>>

    // ==================== Statistics ====================

    @Query("SELECT COUNT(*) FROM media_items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE type = :type")
    fun getCountByType(type: com.streamflix.data.model.MediaType): Flow<Int>

    @Query("SELECT SUM(totalDuration) FROM media_items WHERE lastWatchedAt IS NOT NULL")
    fun getTotalWatchTime(): Flow<Long?>
}
