package com.thanhng224.app.core.audio

import android.content.Context
import android.media.MediaPlayer
import com.thanhng224.app.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MusicManager @Inject constructor(
    private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackIndex = -1 // -1 means Intro, >= 0 means playlist index

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
        }
    }

    private fun playIntro() {
        release() // Safety check
        mediaPlayer = MediaPlayer.create(context, introResId).apply {
            setOnCompletionListener {
                playNextRandomTrack()
            }
            start()
        }
    }

    private fun playNextRandomTrack() {
        release()
        
        // Pick a random track
        val nextTrackResId = playlist.random()
        
        mediaPlayer = MediaPlayer.create(context, nextTrackResId).apply {
            setOnCompletionListener {
                playNextRandomTrack() // Loop forever
            }
            start()
        }
    }

    fun pauseMusic() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
        } else if (mediaPlayer == null) {
            // Should usually not happen if app is just paused, but if killed and restored:
            startMusic()
        }
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
