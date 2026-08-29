package com.taskbar.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.taskbar.service.TaskbarService

/**
 * Простой помощник для запуска/остановки foreground-сервиса TaskbarService.
 */
object TaskbarServiceController {

    fun start(context: Context) {
        if (!canDrawOverlays(context)) return
        val intent = Intent(context, TaskbarService::class.java).apply {
            action = TaskbarService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, TaskbarService::class.java).apply {
            action = TaskbarService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
    }

    fun restart(context: Context) {
        stop(context)
        // Небольшая задержка, чтобы старый сервис успел снять оверлеи
        android.os.Handler(context.mainLooper).postDelayed({ start(context) }, 400L)
    }
}
