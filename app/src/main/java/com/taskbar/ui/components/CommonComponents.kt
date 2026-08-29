package com.taskbar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskbar.ui.theme.TaskbarHeader
import com.taskbar.ui.theme.TaskbarOrange
import com.taskbar.ui.theme.TaskbarSecondary
import com.taskbar.ui.theme.TaskbarSurface
import com.taskbar.ui.theme.TaskbarWhite

/**
 * Шапка экрана в стиле Taskbar: индиго #3F51B5, белые текст и иконки.
 * Кнопка fullscreen есть на всех экранах (разворачивает то же окно).
 */
@Composable
fun TaskbarHeader(
    title: String,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onHelp: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TaskbarHeader)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            HeaderIcon(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                description = "Назад",
                onClick = onBack
            )
        }

        Text(
            text = title,
            color = TaskbarWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        if (onHelp != null) {
            HeaderIcon(
                icon = androidx.compose.material.icons.Icons.Filled.HelpOutline,
                description = "Справка",
                onClick = onHelp
            )
        }

        HeaderIcon(
            icon = if (isFullscreen) {
                androidx.compose.material.icons.Icons.Filled.FullscreenExit
            } else {
                androidx.compose.material.icons.Icons.Filled.Fullscreen
            },
            description = if (isFullscreen) "Оконный режим" else "На весь экран",
            onClick = onToggleFullscreen
        )

        if (checked != null && onCheckedChange != null) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TaskbarWhite,
                    checkedTrackColor = TaskbarOrange,
                    uncheckedThumbColor = TaskbarWhite,
                    uncheckedTrackColor = Color(0x55000000),
                    uncheckedBorderColor = TaskbarSecondary,
                )
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
private fun HeaderIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = TaskbarWhite,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Оранжевый заголовок секции (#FF7043). */
@Composable
fun OrangeSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TaskbarOrange,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 16.dp, bottom = 6.dp)
    )
}

/** Карточка с тёмным фоном и скруглением. */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TaskbarSurface)
    ) {
        content()
    }
}

/**
 * Строка настройки: иконка + заголовок + описание + опциональный элемент справа.
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null && enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TaskbarWhite.copy(alpha = alpha),
            modifier = Modifier.size(26.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TaskbarWhite.copy(alpha = alpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TaskbarSecondary.copy(alpha = alpha),
                    fontSize = 13.sp
                )
            }
        }
        if (trailing != null) {
            Box(modifier = Modifier.padding(start = 8.dp)) {
                // Содержимое справа само учитывает disabled-состояние
                trailing()
            }
        }
    }
}

/** Тонкий разделитель внутри карточек. */
@Composable
fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 54.dp)
            .height(1.dp)
            .background(Color(0x22FFFFFF))
    )
}
