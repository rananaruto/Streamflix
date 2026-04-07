package com.streamflix.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflix.data.model.MediaItem
import com.streamflix.data.model.VideoLink
import com.streamflix.data.repository.MediaRepository
import com.streamflix.extension.ExtensionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Player ViewModel - Manages video playback state
 * 
 * Handles:
 * - Loading video sources
 * - Tracking playback progress
 * - Managing quality selection
 * - Subtitle handling
 */
class PlayerViewModel(
    private val mediaRepository: MediaRepository,
    private val extensionRepository: ExtensionRepository? = null
) : ViewModel() {

    // Playback State
    data class PlaybackState(
        val isPlaying: Boolean = false,
        val position: Long = 0,
        val duration: Long = 0,
        val bufferedPosition: Long = 0,
        val currentQuality: String = "Auto",
        val availableQualities: List<String> = emptyList(),
        val currentSubtitle: String? = null,
        val availableSubtitles: List<SubtitleTrack> = emptyList()
    )

    data class SubtitleTrack(
        val url: String,
        val language: String,
        val label: String
    )

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Video Sources
    private val _videoSources = MutableLiveData<List<VideoLink>>()
    val videoSources: LiveData<List<VideoLink>> = _videoSources

    // Current Media
    private val _currentMedia = MutableLiveData<MediaItem?>()
    val currentMedia: LiveData<MediaItem?> = _currentMedia

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error State
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Current video URI
    private val _currentVideoUri = MutableLiveData<Uri?>()
    val currentVideoUri: LiveData<Uri?> = _currentVideoUri

    // Progress save job
    private var progressSaveJob: kotlinx.coroutines.Job? = null

    /**
     * Load video sources for a media item
     */
    fun loadMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentMedia.value = mediaItem

            try {
                // Load saved position
                val savedPosition = mediaRepository.getWatchPosition(mediaItem.id)
                _playbackState.value = _playbackState.value.copy(position = savedPosition)

                // Find extension and load links
                val extension = extensionRepository?.getEnabledExtensions()
                    ?.find { it.manifest.id == mediaItem.extensionId }

                if (extension != null && mediaItem.url != null) {
                    val linksResponse = extension.api.loadLinks(mediaItem.url)
                    
                    val videoLinks = linksResponse.sources.map { source ->
                        com.streamflix.data.model.VideoLink(
                            url = source.url,
                            quality = source.quality,
                            type = when (source.type.lowercase()) {
                                "m3u8", "hls" -> com.streamflix.data.model.VideoType.M3U8
                                "mp4" -> com.streamflix.data.model.VideoType.MP4
                                "dash" -> com.streamflix.data.model.VideoType.DASH
                                else -> com.streamflix.data.model.VideoType.UNKNOWN
                            },
                            headers = source.headers,
                            isDirect = true
                        )
                    }

                    _videoSources.value = videoLinks

                    // Set default quality
                    val qualities = videoLinks.map { it.quality }.distinct()
                    _playbackState.value = _playbackState.value.copy(
                        availableQualities = qualities,
                        currentQuality = qualities.firstOrNull() ?: "Auto"
                    )

                    // Load first source
                    videoLinks.firstOrNull()?.let { link ->
                        _currentVideoUri.value = Uri.parse(link.url)
                    }

                } else {
                    _error.value = "Extension not found or URL missing"
                }

            } catch (e: Exception) {
                Timber.e(e, "Error loading video sources")
                _error.value = e.message ?: "Failed to load video"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update playback position
     */
    fun updatePosition(position: Long, duration: Long) {
        _playbackState.value = _playbackState.value.copy(
            position = position,
            duration = duration
        )

        // Save progress periodically
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5000) // Save every 5 seconds
            saveProgress(position, duration)
        }
    }

    /**
     * Save watch progress
     */
    private suspend fun saveProgress(position: Long, duration: Long) {
        _currentMedia.value?.let { media ->
            mediaRepository.updateWatchProgress(media.id, position, duration)
            Timber.d("Saved progress for ${media.id}: $position / $duration")
        }
    }

    /**
     * Change video quality
     */
    fun changeQuality(quality: String) {
        val sources = _videoSources.value ?: return
        val newSource = sources.find { it.quality == quality } ?: return

        _playbackState.value = _playbackState.value.copy(currentQuality = quality)
        _currentVideoUri.value = Uri.parse(newSource.url)
    }

    /**
     * Set playback state
     */
    fun setPlaying(isPlaying: Boolean) {
        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
    }

    /**
     * Set buffered position
     */
    fun setBufferedPosition(position: Long) {
        _playbackState.value = _playbackState.value.copy(bufferedPosition = position)
    }

    /**
     * Seek to position
     */
    fun seekTo(position: Long) {
        _playbackState.value = _playbackState.value.copy(position = position)
    }

    /**
     * Get video headers (for authentication/referer)
     */
    fun getVideoHeaders(): Map<String, String> {
        val currentQuality = _playbackState.value.currentQuality
        return _videoSources.value
            ?.find { it.quality == currentQuality }
            ?.headers ?: emptyMap()
    }

    override fun onCleared() {
        super.onCleared()
        // Save final progress
        progressSaveJob?.cancel()
        viewModelScope.launch {
            val state = _playbackState.value
            if (state.duration > 0) {
                saveProgress(state.position, state.duration)
            }
        }
    }
}
