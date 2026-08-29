package com.taskbar.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Информация о приложении для списков/панели.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
    val isGame: Boolean = false,
)

/**
 * Недавно использованное приложение с меткой времени последнего запуска.
 */
data class RecentAppInfo(
    val app: AppInfo,
    val lastUsed: Long,
)

/**
 * Репозиторий: список установленных приложений (PackageManager)
 * и список недавних приложений (UsageStatsManager).
 */
class AppsRepository {

    /**
     * Все приложения с иконкой лаунчера, отсортированные по названию.
     * Видимость пакетов обеспечивается <queries> в AndroidManifest.
     */
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                // Себя в список не включаем
                if (pkg == context.packageName) return@mapNotNull null
                runCatching {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    AppInfo(
                        packageName = pkg,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo).toBitmap(ICON_SIZE, ICON_SIZE),
                        isGame = appInfo.category == ApplicationInfo.CATEGORY_GAME,
                    )
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Недавние приложения через UsageStatsManager (событие MOVE_TO_FOREGROUND
     * за последние [WINDOW_DAYS] дней). Требуется доступ PACKAGE_USAGE_STATS.
     */
    fun getRecentApps(context: Context, limit: Int = 10): List<RecentAppInfo> {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(now - WINDOW_DAYS * DAY_MS, now)

        // Сохраняем порядок «последнее — позже всех» и время последнего запуска
        val lastUsed = LinkedHashMap<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                event.packageName != context.packageName
            ) {
                lastUsed[event.packageName] = event.timeStamp
            }
        }

        val pm = context.packageManager
        return lastUsed.toList().asReversed().mapNotNull { (pkg, time) ->
            // Оставляем только то, что реально можно запустить
            pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
            runCatching {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                RecentAppInfo(
                    app = AppInfo(
                        packageName = pkg,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo).toBitmap(ICON_SIZE, ICON_SIZE),
                        isGame = appInfo.category == ApplicationInfo.CATEGORY_GAME,
                    ),
                    lastUsed = time,
                )
            }.getOrNull()
        }.take(limit)
    }

    private companion object {
        const val ICON_SIZE = 144
        const val WINDOW_DAYS = 5L
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
