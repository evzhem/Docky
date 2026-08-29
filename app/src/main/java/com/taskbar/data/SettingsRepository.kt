package com.taskbar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Единственный экземпляр DataStore Preferences на весь процесс
val Context.taskbarDataStore: DataStore<Preferences> by preferencesDataStore(name = "taskbar_settings")

/**
 * Ключи всех настроек приложения.
 */
object SettingsKeys {
    // Главный тумблер Taskbar (служба с панелью)
    val TASKBAR_ENABLED = booleanPreferencesKey("taskbar_enabled")

    // Режим Freeform
    val FREEFORM_ENABLED = booleanPreferencesKey("freeform_enabled")
    val FREEFORM_SAVE_SIZES = booleanPreferencesKey("freeform_save_window_sizes")
    val FREEFORM_ALWAYS_NEW = booleanPreferencesKey("freeform_always_new_window")
    val FREEFORM_GAMES_FULLSCREEN = booleanPreferencesKey("freeform_games_fullscreen")
    val FREEFORM_WINDOW_SIZE = stringPreferencesKey("freeform_default_window_size")

    // Главные настройки
    val GENERAL_AUTOSTART = booleanPreferencesKey("general_autostart")
    val GENERAL_CLOSE_AFTER_LAUNCH = booleanPreferencesKey("general_close_after_launch")

    // Внешний вид
    val APPEARANCE_OPACITY = intPreferencesKey("appearance_panel_opacity")   // 60..100 (%)
    val APPEARANCE_ICON_SIZE = intPreferencesKey("appearance_icon_size")     // 36..64 (dp)
    val APPEARANCE_POSITION = stringPreferencesKey("appearance_panel_position")

    // Desktop mode
    val DESKTOP_ENABLED = booleanPreferencesKey("desktop_mode_enabled")
    val DESKTOP_FREEFORM = booleanPreferencesKey("desktop_freeform_windows")

    // Расширенные
    val ADVANCED_DEBUG = booleanPreferencesKey("advanced_debug_logging")

    // Сохранённые размеры свободных окон по пакетам (фича «Сохранять размеры окон»)
    fun windowWidthKey(packageName: String) = intPreferencesKey("win_w_$packageName")
    fun windowHeightKey(packageName: String) = intPreferencesKey("win_h_$packageName")
}

/**
 * Размер свободного окна по умолчанию (доля экрана).
 */
enum class WindowSizePref(val title: String, val factor: Float) {
    SMALL("Маленький (60% экрана)", 0.60f),
    MEDIUM("Средний (75% экрана)", 0.75f),
    LARGE("Большой (90% экрана)", 0.90f);

