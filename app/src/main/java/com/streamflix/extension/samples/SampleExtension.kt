package com.streamflix.extension.samples

import com.streamflix.extension.JsoupExtensionApi
import com.streamflix.extension.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber

/**
 * Sample Extension Implementation
 * 
 * This is a template extension that demonstrates how to implement
 * web scraping for a streaming site using Jsoup.
 * 
 * NOTE: This is for educational purposes. Replace the selectors
 * and URLs with actual values for the site you want to scrape.
 */
class SampleExtension(
    manifest: ExtensionManifest,
    httpClient: OkHttpClient
) : JsoupExtensionApi(manifest, httpClient) {

    private val baseUrl = manifest.baseUrl

    override suspend fun getMainPage(): ExtensionHomeResponse {
        return withContext(Dispatchers.IO) {
            try {
                val doc = fetchDocument(baseUrl)
                val sections = mutableListOf<ExtensionSection>()

                // Extract trending movies section
                val trendingSection = extractSection(
                    doc,
                    "trending",
                    "Trending Now",
                    ".trending-movies .movie-item"
                )
                if (trendingSection.items.isNotEmpty()) {
                    sections.add(trendingSection)
                }

                // Extract latest movies section
                val latestSection = extractSection(
                    doc,
                    "latest",
                    "Latest Movies",
                    ".latest-movies .movie-item"
                )
                if (latestSection.items.isNotEmpty()) {
                    sections.add(latestSection)
                }

                // Extract popular series section
                val seriesSection = extractSection(
                    doc,
                    "series",
                    "Popular Series",
                    ".popular-series .series-item"
                )
                if (seriesSection.items.isNotEmpty()) {
                    sections.add(seriesSection)
                }

                ExtensionHomeResponse(
                    sections = sections,
                    hasMore = sections.isNotEmpty()
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading main page")
                ExtensionHomeResponse(emptyList(), false)
            }
        }
    }

    override suspend fun search(query: String, page: Int): ExtensionSearchResponse {
        return withContext(Dispatchers.IO) {
            try {
                val searchUrl = "$baseUrl/search?q=${query.replace(" ", "+")}&page=$page"
                val doc = fetchDocument(searchUrl)

                val items = doc.select(".search-results .movie-item").map { element ->
                    extractMediaItem(element)
                }

                val hasMore = doc.select(".pagination .next").isNotEmpty()

                ExtensionSearchResponse(
                    items = items,
                    hasMore = hasMore,
                    nextPage = if (hasMore) page + 1 else null
                )
            } catch (e: Exception) {
                Timber.e(e, "Error searching for: $query")
                ExtensionSearchResponse(emptyList(), false)
            }
        }
    }

    override suspend fun loadMovie(url: String): ExtensionMovieResponse {
        return withContext(Dispatchers.IO) {
            try {
                val doc = fetchDocument(url)

                val title = doc.selectFirst(".movie-title h1")?.text() ?: "Unknown"
                val description = doc.selectFirst(".movie-description")?.text()
                val poster = doc.selectFirst(".movie-poster img")?.attr("src")?.let { 
                    resolveUrl(url, it) 
                }
                val backdrop = doc.selectFirst(".movie-backdrop")?.attr("style")
                    ?.let { extractBackgroundUrl(it) }
                val rating = doc.selectFirst(".movie-rating")?.text()?.toDoubleOrNull()
                val year = doc.selectFirst(".movie-year")?.text()?.toIntOrNull()
                val duration = doc.selectFirst(".movie-duration")?.text()
                    ?.let { extractDuration(it) }

                val genres = doc.select(".movie-genres a").map { it.text() }
                val cast = doc.select(".movie-cast .actor").map { it.text() }
                val director = doc.selectFirst(".movie-director")?.text()

                // Extract seasons for series
                val seasons = extractSeasons(doc, url)

                // Extract related content
                val related = doc.select(".related-movies .movie-item").map { element ->
                    extractMediaItem(element)
                }

                ExtensionMovieResponse(
                    id = url.substringAfterLast("/"),
                    title = title,
                    description = description,
                    poster = poster,
                    backdrop = backdrop,
                    type = if (seasons.isNotEmpty()) "series" else "movie",
                    rating = rating,
                    year = year,
                    duration = duration,
                    genres = genres,
                    cast = cast,
                    director = director,
                    seasons = seasons,
                    related = related
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading movie: $url")
                throw e
            }
        }
    }

    override suspend fun loadLinks(url: String): ExtensionLinksResponse {
        return withContext(Dispatchers.IO) {
            try {
                val doc = fetchDocument(url)
                val sources = mutableListOf<ExtensionVideoSource>()
                val subtitles = mutableListOf<ExtensionSubtitle>()

                // Extract video sources from the page
                doc.select(".video-source").forEach { element ->
                    val videoUrl = element.attr("data-src")
                    val quality = element.attr("data-quality")
                        .ifEmpty { extractQuality(element.text()) }
                    val type = element.attr("data-type")
                        .ifEmpty { detectVideoType(videoUrl) }

                    if (videoUrl.isNotBlank()) {
                        sources.add(
                            ExtensionVideoSource(
                                url = videoUrl,
                                quality = quality,
                                type = type,
                                headers = mapOf(
                                    "Referer" to baseUrl,
                                    "User-Agent" to "Mozilla/5.0"
                                )
                            )
                        )
                    }
                }

                // Extract subtitle sources
                doc.select(".subtitle-track").forEach { element ->
                    val subUrl = element.attr("data-src")
                    val language = element.attr("data-lang")
                    val label = element.text()

                    if (subUrl.isNotBlank()) {
                        subtitles.add(
                            ExtensionSubtitle(
                                url = subUrl,
                                language = language,
                                label = label
                            )
                        )
                    }
                }

                // If no sources found, try to extract from embedded player
                if (sources.isEmpty()) {
                    val embeddedSource = extractEmbeddedPlayerSource(doc)
                    if (embeddedSource != null) {
                        sources.add(embeddedSource)
                    }
                }

                ExtensionLinksResponse(
                    sources = sources,
                    subtitles = subtitles
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading links for: $url")
                ExtensionLinksResponse(emptyList(), emptyList())
            }
        }
    }

    // ==================== Helper Methods ====================

    private fun extractSection(
        doc: Document,
        id: String,
        title: String,
        selector: String
    ): ExtensionSection {
        val items = doc.select(selector).map { element ->
            extractMediaItem(element)
        }

        return ExtensionSection(
            id = id,
            title = title,
            items = items,
            layout = "horizontal"
        )
    }

    private fun extractMediaItem(element: Element): ExtensionMediaItem {
        val link = element.selectFirst("a")?.attr("href") ?: ""
        val title = element.selectFirst(".title")?.text()
            ?: element.selectFirst("img")?.attr("alt")
            ?: "Unknown"
        val poster = element.selectFirst("img")?.attr("src")?.let {
            resolveUrl(baseUrl, it)
        }
        val rating = element.selectFirst(".rating")?.text()?.toDoubleOrNull()
        val year = element.selectFirst(".year")?.text()?.toIntOrNull()

        return ExtensionMediaItem(
            id = link.substringAfterLast("/").ifEmpty { title },
            title = title,
            poster = poster,
            url = if (link.startsWith("http")) link else "$baseUrl$link",
            rating = rating,
            year = year,
            type = "movie"
        )
    }

    private fun extractSeasons(doc: Document, baseUrl: String): List<ExtensionSeason> {
        val seasons = mutableListOf<ExtensionSeason>()

        doc.select(".season").forEach { seasonElement ->
            val seasonNumber = seasonElement.attr("data-season").toIntOrNull() ?: return@forEach
            val seasonTitle = seasonElement.selectFirst(".season-title")?.text()

            val episodes = seasonElement.select(".episode").map { episodeElement ->
                ExtensionEpisode(
                    id = episodeElement.attr("data-id"),
                    title = episodeElement.selectFirst(".episode-title")?.text() ?: "Episode",
                    description = episodeElement.selectFirst(".episode-description")?.text(),
                    number = episodeElement.attr("data-episode").toIntOrNull() ?: 0,
                    season = seasonNumber,
                    thumbnail = episodeElement.selectFirst("img")?.attr("src")?.let {
                        resolveUrl(baseUrl, it)
                    },
                    duration = episodeElement.selectFirst(".duration")?.text()
                        ?.let { extractDuration(it) },
                    url = episodeElement.selectFirst("a")?.attr("href")?.let {
                        if (it.startsWith("http")) it else "$baseUrl$it"
                    } ?: ""
                )
            }

            if (episodes.isNotEmpty()) {
                seasons.add(
                    ExtensionSeason(
                        number = seasonNumber,
                        title = seasonTitle,
                        episodes = episodes
                    )
                )
            }
        }

        return seasons
    }

    private fun extractBackgroundUrl(style: String): String? {
        val regex = Regex("url\\(['\"]?(.*?)['\"]?\\)")
        return regex.find(style)?.groupValues?.get(1)
    }

    private fun extractDuration(text: String): Int? {
        // Extract minutes from strings like "2h 30min" or "150 min"
        val hourRegex = Regex("(\\d+)h")
        val minRegex = Regex("(\\d+)min")

        val hours = hourRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = minRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return if (hours > 0 || minutes > 0) {
            hours * 60 + minutes
        } else {
            text.filter { it.isDigit() }.toIntOrNull()
        }
    }

    private fun detectVideoType(url: String): String {
        return when {
            url.contains(".m3u8") -> "m3u8"
            url.contains(".mp4") -> "mp4"
            url.contains(".mkv") -> "mkv"
            url.contains("manifest") || url.contains("mpd") -> "dash"
            else -> "m3u8" // Default to HLS
        }
    }

    private fun extractEmbeddedPlayerSource(doc: Document): ExtensionVideoSource? {
        // Try to extract from common embedded players
        val iframe = doc.selectFirst("iframe[src*="player"], iframe[src*="embed"]")
        if (iframe != null) {
            val src = iframe.attr("src")
            return ExtensionVideoSource(
                url = src,
                quality = "Auto",
                type = "m3u8",
                headers = mapOf("Referer" to baseUrl)
            )
        }

        // Try to find video element
        val video = doc.selectFirst("video source")
        if (video != null) {
            val src = video.attr("src")
            return ExtensionVideoSource(
                url = src,
                quality = extractQuality(video.attr("data-quality")),
                type = detectVideoType(src),
                headers = mapOf("Referer" to baseUrl)
            )
        }

        return null
    }
}

/**
 * Extension Factory - Creates extension instances
 */
object ExtensionFactory {
    
    fun createSampleExtension(httpClient: OkHttpClient): SampleExtension {
        val manifest = ExtensionManifest(
            id = "sample.movies.extension",
            name = "Sample Movies",
            version = "1.0.0",
            author = "StreamFlix",
            description = "Sample extension for demonstration",
            language = "en",
            baseUrl = "https://example.com",
            apiVersion = 1
        )
        
        return SampleExtension(manifest, httpClient)
    }
}
