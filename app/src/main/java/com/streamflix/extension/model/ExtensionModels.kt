package com.streamflix.extension.model

import com.google.gson.annotations.SerializedName
import com.streamflix.data.model.MediaItem
import com.streamflix.data.model.MediaType
import com.streamflix.data.model.VideoLink

/**
 * Extension API Response Models
 * 
 * These models define the contract between the app and extension plugins.
 * Extensions must return data in these formats.
 */

/**
 * Extension Manifest - Required metadata for each extension
 */
data class ExtensionManifest(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("version")
    val version: String,
    
    @SerializedName("author")
    val author: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("language")
    val language: String = "en",
    
    @SerializedName("icon")
    val icon: String? = null,
    
    @SerializedName("categories")
    val categories: List<String> = emptyList(),
    
    @SerializedName("baseUrl")
    val baseUrl: String,
    
    @SerializedName("apiVersion")
    val apiVersion: Int = 1
)

/**
 * Home Page Response from Extension
 */
data class ExtensionHomeResponse(
    @SerializedName("sections")
    val sections: List<ExtensionSection>,
    
    @SerializedName("hasMore")
    val hasMore: Boolean = false
)

/**
 * Section within home page
 */
data class ExtensionSection(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("items")
    val items: List<ExtensionMediaItem>,
    
    @SerializedName("layout")
    val layout: String = "grid" // grid, horizontal, carousel
)

/**
 * Media Item from Extension
 */
data class ExtensionMediaItem(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("poster")
    val poster: String? = null,
    
    @SerializedName("backdrop")
    val backdrop: String? = null,
    
    @SerializedName("type")
    val type: String = "movie", // movie, series, episode, anime
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("rating")
    val rating: Double? = null,
    
    @SerializedName("year")
    val year: Int? = null,
    
    @SerializedName("duration")
    val duration: Int? = null
)

/**
 * Search Response from Extension
 */
data class ExtensionSearchResponse(
    @SerializedName("items")
    val items: List<ExtensionMediaItem>,
    
    @SerializedName("hasMore")
    val hasMore: Boolean = false,
    
    @SerializedName("nextPage")
    val nextPage: Int? = null
)

/**
 * Movie Details Response from Extension
 */
data class ExtensionMovieResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("poster")
    val poster: String? = null,
    
    @SerializedName("backdrop")
    val backdrop: String? = null,
    
    @SerializedName("type")
    val type: String = "movie",
    
    @SerializedName("rating")
    val rating: Double? = null,
    
    @SerializedName("year")
    val year: Int? = null,
    
    @SerializedName("duration")
    val duration: Int? = null,
    
    @SerializedName("genres")
    val genres: List<String> = emptyList(),
    
    @SerializedName("cast")
    val cast: List<String> = emptyList(),
    
    @SerializedName("director")
    val director: String? = null,
    
    @SerializedName("seasons")
    val seasons: List<ExtensionSeason> = emptyList(),
    
    @SerializedName("related")
    val related: List<ExtensionMediaItem> = emptyList()
)

/**
 * Season from Extension
 */
data class ExtensionSeason(
    @SerializedName("number")
    val number: Int,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("episodes")
    val episodes: List<ExtensionEpisode>
)

/**
 * Episode from Extension
 */
data class ExtensionEpisode(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("number")
    val number: Int,
    
    @SerializedName("season")
    val season: Int,
    
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    
    @SerializedName("duration")
    val duration: Int? = null,
    
    @SerializedName("url")
    val url: String
)

/**
 * Video Links Response from Extension
 */
data class ExtensionLinksResponse(
    @SerializedName("sources")
    val sources: List<ExtensionVideoSource>,
    
    @SerializedName("subtitles")
    val subtitles: List<ExtensionSubtitle> = emptyList()
)

/**
 * Video Source from Extension
 */
data class ExtensionVideoSource(
    @SerializedName("url")
    val url: String,
    
    @SerializedName("quality")
    val quality: String, // 1080p, 720p, 480p, Auto
    
    @SerializedName("type")
    val type: String = "m3u8", // m3u8, mp4, dash
    
    @SerializedName("headers")
    val headers: Map<String, String> = emptyMap()
)

/**
 * Subtitle from Extension
 */
data class ExtensionSubtitle(
    @SerializedName("url")
    val url: String,
    
    @SerializedName("language")
    val language: String,
    
    @SerializedName("label")
    val label: String
)

/**
 * Extension API Interface
 * 
 * All extensions must implement these methods
 */
interface ExtensionApi {
    
    /**
     * Get extension manifest/info
     */
    suspend fun getManifest(): ExtensionManifest
    
    /**
     * Get home page content sections
     */
    suspend fun getMainPage(): ExtensionHomeResponse
    
    /**
     * Search for content
     */
    suspend fun search(query: String, page: Int = 1): ExtensionSearchResponse
    
    /**
     * Load movie/series details
     */
    suspend fun loadMovie(url: String): ExtensionMovieResponse
    
    /**
     * Load streaming links for an episode/movie
     */
    suspend fun loadLinks(url: String): ExtensionLinksResponse
}

/**
 * Extension Loader Interface
 */
interface ExtensionLoader {
    fun loadExtension(source: ExtensionSource): LoadedExtension
    fun unloadExtension(extensionId: String)
    fun getLoadedExtensions(): List<LoadedExtension>
}

/**
 * Extension Source - Where to load from
 */
sealed class ExtensionSource {
    data class GitHub(val repoUrl: String, val releaseTag: String? = null) : ExtensionSource()
    data class Local(val filePath: String) : ExtensionSource()
    data class BuiltIn(val assetPath: String) : ExtensionSource()
}

/**
 * Loaded Extension Wrapper
 */
data class LoadedExtension(
    val manifest: ExtensionManifest,
    val api: ExtensionApi,
    val source: ExtensionSource,
    val isEnabled: Boolean = true
)

/**
 * Extension Converters - Convert extension models to app models
 */
object ExtensionConverters {
    
    fun ExtensionMediaItem.toMediaItem(extensionId: String, extensionName: String): MediaItem {
        return MediaItem(
            id = "${extensionId}_$id",
            title = title,
            description = description,
            posterUrl = poster,
            backdropUrl = backdrop,
            type = when (type.lowercase()) {
                "series", "tv" -> MediaType.SERIES
                "anime" -> MediaType.ANIME
                "documentary" -> MediaType.DOCUMENTARY
                else -> MediaType.MOVIE
            },
            rating = rating,
            year = year,
            duration = duration,
            extensionId = extensionId,
            extensionName = extensionName,
            url = url
        )
    }
    
    fun ExtensionVideoSource.toVideoLink(): VideoLink {
        return VideoLink(
            url = url,
            quality = quality,
            type = when (type.lowercase()) {
                "m3u8", "hls" -> com.streamflix.data.model.VideoType.M3U8
                "mp4" -> com.streamflix.data.model.VideoType.MP4
                "dash" -> com.streamflix.data.model.VideoType.DASH
                else -> com.streamflix.data.model.VideoType.UNKNOWN
            },
            headers = headers,
            isDirect = true
        )
    }
}
