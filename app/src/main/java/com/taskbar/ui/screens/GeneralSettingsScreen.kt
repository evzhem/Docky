package com.taskbar.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.taskbar.util.canDrawOverlays
import com.taskbar.util.hasNotificationPermission
import com.taskbar.util.hasUsageAccess
import com.taskbar.util.rememberGrantedState
import com.taskbar.util.requestOverlayPermission
import com.taskbar.util.requestUsageAccess
import com.taskbar.viewmodel.SettingsViewModel

/**
 * Экран «Главные настройки»: разрешения, автозапуск, поведение меню,
 * перезапуск службы.
 */
@Composable
fun GeneralSettingsScreen(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Состояния разрешений автоматически обновляются при возврате в приложение
    val overlayGranted = rememberGrantedState { canDrawOverlays(it) }
    val usageGranted = rememberGrantedState { hasUsageAccess(it) }
    val notificationsGranted = rememberGrantedState { hasNotificationPermission(it) }

    ScreenScaffold(
        title = "Главные настройки",
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        onBack = onBack
    ) {
        OrangeSectionHeader(text = "Разрешения")
        SettingsCard {
            SettingRow(
                icon = Icons.Filled.Layers,
                title = "Поверх других приложений",
                subtitle = if (overlayGranted) "Разрешение выдано" else "Разрешение не выдано",
                trailing = { StatusDot(ok = overlayGranted) },
                onClick = { requestOverlayPermission(context) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.QueryStats,
                title = "Доступ к статистике использования",
                subtitle = if (usageGranted) "Доступ выдан" else "Доступ не выдан (нужен для недавних приложений)",
                trailing = { StatusDot(ok = usageGranted) },
                onClick = { requestUsageAccess(context) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.Notifications,
                title = "Уведомления",
                subtitle = if (notificationsGranted) "Уведомления разрешены" else "Уведомления запрещены",
                trailing = { StatusDot(ok = notificationsGranted) },
                onClick = {
                    // Открываем системные настройки уведомлений приложения
                    runCatching {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        ).apply {
                            putExtra(
                                android.provider.Settings.EXTRA_APP_PACKAGE,
                                context.packageName
                            )
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }

        OrangeSectionHeader(text = "Поведение")
        SettingsCard {
            SettingRow(
                icon = Icons.Filled.PowerSettingsNew,
                title = "Запускать при загрузке устройства",
                subtitle = "Автоматически показывать панель после включения телефона",
                trailing = {
                    androidx.compose.material3.Switch(
                        checked = state.autostart,
                        onCheckedChange = vm::setAutostart,
                        colors = orangeSwitchColors()
                    )
                }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.SwitchLeft,
                title = "Закрывать меню после запуска",
                subtitle = "Скрывать меню приложений после выбора программы",
                trailing = {
                    androidx.compose.material3.Switch(
                        checked = state.closeAfterLaunch,
                        onCheckedChange = vm::setCloseAfterLaunch,
                        colors = orangeSwitchColors()
                    )
                }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.RestartAlt,
                title = "Перезапустить панель Taskbar",
                subtitle = "Останавливает и заново запускает службу с панелью",
                onClick = { TaskbarServiceController.restart(context) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = "Панель Taskbar работает как foreground-служба и показывает " +
                    "постоянное уведомление, пока активна. Если службы не видно — " +
                    "проверьте главный тумблер на экране Taskbar и разрешение " +
                    "«Поверх других приложений».",
                color = TaskbarSecondary,
                fontSize = 12.sp
            )
        }
    }
}

/** Цветовая схема переключателей: оранжевый трек, как в дизайне Taskbar. */
@Composable
fun orangeSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor = TaskbarWhite,
    checkedTrackColor = TaskbarOrange,
    uncheckedThumbColor = TaskbarWhite,
    uncheckedTrackColor = androidx.compose.ui.graphics.Color(0x55000000),
    uncheckedBorderColor = TaskbarSecondary,
)

/** Зелёная/серая точка-статус разрешения. */
@Composable
private fun StatusDot(ok: Boolean) {
    Text(
        text = if (ok) "✓" else "!",
        color = if (ok) androidx.compose.ui.graphics.Color(0xFF4CAF50) else TaskbarOrange,
        fontWeight = FontWeight.Bold
    )
}
