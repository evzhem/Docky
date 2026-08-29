package com.taskbar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Каркас экрана: фирменная шапка + вертикально прокручиваемое содержимое.
 * Инсеты системных баров действуют только когда окно развёрнуто на весь
 * экран; у плавающего окна отступы уже есть (см. MainActivity).
 */
@Composable
fun ScreenScaffold(
    title: String,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: (() -> Unit)? = null,
    onHelp: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TaskbarHeader(
            title = title,
            isFullscreen = isFullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onBack = onBack,
            onHelp = onHelp,
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
        ) {
            content()
            // Отступ под системную навигацию (0 в плавающем режиме)
            Box(
                modifier = Modifier
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
            Box(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}
