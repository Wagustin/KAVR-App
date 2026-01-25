package com.thanhng224.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhng224.app.core.audio.MusicManager
import com.thanhng224.app.core.data.local.AppPreferences
import com.thanhng224.app.feature.auth.domain.usecases.CheckLoginStatusUseCase
import com.thanhng224.app.feature.onboarding.domain.usecases.CheckFirstLaunchUseCase
import com.thanhng224.app.feature.onboarding.domain.usecases.CompleteOnboardingUseCase
import com.thanhng224.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val checkFirstLaunchUseCase: CheckFirstLaunchUseCase,
    private val checkLoginStatusUseCase: CheckLoginStatusUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val appPreferences: AppPreferences,
    private val musicManager: MusicManager
) : ViewModel() {
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    val isDarkMode = appPreferences.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    
    val isMusicPlaying: StateFlow<Boolean> = musicManager.isPlaying

    init {
        viewModelScope.launch {
            val isFirstLaunch = checkFirstLaunchUseCase().first()
            // We purposefully ignore the login status to always show the "Start" screen (LoginScreen)
            // val isLoggedIn = checkLoginStatusUseCase().first()

            _startDestination.value = when {
                isFirstLaunch -> Screen.Onboarding.route
                // Always go to Login route (which is now the "Start" screen) if not first launch
                else -> Screen.Login.route
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            completeOnboardingUseCase()
        }
    }
    
    fun toggleTheme() {
        viewModelScope.launch {
            val current = isDarkMode.value
            appPreferences.setDarkMode(!current)
        }
    }

    // Music Controls
    fun toggleMusic() {
        if (isMusicPlaying.value) {
            musicManager.pauseMusic()
        } else {
            musicManager.resumeMusic()
        }
    }
    
    fun nextTrack() = musicManager.skipToNext()
    fun previousTrack() = musicManager.skipToPrevious()
    fun seekMusic(position: Int) = musicManager.seekTo(position)
    fun setMusicVolume(volume: Float) = musicManager.setVolume(volume)
    
    fun getMusicPosition() = musicManager.getCurrentPosition()
    fun getMusicDuration() = musicManager.getDuration()
}
