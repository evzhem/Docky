package com.taskbar.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskbar.ui.components.OrangeSectionHeader
import com.taskbar.ui.components.RowDivider
import com.taskbar.ui.components.ScreenScaffold
import com.taskbar.ui.components.SettingRow
import com.taskbar.ui.components.SettingsCard
import com.taskbar.ui.navigation.TaskbarRoutes
import com.taskbar.ui.theme.TaskbarOrange
import com.taskbar.ui.theme.TaskbarSecondary
import com.taskbar.ui.theme.TaskbarWhite
import com.taskbar.util.TaskbarServiceController
import com.taskbar.util.canDrawOverlays
import com.taskbar.util.hasNotificationPermission
import com.taskbar.util.openStorePage
import com.taskbar.util.requestOverlayPermission
import com.taskbar.viewmodel.SettingsViewModel

/**
 * Главный экран Taskbar: тумблер службы, навигация по разделам настроек,
 * секции «О Taskbar» и «Пожертвовать».
 */
@Composable
fun MainScreen(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onNavigate: (String) -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Запрос разрешения на уведомления (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Результат не критичен: служба работает и без уведомления
    }

    // Включение/выключение панели Taskbar
    val onTaskbarToggle: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (canDrawOverlays(context)) {
                // На Android 13+ заодно просим уведомления для foreground-сервиса
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                vm.setTaskbarEnabled(true)
                TaskbarServiceController.start(context)
            } else {
                // Без права «поверх других приложений» оверлей невозможен —
                // отправляем пользователя на системный экран разрешения
                requestOverlayPermission(context)
            }
        } else {
            vm.setTaskbarEnabled(false)
            TaskbarServiceController.stop(context)
        }
    }

    ScreenScaffold(
        title = "Taskbar",
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        checked = state.taskbarEnabled,
        onCheckedChange = onTaskbarToggle
    ) {
        // ---- Разделы настроек ----
        SettingsCard {
            SettingRow(
                icon = Icons.Filled.Settings,
                title = "Главные настройки",
                onClick = { onNavigate(TaskbarRoutes.GENERAL) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.Palette,
                title = "Внешний вид",
                onClick = { onNavigate(TaskbarRoutes.APPEARANCE) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.History,
                title = "Недавние приложения",
                onClick = { onNavigate(TaskbarRoutes.RECENT) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.GridView,
                title = "Режим Freeform",
                onClick = { onNavigate(TaskbarRoutes.FREEFORM) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.DesktopWindows,
                title = "Desktop mode",
                onClick = { onNavigate(TaskbarRoutes.DESKTOP) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.Tune,
                title = "Расширенные настройки",
                onClick = { onNavigate(TaskbarRoutes.ADVANCED) }
            )
        }

        // ---- Секция «О Taskbar» ----
        OrangeSectionHeader(text = "О Taskbar")
        SettingsCard {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Taskbar: меню приложений и список недавних у вас под рукой",
                    color = TaskbarWhite
                )
            }
            RowDivider()
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "© 2026 Taskbar",
                    color = TaskbarSecondary
                )
            }
            RowDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openStorePage(context) }
                    .padding(16.dp)
            ) {
                Text(
                    text = "Нажмите сюда, чтобы оставить оценку или обновиться! 😁",
                    color = TaskbarOrange,
                    textDecoration = TextDecoration.Underline
                )
            }
        }

        // ---- Секция «Пожертвовать» ----
        OrangeSectionHeader(text = "Пожертвовать")
        SettingsCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openStorePage(context) }
                    .padding(16.dp)
            ) {
                Text(
                    text = "Поддержите разработку Taskbar обновлением до версии Donate.",
                    color = TaskbarWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
