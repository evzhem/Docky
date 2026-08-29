package com.taskbar.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskbar.ui.components.RowDivider
import com.taskbar.ui.components.ScreenScaffold
import com.taskbar.ui.components.SettingsCard
import com.taskbar.ui.theme.TaskbarOrange
import com.taskbar.ui.theme.TaskbarSecondary
import com.taskbar.ui.theme.TaskbarWhite
import com.taskbar.util.hasUsageAccess
import com.taskbar.util.rememberGrantedState
import com.taskbar.util.requestUsageAccess
import com.taskbar.viewmodel.AppsViewModel
import java.text.DateFormat
import java.util.Date

/**
 * Экран «Недавние приложения»: список из UsageStatsManager.
 * Если доступ к статистике использования не выдан — показываем инструкцию.
 */
@Composable
fun RecentAppsScreen(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    vm: AppsViewModel = viewModel(),
) {
    val recent by vm.recentApps.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val usageGranted = rememberGrantedState { hasUsageAccess(it) }

    // Обновляем список каждый раз при возврате на экран с выданным доступом
    LaunchedEffect(usageGranted) {
        if (usageGranted) vm.loadRecentApps()
    }

    ScreenScaffold(
        title = "Недавние приложения",
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        onBack = onBack
    ) {
        if (!usageGranted) {
            // ---- Нет доступа к использованию ----
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.QueryStats,
                        contentDescription = null,
                        tint = TaskbarOrange,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Нужен доступ к истории использования",
                        color = TaskbarWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Taskbar использует PACKAGE_USAGE_STATS " +
                            "(UsageStatsManager), чтобы показывать недавние приложения " +
                            "на панели и в этом списке. Нажмите кнопку ниже и разрешите " +
                            "доступ для Taskbar в системных настройках.",
                        color = TaskbarSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { requestUsageAccess(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskbarOrange
                        )
                    ) {
                        Icon(Icons.Filled.QueryStats, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Выдать доступ")
                    }
                }
            }
        } else {
            // ---- Список недавних приложений ----
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = TaskbarWhite
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        "Последние использованные приложения",
                        color = TaskbarWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Обновить",
                        tint = TaskbarOrange,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { vm.loadRecentApps() }
                    )
                }
                RowDivider()

                if (recent.isEmpty()) {
                    EmptyRecentList()
                } else {
                    recent.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.launchApp(item.app) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = item.app.icon.asImageBitmap(),
                                contentDescription = item.app.label,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.app.label,
                                    color = TaskbarWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.app.packageName,
                                    color = TaskbarSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = DateFormat.getDateTimeInstance(
                                    DateFormat.SHORT,
                                    DateFormat.SHORT
                                ).format(Date(item.lastUsed)),
                                color = TaskbarSecondary
                            )
                        }
                        if (index != recent.lastIndex) RowDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Нажатие на приложение запускает его — в режиме Freeform, " +
                    "если он включён и приложение не является игрой.",
                color = TaskbarSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun EmptyRecentList() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = null,
                tint = TaskbarSecondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Пока нет данных. Откройте несколько приложений и обновите список.",
                color = TaskbarSecondary
            )
        }
    }
}
