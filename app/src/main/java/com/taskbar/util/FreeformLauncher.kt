package com.taskbar.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import com.taskbar.data.WindowSizePref

/**
 * Запуск приложений в режиме Freeform.
 *
 * Границы окна задаются скрытым методом ActivityOptions.setLaunchBounds(Rect),
 * который вызываем через reflection (он отсутствует в публичном SDK как
 * гарантированно доступный на всех версиях).
 *
 * Чтобы система учла границы, на устройстве должны быть включены флаги:
 *   adb shell settings put global enable_freeform_support 1
 *   adb shell settings put global force_resizable_activities 1
 */
object FreeformLauncher {

    private const val TAG = "FreeformLauncher"

    /**
     * Запускает [packageName] в свободном окне с заданными границами.
     *
     * @param freeformEnabled включён ли режим Freeform в настройках Taskbar
     * @param alwaysNewWindow всегда создавать новое окно (FLAG_ACTIVITY_MULTIPLE_TASK)
     * @param gamesFullscreen игры запускать на весь экран, без Freeform
     * @param isGame является ли приложение игрой
     * @param windowSize выбранный размер окна по умолчанию
     */
    fun launch(
        context: Context,
        packageName: String,
        freeformEnabled: Boolean,
        alwaysNewWindow: Boolean,
        gamesFullscreen: Boolean,
        isGame: Boolean,
        windowSize: WindowSizePref,
        debugLogging: Boolean = false,
        boundsOverride: Rect? = null,
    ): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false

        val useFreeform = freeformEnabled && !(gamesFullscreen && isGame)

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (useFreeform) {
            if (alwaysNewWindow) {
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            val options = ActivityOptions.makeBasic()
            // Сохранённый размер окна приоритетнее размера по умолчанию
            val bounds = boundsOverride ?: defaultBounds(context, windowSize)
            setLaunchBoundsViaReflection(options, bounds)
            if (debugLogging) {
                Log.d(TAG, "Freeform-запуск $packageName, bounds=$bounds")
            }
            return runCatching {
                context.startActivity(intent, options.toBundle())
                true
            }.getOrElse {
                Log.e(TAG, "Не удалось запустить $packageName в Freeform", it)
                false
            }
        }

        return runCatching {
            context.startActivity(intent)
            true
        }.getOrElse {
            Log.e(TAG, "Не удалось запустить $packageName", it)
            false
        }
    }

    /**
     * Границы окна по центру экрана согласно выбранному размеру.
     */
    fun defaultBounds(context: Context, size: WindowSizePref): Rect {
        val (screenWidth, screenHeight) = screenSize(context)
        val width = (screenWidth * size.factor).toInt()
        val height = (screenHeight * size.factor).toInt()
        return boundsForSize(context, width, height)
    }

    /**
     * Границы окна заданных размеров, отцентрованные на экране
     * (используется для сохранённых «Сохранять размеры окон» размеров).
     */
    fun boundsForSize(context: Context, width: Int, height: Int): Rect {
        val (screenWidth, screenHeight) = screenSize(context)
        val w = width.coerceAtMost(screenWidth)
        val h = height.coerceAtMost(screenHeight)
        val left = ((screenWidth - w) / 2).coerceAtLeast(0)
        val top = ((screenHeight - h) / 2).coerceAtLeast(0)
        return Rect(left, top, left + w, top + h)
    }

    /** Размер экрана с учётом вырезов/системных баров. */
    @Suppress("DEPRECATION")
    private fun screenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = android.util.DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    /**
     * Вызов скрытого метода ActivityOptions.setLaunchBounds(Rect) через reflection.
     */
    private fun setLaunchBoundsViaReflection(options: ActivityOptions, bounds: Rect) {
        runCatching {
            val method = ActivityOptions::class.java
                .getMethod("setLaunchBounds", Rect::class.java)
            method.isAccessible = true
            method.invoke(options, bounds)
        }.onFailure {
            Log.e(TAG, "setLaunchBounds недоступен через reflection", it)
        }
    }

    // ---- Состояние системных флагов Freeform/Desktop (читаются напрямую) ----

    private fun globalFlag(context: Context, name: String): Boolean = runCatching {
        Settings.Global.getString(context.contentResolver, name) == "1"
    }.getOrDefault(false)

    fun isFreeformSupportEnabled(context: Context): Boolean =
        globalFlag(context, "enable_freeform_support")

    fun isForceResizableEnabled(context: Context): Boolean =
        globalFlag(context, "force_resizable_activities")

    fun isDesktopModeEnabled(context: Context): Boolean =
        globalFlag(context, "force_desktop_mode_on_external_displays")

    /** Команды для включения Freeform через ADB. */
    const val FREEFORM_ADB_COMMANDS =
        "adb shell settings put global enable_freeform_support 1\n" +
            "adb shell settings put global force_resizable_activities 1"

    /** Команда для включения Desktop mode через ADB. */
    const val DESKTOP_ADB_COMMAND =
        "adb shell settings put global force_desktop_mode_on_external_displays 1"
}
