package com.taskbar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskbar.data.PanelPosition
import com.taskbar.data.SettingsRepository
import com.taskbar.data.SettingsUiState
import com.taskbar.data.WindowSizePref
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel всех экранов настроек.
 * Источник истины — DataStore Preferences, наружу отдаётся StateFlow.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SettingsRepository(app.applicationContext)

    val uiState: StateFlow<SettingsUiState> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    // ---- Главный тумблер ----
    fun setTaskbarEnabled(value: Boolean) = launch { repository.setTaskbarEnabled(value) }

    // ---- Freeform ----
    fun setFreeformEnabled(value: Boolean) = launch { repository.setFreeformEnabled(value) }
    fun setSaveWindowSizes(value: Boolean) = launch { repository.setSaveWindowSizes(value) }
    fun setAlwaysNewWindow(value: Boolean) = launch { repository.setAlwaysNewWindow(value) }
    fun setGamesFullscreen(value: Boolean) = launch { repository.setGamesFullscreen(value) }
    fun setWindowSize(value: WindowSizePref) = launch { repository.setWindowSize(value) }

    // ---- Главные настройки ----
    fun setAutostart(value: Boolean) = launch { repository.setAutostart(value) }
    fun setCloseAfterLaunch(value: Boolean) = launch { repository.setCloseAfterLaunch(value) }

    // ---- Внешний вид ----
    fun setPanelOpacity(value: Int) = launch { repository.setPanelOpacity(value) }
    fun setPanelIconSize(value: Int) = launch { repository.setPanelIconSize(value) }
    fun setPanelPosition(value: PanelPosition) = launch { repository.setPanelPosition(value) }

    // ---- Desktop mode ----
    fun setDesktopEnabled(value: Boolean) = launch { repository.setDesktopEnabled(value) }
    fun setDesktopFreeform(value: Boolean) = launch { repository.setDesktopFreeform(value) }

    // ---- Расширенные ----
    fun setDebugLogging(value: Boolean) = launch { repository.setDebugLogging(value) }
    fun resetSettings() = launch { repository.reset() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
