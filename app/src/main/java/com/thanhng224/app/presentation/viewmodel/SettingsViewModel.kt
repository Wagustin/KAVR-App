package com.thanhng224.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhng224.app.R
import com.thanhng224.app.core.data.local.AppPreferences
import com.thanhng224.app.core.di.IoDispatcher
import com.thanhng224.app.feature.auth.domain.usecases.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val showTermsDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val logoutRequested: Boolean = false,
    val snackbarMessageRes: Int? = null,
    val darkModeEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val appPreferences: AppPreferences,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _internalUiState = MutableStateFlow(SettingsUiState())
    
    val uiState: StateFlow<SettingsUiState> = combine(
        _internalUiState,
        appPreferences.isDarkMode
    ) { state, isDarkMode ->
        state.copy(darkModeEnabled = isDarkMode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onNotificationsToggled(enabled: Boolean) {
        _internalUiState.update {
            it.copy(
                notificationsEnabled = enabled,
                snackbarMessageRes = if (enabled) {
                    R.string.settings_notifications_enabled_message
                } else {
                    R.string.settings_notifications_disabled_message
                }
            )
        }
    }

    fun onDarkModeToggled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setDarkMode(enabled)
        }
    }

    fun onShowTermsDialog() {
        _internalUiState.update { it.copy(showTermsDialog = true) }
    }

    fun onShowPrivacyDialog() {
        _internalUiState.update { it.copy(showPrivacyDialog = true) }
    }

    fun onDismissTermsDialog() {
        _internalUiState.update { it.copy(showTermsDialog = false) }
    }

    fun onDismissPrivacyDialog() {
        _internalUiState.update { it.copy(showPrivacyDialog = false) }
    }

    fun onLogoutDialogShown() {
        _internalUiState.update { it.copy(showLogoutDialog = true) }
    }

    fun onLogoutDialogDismissed() {
        _internalUiState.update { it.copy(showLogoutDialog = false) }
    }

    fun onLogoutConfirmed() {
        viewModelScope.launch(ioDispatcher) {
            logoutUseCase()
            _internalUiState.update {
                it.copy(
                    showLogoutDialog = false,
                    logoutRequested = true
                )
            }
        }
    }

    fun onLogoutHandled() {
        _internalUiState.update { it.copy(logoutRequested = false) }
    }

    fun onSnackbarShown() {
        _internalUiState.update { it.copy(snackbarMessageRes = null) }
    }
}
