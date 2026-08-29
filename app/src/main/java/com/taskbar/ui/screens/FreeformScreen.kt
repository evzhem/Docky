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
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskbar.data.WindowSizePref
import com.taskbar.ui.components.OrangeSectionHeader
import com.taskbar.ui.components.RowDivider
import com.taskbar.ui.components.ScreenScaffold
import com.taskbar.ui.components.SettingRow
import com.taskbar.ui.components.SettingsCard
import com.taskbar.ui.theme.TaskbarOrange
import com.taskbar.ui.theme.TaskbarSecondary
import com.taskbar.ui.theme.TaskbarSurface
import com.taskbar.ui.theme.TaskbarWhite
import com.taskbar.util.FreeformLauncher
import com.taskbar.viewmodel.SettingsViewModel

/**
 * Экран «Режим Freeform»: главный чекбокс, подопции (неактивны, пока
 * режим выключен), выбор размера окна по умолчанию, статус adb-флагов
 * и справка с инструкцией.
 */
@Composable
fun FreeformScreen(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showHelp by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }

    // Состояние системных флагов Freeform (перечитывается кнопкой)
    var freeformFlag by remember {
        mutableStateOf(FreeformLauncher.isFreeformSupportEnabled(context))
    }
    var resizableFlag by remember {
        mutableStateOf(FreeformLauncher.isForceResizableEnabled(context))
    }

    val freeformEnabled = state.freeformEnabled

    ScreenScaffold(
        title = "Режим Freeform",
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        onBack = onBack,
        onHelp = { showHelp = true },
        checked = freeformEnabled,
        onCheckedChange = vm::setFreeformEnabled
    ) {
        // ---- Главный переключатель режима ----
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CropSquare,
                    contentDescription = null,
                    tint = TaskbarWhite,
                    modifier = Modifier
                        .height(26.dp)
                        .padding(end = 14.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Режим Freeform", color = TaskbarWhite)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Разрешить Taskbar запускать приложения в режиме Freeform " +
                            "для большей схожести с ПК.",
                        color = TaskbarSecondary
                    )
                }
                Checkbox(
                    checked = freeformEnabled,
                    onCheckedChange = vm::setFreeformEnabled,
                    colors = CheckboxDefaults.colors(
                        checkedColor = TaskbarOrange,
                        uncheckedColor = TaskbarSecondary,
                        checkmarkColor = TaskbarWhite
                    )
                )
            }
        }

        // ---- Предупреждение об экспериментальной возможности ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = TaskbarOrange,
                modifier = Modifier.height(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Это экспериментальная возможность, могут быть проблемы " +
                    "со стандартным режимом многозадачности.",
                color = TaskbarSecondary
            )
        }

        OrangeSectionHeader(text = "Параметры Freeform")

        // ---- Подопции (серые и неактивные, пока режим выключен) ----
        SettingsCard {
            SwitchSubRow(
                title = "Сохранять размеры окон",
                subtitle = "при открытии окон с контекстного меню",
                checked = state.saveWindowSizes,
                enabled = freeformEnabled,
                onCheckedChange = vm::setSaveWindowSizes
            )
            RowDivider()
            SwitchSubRow(
                title = "Всегда открывать в новом окне",
                subtitle = "Всегда запускать приложение в новом окне (если можно)",
                checked = state.alwaysNewWindow,
                enabled = freeformEnabled,
                onCheckedChange = vm::setAlwaysNewWindow
            )
            RowDivider()
            SwitchSubRow(
                title = "Открывать игры в полный экран",
                subtitle = "Если приложение является игрой, не запускать его в режиме Freeform",
                checked = state.gamesFullscreen,
                enabled = freeformEnabled,
                onCheckedChange = vm::setGamesFullscreen
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.OpenInNew,
                title = "Размер окна по умолчанию",
                subtitle = state.windowSize.title,
                enabled = freeformEnabled,
                onClick = { showSizeDialog = true }
            )
        }

        OrangeSectionHeader(text = "Состояние системы")

        // ---- Статус adb-флагов ----
        SettingsCard {
            SettingRow(
                icon = Icons.Filled.CheckCircle,
                title = "enable_freeform_support",
                subtitle = if (freeformFlag) "Флаг включён" else "Флаг не включён",
                trailing = { FlagDot(freeformFlag) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.CheckCircle,
                title = "force_resizable_activities",
                subtitle = if (resizableFlag) "Флаг включён" else "Флаг не включён",
                trailing = { FlagDot(resizableFlag) }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Filled.Refresh,
                title = "Проверить состояние флагов",
                subtitle = "Перечитать значения глобальных настроек",
                onClick = {
                    freeformFlag = FreeformLauncher.isFreeformSupportEnabled(context)
                    resizableFlag = FreeformLauncher.isForceResizableEnabled(context)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // ---- Диалог выбора размера окна ----
    if (showSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSizeDialog = false },
            title = { Text("Размер окна по умолчанию", color = TaskbarWhite) },
            containerColor = TaskbarSurface,
            text = {
                Column {
                    WindowSizePref.entries.forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.windowSize == size,
                                onClick = {
                                    vm.setWindowSize(size)
                                    showSizeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TaskbarOrange,
                                    unselectedColor = TaskbarSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(size.title, color = TaskbarWhite)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSizeDialog = false }) {
                    Text("Закрыть", color = TaskbarOrange)
                }
            }
        )
    }

    // ---- Справка с adb-инструкцией ----
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Как включить Freeform", color = TaskbarWhite) },
            containerColor = TaskbarSurface,
            text = {
                Column {
                    Text(
                        text = "Чтобы система позволяла запускать приложения в свободных " +
                            "окнах, выполните на компьютере с включённой отладкой по ADB:",
                        color = TaskbarSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = FreeformLauncher.FREEFORM_ADB_COMMANDS,
                        style = TextStyle(color = TaskbarWhite)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(FreeformLauncher.FREEFORM_ADB_COMMANDS))
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Скопировать команды")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "После перезапуска приложений свободные окна заработают. " +
                            "Taskbar задаёт границы окна через ActivityOptions.setLaunchBounds " +
                            "(вызов скрытого API через reflection).",
                        color = TaskbarSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("Понятно", color = TaskbarOrange)
                }
            }
        )
    }
}

/** Подопция Freeform с переключателем и неактивным (серым) состоянием. */
@Composable
private fun SwitchSubRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TaskbarWhite.copy(alpha = if (enabled) 1f else 0.4f),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TaskbarSecondary.copy(alpha = if (enabled) 1f else 0.4f),
                fontSize = 13.sp
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TaskbarWhite,
                checkedTrackColor = TaskbarOrange,
                uncheckedThumbColor = TaskbarWhite,
                uncheckedTrackColor = Color(0x55000000),
                disabledCheckedThumbColor = TaskbarWhite.copy(alpha = 0.4f),
                disabledCheckedTrackColor = TaskbarOrange.copy(alpha = 0.3f),
                disabledUncheckedThumbColor = TaskbarWhite.copy(alpha = 0.4f),
                disabledUncheckedTrackColor = Color(0x33000000),
            )
        )
    }
}

/** Цветной индикатор состояния флага: зелёный — включён, серый — нет. */
@Composable
private fun FlagDot(enabled: Boolean) {
    Text(
        text = "●",
        color = if (enabled) Color(0xFF4CAF50) else TaskbarSecondary,
        fontSize = 18.sp
    )
}
