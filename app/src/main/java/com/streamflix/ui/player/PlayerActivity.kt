package com.streamflix.ui.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.streamflix.data.model.VideoType
import com.streamflix.databinding.ActivityPlayerBinding
import com.streamflix.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Player Activity - Video playback with ExoPlayer
 * 
 * Features:
 * - HLS (m3u8) and MP4 playback
 * - Custom controls
 * - Picture-in-Picture
 * - Fullscreen mode
 * - Gesture controls
 */
@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModel()
    
    private var exoPlayer: ExoPlayer? = null
    private var isFullscreen = true
    
    companion object {
        const val EXTRA_MEDIA_ITEM = "extra_media_item"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get media item from intent
        val mediaItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_MEDIA_ITEM, com.streamflix.data.model.MediaItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_MEDIA_ITEM)
        }
        
        if (mediaItem == null) {
            Toast.makeText(this, "Error loading video", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupFullscreen()
        setupPlayer()
        setupControls()
        observeViewModel()
        
        // Load video
        viewModel.loadMedia(mediaItem)
    }

    private fun setupFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun setupPlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
            .apply {
                binding.playerView.player = this
                
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                binding.progressLoading.visibility = View.VISIBLE
                            }
                            Player.STATE_READY -> {
                                binding.progressLoading.visibility = View.GONE
                            }
                            Player.STATE_ENDED -> {
                                // Handle ended
                            }
                            Player.STATE_IDLE -> {
                                binding.progressLoading.visibility = View.GONE
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        binding.progressLoading.visibility = View.GONE
                        binding.errorView.visibility = View.VISIBLE
                        Toast.makeText(this@PlayerActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        viewModel.setPlaying(isPlaying)
                    }
                })
            }
    }

    private fun setupControls() {
        // Retry button
        binding.btnRetry.setOnClickListener {
            binding.errorView.visibility = View.GONE
            exoPlayer?.prepare()
        }

        // Custom control buttons from layout
        binding.playerView.findViewById<ImageButton>(R.id.exo_back)?.setOnClickListener {
            finish()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btn_settings)?.setOnClickListener {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btn_cast)?.setOnClickListener {
            Toast.makeText(this, "Cast", Toast.LENGTH_SHORT).show()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btn_subtitle)?.setOnClickListener {
            Toast.makeText(this, "Subtitles", Toast.LENGTH_SHORT).show()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btn_quality)?.setOnClickListener {
            Toast.makeText(this, "Quality", Toast.LENGTH_SHORT).show()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btn_next)?.setOnClickListener {
            Toast.makeText(this, "Next Episode", Toast.LENGTH_SHORT).show()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btn_pip)?.setOnClickListener {
            enterPipMode()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btn_fullscreen)?.setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentVideoUri.collect { uri ->
                        uri?.let { playVideo(it) }
                    }
                }
                
                launch {
                    viewModel.playbackState.collect { state ->
                        // Update UI based on playback state
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun playVideo(uri: Uri) {
        val headers = viewModel.getVideoHeaders()
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
        
        val mediaSource = when {
            uri.toString().contains(".m3u8") -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
        }
        
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            play()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        
        if (isInPictureInPictureMode) {
            binding.playerView.useController = false
        } else {
            binding.playerView.useController = true
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode) {
            exoPlayer?.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onBackPressed() {
        if (isFullscreen) {
            isFullscreen = false
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onBackPressed()
    }
}
