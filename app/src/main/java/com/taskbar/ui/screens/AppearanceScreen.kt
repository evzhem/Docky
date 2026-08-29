package com.taskbar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskbar.data.PanelPosition
import com.taskbar.ui.components.ScreenScaffold
import com.taskbar.ui.components.SettingRow
import com.taskbar.ui.components.SettingsCard
import com.taskbar.ui.theme.TaskbarOrange
import com.taskbar.ui.theme.TaskbarSecondary
import com.taskbar.ui.theme.TaskbarWhite
import com.taskbar.viewmodel.SettingsViewModel

/**
 * Экран «Внешний вид»: прозрачность панели, размер иконок, положение.
 * Все настройки применяются к плавающей панели сервиса на лету.
 */
@Composable
fun AppearanceScreen(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showPositionDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Внешний вид",
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        onBack = onBack
    ) {
        SettingsCard {
            // Прозрачность панели
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Opacity,
                        contentDescription = null,
                        tint = TaskbarWhite,
                        modifier = Modifier.height(26.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Прозрачность панели", color = TaskbarWhite)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${state.panelOpacity}%", color = TaskbarOrange)
                }
                Slider(
                    value = state.panelOpacity.toFloat(),
                    onValueChange = { vm.setPanelOpacity(it.toInt()) },
                    valueRange = 60f..100f,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = TaskbarOrange,
                        activeTrackColor = TaskbarOrange,
                        inactiveTrackColor = TaskbarSecondary.copy(alpha = 0.4f)
                    )
                )
            }

            androidx.compose.material3.HorizontalDivider(
                color = androidx.compose.ui.graphics.Color(0x22FFFFFF)
            )

            // Размер иконок
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FormatSize,
                        contentDescription = null,
                        tint = TaskbarWhite,
                        modifier = Modifier.height(26.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Размер иконок на панели", color = TaskbarWhite)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${state.panelIconSize} dp", color = TaskbarOrange)
                }
                Slider(
                    value = state.panelIconSize.toFloat(),
                    onValueChange = { vm.setPanelIconSize(it.toInt()) },
                    valueRange = 36f..64f,
                    steps = 27,
                    colors = SliderDefaults.colors(
                        thumbColor = TaskbarOrange,
                        activeTrackColor = TaskbarOrange,
                        inactiveTrackColor = TaskbarSecondary.copy(alpha = 0.4f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCard {
            SettingRow(
                icon = Icons.Filled.SwapVert,
                title = "Положение панели",
                subtitle = state.panelPosition.title,
                onClick = { showPositionDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Изменения применяются к плавающей панели сразу после выхода " +
                "из настроек (панель перезапускать не нужно).",
            color = TaskbarSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

    // Диалог выбора положения панели
    if (showPositionDialog) {
        AlertDialog(
            onDismissRequest = { showPositionDialog = false },
            title = { Text("Положение панели", color = TaskbarWhite) },
            containerColor = com.taskbar.ui.theme.TaskbarSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PanelPosition.entries.forEach { position ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.panelPosition == position,
                                onClick = {
                                    vm.setPanelPosition(position)
                                    showPositionDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TaskbarOrange,
                                    unselectedColor = TaskbarSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(position.title, color = TaskbarWhite)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPositionDialog = false }) {
                    Text("Закрыть", color = TaskbarOrange)
                }
            }
        )
    }
}
