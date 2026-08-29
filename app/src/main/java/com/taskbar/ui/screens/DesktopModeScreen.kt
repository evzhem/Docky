package com.taskbar.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import com.taskbar.util.FreeformLauncher
import com.taskbar.viewmodel.SettingsViewModel

/**
 * Экран «Desktop mode»: режим рабочего стола на внешних дисплеях
 * (управляется глобальной настройкой force_desktop_mode_on_external_displays)
 * и связка с Freeform-окнами.
 */
@Composable
fun DesktopModeScreen(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var desktopFlag by remember {
        mutableStateOf(FreeformLauncher.isDesktopModeEnabled(context))
    }

    ScreenScaffold(
        title = "Desktop mode",
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        onBack = onBack
    ) {
        SettingsCard {
            // Переключатель Desktop mode в настройках приложения
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DesktopWindows,
                    contentDescription = null,
                    tint = TaskbarWhite,
                    modifier = Modifier
                        .height(26.dp)
                        .padding(end = 0.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Режим Desktop", color = TaskbarWhite)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Использовать режим рабочего стола при подключении " +
                            "внешнего дисплея (Android 10+).",
                        color = TaskbarSecondary
                    )
                }
                Switch(
                    checked = state.desktopEnabled,
                    onCheckedChange = vm::setDesktopEnabled,
                    colors = orangeSwitchColors()
                )
            }
            RowDivider()
            // Freeform-окна в desktop-режиме
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.GridView,
                    contentDescription = null,
                    tint = TaskbarWhite.copy(alpha = if (state.desktopEnabled) 1f else 0.4f),
                    modifier = Modifier.height(26.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Окна Freeform в desktop mode",
                        color = TaskbarWhite.copy(alpha = if (state.desktopEnabled) 1f else 0.4f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Запускать приложения свободными окнами и в desktop mode",
                        color = TaskbarSecondary.copy(alpha = if (state.desktopEnabled) 1f else 0.4f)
                    )
                }
                Switch(
                    checked = state.desktopFreeform,
                    enabled = state.desktopEnabled,
                    onCheckedChange = vm::setDesktopFreeform,
                    colors = orangeSwitchColors()
                )
            }
        }

        OrangeSectionHeader(text = "Включение через ADB")
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Системный режим рабочего стола включается глобальной настройкой. " +
                        "Выполните на компьютере с ADB:",
                    color = TaskbarSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = FreeformLauncher.DESKTOP_ADB_COMMAND,
                    color = TaskbarWhite
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(FreeformLauncher.DESKTOP_ADB_COMMAND))
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Скопировать команду")
                }
            }
            RowDivider()
            SettingRow(
                icon = Icons.Filled.CheckCircle,
                title = "force_desktop_mode_on_external_displays",
                subtitle = if (desktopFlag) "Флаг включён" else "Флаг не включён",
                onClick = { desktopFlag = FreeformLauncher.isDesktopModeEnabled(context) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.Refresh,
                title = "Перепроверить состояние",
                onClick = { desktopFlag = FreeformLauncher.isDesktopModeEnabled(context) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
