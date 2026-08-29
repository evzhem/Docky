package com.taskbar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.taskbar.data.SettingsKeys
import com.taskbar.data.taskbarDataStore
import com.taskbar.util.TaskbarServiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Автозапуск панели Taskbar после перезагрузки устройства
 * (или обновления приложения), если включены соответствующие настройки.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        // goAsync() даёт время прочитать DataStore вне основного потока
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val prefs = context.taskbarDataStore.data.first()
                val taskbarEnabled = prefs[SettingsKeys.TASKBAR_ENABLED] ?: false
                val autostart = prefs[SettingsKeys.GENERAL_AUTOSTART] ?: true

                if (taskbarEnabled && autostart && Settings.canDrawOverlays(context)) {
                    TaskbarServiceController.start(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
