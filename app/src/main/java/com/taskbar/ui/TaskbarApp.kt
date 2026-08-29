package com.taskbar.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.taskbar.ui.navigation.TaskbarRoutes
import com.taskbar.ui.screens.AdvancedSettingsScreen
import com.taskbar.ui.screens.AppearanceScreen
import com.taskbar.ui.screens.DesktopModeScreen
import com.taskbar.ui.screens.FreeformScreen
import com.taskbar.ui.screens.GeneralSettingsScreen
import com.taskbar.ui.screens.MainScreen
import com.taskbar.ui.screens.RecentAppsScreen
import com.taskbar.ui.theme.TaskbarBackground

/**
 * Корневой composable: плавающая «карточка» со скруглением 20dp и тенью
 * (когда окно не на весь экран) + навигация между экранами настроек.
 *
 * Само окно Activity меньше экрана (см. MainActivity), а отступ вокруг
 * Surface нужен, чтобы тень рендерилась внутри окна и была видна.
 */
@Composable
fun TaskbarApp(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
) {
    val shape = if (isFullscreen) {
        RoundedCornerShape(0.dp)
    } else {
        RoundedCornerShape(20.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isFullscreen) 0.dp else 10.dp),
        shape = shape,
        color = TaskbarBackground,
        shadowElevation = if (isFullscreen) 0.dp else 8.dp,
    ) {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = TaskbarRoutes.MAIN,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
        ) {
            composable(TaskbarRoutes.MAIN) {
                MainScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(TaskbarRoutes.GENERAL) {
                GeneralSettingsScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(TaskbarRoutes.APPEARANCE) {
                AppearanceScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(TaskbarRoutes.RECENT) {
                RecentAppsScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(TaskbarRoutes.FREEFORM) {
                FreeformScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(TaskbarRoutes.DESKTOP) {
                DesktopModeScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(TaskbarRoutes.ADVANCED) {
                AdvancedSettingsScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
