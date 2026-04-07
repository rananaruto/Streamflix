package com.streamflix.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Media Item Model - Core data class representing movies, series, or episodes
 * 
 * This is the main data structure used throughout the app for displaying
 * content from various extensions.
 */
@Parcelize
@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val type: MediaType,
    val category: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val duration: Int? = null, // in minutes
    val extensionId: String,
    val extensionName: String,
    val url: String? = null, // Source URL from extension
    val isFavorite: Boolean = false,
    val lastWatchedPosition: Long = 0,
    val totalDuration: Long = 0,
    val watchProgress: Float = 0f,
    val addedToFavoritesAt: Long? = null,
    val lastWatchedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Media Type Enum - Distinguishes between different content types
 */
enum class MediaType {
    MOVIE,
    SERIES,
    EPISODE,
    ANIME,
    DOCUMENTARY
}

/**
 * Extension Model - Represents a loaded extension/plugin
 */
@Parcelize
@Entity(tableName = "extensions")
data class Extension(
    @PrimaryKey
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val sourceUrl: String, // GitHub repo or local path
    val language: String = "en",
    val isEnabled: Boolean = true,
    val isInstalled: Boolean = false,
    val installedAt: Long? = null,
    val updatedAt: Long? = null,
    val categories: List<String> = emptyList()
) : Parcelable

/**
 * Video Link Model - Represents a streaming link
 */
@Parcelize
data class VideoLink(
    val url: String,
    val quality: String, // 1080p, 720p, 480p, etc.
    val type: VideoType,
    val headers: Map<String, String> = emptyMap(),
    val subtitleUrl: String? = null,
    val isDirect: Boolean = true
) : Parcelable

/**
 * Video Type Enum - Supported video formats
 */
enum class VideoType {
    M3U8,
    MP4,
    MKV,
    DASH,
    UNKNOWN
}

/**
 * Category Model - For organizing content
 */
@Parcelize
data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,
    val extensionId: String
) : Parcelable

/**
 * Home Page Section - For organizing content on home screen
 */
@Parcelize
data class HomeSection(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
    val type: SectionType = SectionType.GRID
) : Parcelable

/**
 * Section Type Enum - Layout types for home sections
 */
enum class SectionType {
    GRID,
    HORIZONTAL_LIST,
    CAROUSEL,
    CATEGORY_GRID
}

/**
 * Search Result Wrapper
 */
data class SearchResult(
    val query: String,
    val items: List<MediaItem>,
    val hasMore: Boolean = false,
    val nextPage: Int? = null
)

/**
 * Watch History Entry
 */
@Entity(tableName = "watch_history")
data class WatchHistoryEntry(
    @PrimaryKey
    val mediaId: String,
    val position: Long = 0,
    val duration: Long = 0,
    val progress: Float = 0f,
    val watchedAt: Long = System.currentTimeMillis()
)

/**
 * Episode Model - For series content
 */
@Parcelize
data class Episode(
    val id: String,
    val title: String,
    val description: String? = null,
    val number: Int,
    val season: Int,
    val thumbnailUrl: String? = null,
    val duration: Int? = null,
    val url: String? = null
) : Parcelable

/**
 * Season Model - Contains episodes
 */
@Parcelize
data class Season(
    val number: Int,
    val title: String? = null,
    val episodes: List<Episode>
) : Parcelable

/**
 * Movie Details - Extended info for movie page
 */
@Parcelize
data class MovieDetails(
    val mediaItem: MediaItem,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val director: String? = null,
    val seasons: List<Season> = emptyList(),
    val relatedContent: List<MediaItem> = emptyList(),
    val videoLinks: List<VideoLink> = emptyList()
) : Parcelable

/**
 * App Theme Settings
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useLiquidGlass: Boolean = true,
    val autoPlayNext: Boolean = true,
    val defaultVideoQuality: String = "Auto",
    val subtitleLanguage: String = "en",
    val downloadQuality: String = "720p"
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
