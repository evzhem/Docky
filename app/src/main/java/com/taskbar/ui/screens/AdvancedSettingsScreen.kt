package com.taskbar.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskbar.ui.components.OrangeSectionHeader
import com.taskbar.ui.components.RowDivider
import com.taskbar.ui.components.ScreenScaffold
import com.taskbar.ui.components.SettingRow
import com.taskbar.ui.components.SettingsCard
import com.taskbar.ui.theme.TaskbarOrange
import com.taskbar.ui.theme.TaskbarSecondary
import com.taskbar.ui.theme.TaskbarWhite
import com.taskbar.util.TaskbarServiceController
import com.taskbar.viewmodel.SettingsViewModel

/**
 * Экран «Расширенные настройки»: отладка, перезапуск службы,
 * сброс настроек, информация о версии.
 */
@Composable
fun AdvancedSettingsScreen(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Расширенные настройки",
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        onBack = onBack
    ) {
        SettingsCard {
            SettingRow(
                icon = Icons.Filled.BugReport,
                title = "Отладочные логи",
                subtitle = "Писать в Logcat информацию о Freeform-запусках (тег FreeformLauncher)",
                trailing = {
                    androidx.compose.material3.Switch(
                        checked = state.debugLogging,
                        onCheckedChange = vm::setDebugLogging,
                        colors = orangeSwitchColors()
                    )
                }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.RestartAlt,
                title = "Перезапустить службу Taskbar",
                subtitle = "Полный перезапуск foreground-сервиса с панелью",
                onClick = { TaskbarServiceController.restart(context) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.DeleteForever,
                title = "Сбросить все настройки",
                subtitle = "Вернуть все параметры Taskbar к значениям по умолчанию",
                onClick = { showResetDialog = true }
            )
        }

        OrangeSectionHeader(text = "Информация")
        SettingsCard {
            SettingRow(
                icon = Icons.Filled.Info,
                title = "Версия Taskbar",
                subtitle = run {
                    val info = context.packageManager.getPackageInfo(context.packageName, 0)
                    "${info.versionName} (сборка ${info.longVersionCodeCompat()})"
                }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.Info,
                title = "Способ запуска свободных окон",
                subtitle = "ActivityOptions.makeBasic() + setLaunchBounds(Rect) через reflection"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = "Taskbar запускает приложения в свободных окнах через скрытый " +
                    "метод ActivityOptions.setLaunchBounds. Для работы режима на устройстве " +
                    "должны быть включены экспериментальные флаги Freeform " +
                    "(см. экран «Режим Freeform» → «?»).",
                color = TaskbarSecondary
            )
        }
    }

    // Подтверждение сброса настроек
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить настройки?", color = TaskbarWhite) },
            containerColor = com.taskbar.ui.theme.TaskbarSurface,
            text = {
                Text(
                    text = "Все параметры Taskbar (Freeform, внешний вид, desktop mode) " +
                        "будут возвращены к значениям по умолчанию. Панель будет остановлена.",
                    color = TaskbarSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetSettings()
                    TaskbarServiceController.stop(context)
                    showResetDialog = false
                }) {
                    Text("Сбросить", color = TaskbarOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена", color = TaskbarSecondary)
                }
            }
        )
    }
}

/** versionCode без вызова устаревшего versionCode на новых API. */
private fun android.content.pm.PackageInfo.longVersionCodeCompat(): Long =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }
