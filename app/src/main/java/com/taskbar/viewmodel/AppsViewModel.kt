package com.taskbar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskbar.data.AppInfo
import com.taskbar.data.AppsRepository
import com.taskbar.data.RecentAppInfo
import com.taskbar.data.SettingsUiState
import com.taskbar.data.toUiState
import com.taskbar.data.taskbarDataStore
import com.taskbar.util.FreeformLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel списков приложений (все приложения и недавние)
 * и запуска приложений с учётом настроек Freeform.
 */
class AppsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppsRepository()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _recentApps = MutableStateFlow<List<RecentAppInfo>>(emptyList())
    val recentApps: StateFlow<List<RecentAppInfo>> = _recentApps.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Загрузить полный список установленных приложений (для меню). */
    fun loadApps() {
        viewModelScope.launch {
            _loading.value = true
            _apps.value = withContext(Dispatchers.IO) {
                repository.getInstalledApps(getApplication())
            }
            _loading.value = false
        }
    }

    /** Обновить список недавних приложений (UsageStatsManager). */
    fun loadRecentApps() {
        viewModelScope.launch {
            _loading.value = true
            _recentApps.value = withContext(Dispatchers.IO) {
                repository.getRecentApps(getApplication())
            }
            _loading.value = false
        }
    }

    /**
     * Запустить приложение: настройки Freeform читаются из DataStore
     * на момент запуска.
     */
    fun launchApp(app: AppInfo) {
        viewModelScope.launch {
            // Настройки Freeform читаются из DataStore на момент запуска
            val settings: SettingsUiState =
                getApplication<Application>().taskbarDataStore.data
                    .first()
                    .toUiState()
            withContext(Dispatchers.Main) {
                FreeformLauncher.launch(
                    context = getApplication(),
                    packageName = app.packageName,
                    freeformEnabled = settings.freeformEnabled,
                    alwaysNewWindow = settings.alwaysNewWindow,
                    gamesFullscreen = settings.gamesFullscreen,
                    isGame = app.isGame,
                    windowSize = settings.windowSize,
                    debugLogging = settings.debugLogging,
                )
            }
        }
    }
}