    companion object {
        fun fromName(name: String?): WindowSizePref =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

/**
 * Положение плавающей панели Taskbar.
 */
enum class PanelPosition(val title: String) {
    BOTTOM("Снизу экрана"),
    TOP("Сверху экрана");

    companion object {
        fun fromName(name: String?): PanelPosition =
            entries.firstOrNull { it.name == name } ?: BOTTOM
    }
}

/**
 * Полное состояние настроек в виде immutable-объекта для UI (MVVM).
 */
data class SettingsUiState(
    val taskbarEnabled: Boolean = false,
    // Freeform
    val freeformEnabled: Boolean = false,
    val saveWindowSizes: Boolean = false,
    val alwaysNewWindow: Boolean = false,
    val gamesFullscreen: Boolean = true,
    val windowSize: WindowSizePref = WindowSizePref.MEDIUM,
    // Главные
    val autostart: Boolean = true,
    val closeAfterLaunch: Boolean = true,
    // Внешний вид
    val panelOpacity: Int = 90,
    val panelIconSize: Int = 48,
    val panelPosition: PanelPosition = PanelPosition.BOTTOM,
    // Desktop
    val desktopEnabled: Boolean = false,
    val desktopFreeform: Boolean = true,
    // Расширенные
    val debugLogging: Boolean = false,
)

/**
 * Репозиторий настроек поверх DataStore Preferences.
 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<SettingsUiState> =
        context.taskbarDataStore.data.map { prefs -> prefs.toUiState() }

    suspend fun setTaskbarEnabled(value: Boolean) = edit { it[SettingsKeys.TASKBAR_ENABLED] = value }
    suspend fun setFreeformEnabled(value: Boolean) = edit { it[SettingsKeys.FREEFORM_ENABLED] = value }
    suspend fun setSaveWindowSizes(value: Boolean) = edit { it[SettingsKeys.FREEFORM_SAVE_SIZES] = value }
    suspend fun setAlwaysNewWindow(value: Boolean) = edit { it[SettingsKeys.FREEFORM_ALWAYS_NEW] = value }
    suspend fun setGamesFullscreen(value: Boolean) = edit { it[SettingsKeys.FREEFORM_GAMES_FULLSCREEN] = value }
    suspend fun setWindowSize(value: WindowSizePref) = edit { it[SettingsKeys.FREEFORM_WINDOW_SIZE] = value.name }
    suspend fun setAutostart(value: Boolean) = edit { it[SettingsKeys.GENERAL_AUTOSTART] = value }
    suspend fun setCloseAfterLaunch(value: Boolean) = edit { it[SettingsKeys.GENERAL_CLOSE_AFTER_LAUNCH] = value }
    suspend fun setPanelOpacity(value: Int) = edit { it[SettingsKeys.APPEARANCE_OPACITY] = value.coerceIn(60, 100) }
    suspend fun setPanelIconSize(value: Int) = edit { it[SettingsKeys.APPEARANCE_ICON_SIZE] = value.coerceIn(36, 64) }
    suspend fun setPanelPosition(value: PanelPosition) = edit { it[SettingsKeys.APPEARANCE_POSITION] = value.name }
    suspend fun setDesktopEnabled(value: Boolean) = edit { it[SettingsKeys.DESKTOP_ENABLED] = value }
    suspend fun setDesktopFreeform(value: Boolean) = edit { it[SettingsKeys.DESKTOP_FREEFORM] = value }
    suspend fun setDebugLogging(value: Boolean) = edit { it[SettingsKeys.ADVANCED_DEBUG] = value }

    /** Полный сброс настроек к значениям по умолчанию. */
    suspend fun reset() = edit { it.clear() }

    // ---- Сохранённые размеры свободных окон (фича «Сохранять размеры окон») ----

    /** Прочитать сохранённый размер окна для пакета (ширина, высота), если есть. */
    suspend fun getSavedWindowSize(packageName: String): Pair<Int, Int>? {
        val prefs = context.taskbarDataStore.data.first()
        val w = prefs[SettingsKeys.windowWidthKey(packageName)] ?: return null
        val h = prefs[SettingsKeys.windowHeightKey(packageName)] ?: return null
        return w to h
    }

    /** Сохранить размер свободного окна для пакета. */
    suspend fun saveWindowSize(packageName: String, width: Int, height: Int) = edit {
        it[SettingsKeys.windowWidthKey(packageName)] = width
        it[SettingsKeys.windowHeightKey(packageName)] = height
    }

    private suspend fun edit(block: MutablePreferences.() -> Unit) {
        context.taskbarDataStore.edit { preferences -> preferences.block() }
    }
}

/** Преобразование «сырых» Preferences в состояние для UI. */
fun Preferences.toUiState(): SettingsUiState = SettingsUiState(
    taskbarEnabled = this[SettingsKeys.TASKBAR_ENABLED] ?: false,
    freeformEnabled = this[SettingsKeys.FREEFORM_ENABLED] ?: false,
    saveWindowSizes = this[SettingsKeys.FREEFORM_SAVE_SIZES] ?: false,
    alwaysNewWindow = this[SettingsKeys.FREEFORM_ALWAYS_NEW] ?: false,
    gamesFullscreen = this[SettingsKeys.FREEFORM_GAMES_FULLSCREEN] ?: true,
    windowSize = WindowSizePref.fromName(this[SettingsKeys.FREEFORM_WINDOW_SIZE]),
    autostart = this[SettingsKeys.GENERAL_AUTOSTART] ?: true,
    closeAfterLaunch = this[SettingsKeys.GENERAL_CLOSE_AFTER_LAUNCH] ?: true,
    panelOpacity = this[SettingsKeys.APPEARANCE_OPACITY] ?: 90,
    panelIconSize = this[SettingsKeys.APPEARANCE_ICON_SIZE] ?: 48,
    panelPosition = PanelPosition.fromName(this[SettingsKeys.APPEARANCE_POSITION]),
    desktopEnabled = this[SettingsKeys.DESKTOP_ENABLED] ?: false,
    desktopFreeform = this[SettingsKeys.DESKTOP_FREEFORM] ?: true,
    debugLogging = this[SettingsKeys.ADVANCED_DEBUG] ?: false,
)
