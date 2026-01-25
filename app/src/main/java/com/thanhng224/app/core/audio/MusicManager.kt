package com.thanhng224.app.core.audio

import android.content.Context
import android.media.MediaPlayer
import com.thanhng224.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null
    
    // StateFlow for playback status
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // History for "Previous" functionality
    private val trackHistory = Stack<Int>()
    private var currentTrackResId: Int = -1
    private var currentVolume = 0.5f

    // The intro track is mandatory first
    private val introResId = R.raw.intro

    // The playlist for random playback after intro
    private val playlist = listOf(
        R.raw.track_1,
        R.raw.track_2,
        R.raw.track_3,
        R.raw.track_4,
        R.raw.track_5,
        R.raw.track_6,
        R.raw.track_7
    )

    fun startMusic() {
        if (mediaPlayer == null) {
            playIntro()
        } else if (!mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
            _isPlaying.value = true
        }
    }

    private fun playIntro() {
        playTrack(introResId, isIntro = true)
    }

    fun skipToNext() {
        // Push current to history if valid
        if (currentTrackResId != -1) {
            trackHistory.push(currentTrackResId)
        }
        playNextRandomTrack()
    }

    fun skipToPrevious() {
        if (trackHistory.isNotEmpty()) {
            val previousTrackId = trackHistory.pop()
            playTrack(previousTrackId)
        } else {
            // Restart current if no history
            mediaPlayer?.seekTo(0)
        }
    }

    private fun playNextRandomTrack() {
        if (playlist.isEmpty()) return
        
        // Simple random strategy: pick one at random
        // Ideally we could avoid repeating the *exact* same one immediately, but simple is fine for now
        var nextTrackResId = playlist.random()
        
        // Retry once if it picked the same track, just for variety
        if (nextTrackResId == currentTrackResId && playlist.size > 1) {
            nextTrackResId = playlist.random()
        }
        
        playTrack(nextTrackResId)
    }

    private fun playTrack(resId: Int, isIntro: Boolean = false) {
        try {
            // Release previous resource
            mediaPlayer?.release()
            
            currentTrackResId = resId
            mediaPlayer = MediaPlayer.create(context, resId)
            
            if (mediaPlayer == null) {
                // Asset failed to load
                if (isIntro) playNextRandomTrack()
                else skipToNext() // Try another
                return
            }
            
            mediaPlayer?.setVolume(currentVolume, currentVolume)

            mediaPlayer?.apply {
                setOnCompletionListener {
                    // When done, auto-play next
                    playNextRandomTrack()
                }
                setOnErrorListener { _, _, _ ->
                    skipToNext() // Skip on error
                    true
                }
                start()
                _isPlaying.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            if (isIntro) playNextRandomTrack()
        }
    }

    fun pauseMusic() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            _isPlaying.value = false
        }
    }

    fun resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
            _isPlaying.value = true
        } else if (mediaPlayer == null) {
            startMusic()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun setVolume(volume: Float) {
        // volume should be 0.0f to 1.0f
        currentVolume = volume
        mediaPlayer?.setVolume(volume, volume)
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
    }
}
