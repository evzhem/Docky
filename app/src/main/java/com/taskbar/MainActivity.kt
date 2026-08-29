package com.taskbar

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.taskbar.ui.TaskbarApp
import com.taskbar.ui.theme.TaskbarTheme

/**
 * Единственная Activity приложения.
 *
 * Открывается как ПЛАВАЮЩЕЕ ОКНО: ~85% ширины и ~80% высоты экрана,
 * по центру, без перетаскивания. Прозрачная тема (windowIsTranslucent,
 * прозрачный фон, без затемнения) — за окном виден рабочий стол.
 *
 * Кнопка fullscreen разворачивает то же окно на весь экран и обратно:
 * меняются только WindowManager.LayoutParams, Activity не пересоздаётся.
 */
class MainActivity : ComponentActivity() {

    /** Текущий режим окна: false — плавающее, true — на весь экран. */
    private var isFullscreen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Страховка: прозрачный фон окна и отсутствие затемнения позади
        window.setBackgroundDrawable(ColorDrawable(0))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        // Edge-to-edge: контент сам учитывает системные бары (в плавающем
        // режиме инсеты равны нулю, в fullscreen — реальная высота баров)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            // Светлые иконки системных баров на тёмном фоне Taskbar
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            // Прячем клавиатуру при переключении оконного режима
            hide(WindowInsetsCompat.Type.ime())
        }

        setContent {
            TaskbarTheme {
                TaskbarApp(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = ::toggleWindowMode
                )
            }
        }

        // Стартовое окно — плавающее, 85% × 80%
        applyWindowMode(fullscreen = false)
    }

    /** Переключение «плавающее окно ↔ весь экран» для ТОЙ ЖЕ Activity. */
    private fun toggleWindowMode() {
        isFullscreen = !isFullscreen
        applyWindowMode(isFullscreen)
    }

    /**
     * Пересчитывает и применяет WindowManager.LayoutParams окна.
     * Окно всегда остаётся «плавающим» (тип окна не меняется), меняются
     * только размеры и гравитация.
     */
    private fun applyWindowMode(fullscreen: Boolean) {
        val params = window.attributes

        if (fullscreen) {
            // Во весь экран
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 0
            params.y = 0
        } else {
            // Плавающее окно: 85% ширины, 80% высоты, строго по центру
            val (screenWidth, screenHeight) = displaySize()
            params.width = (screenWidth * WINDOW_WIDTH_RATIO).toInt()
            params.height = (screenHeight * WINDOW_HEIGHT_RATIO).toInt()
            params.gravity = Gravity.CENTER
            params.x = 0
            params.y = 0
        }

        window.attributes = params
    }

    /** Реальный размер экрана с учётом системных баров и вырезов. */
    @Suppress("DEPRECATION")
    private fun displaySize(): Pair<Int, Int> {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private companion object {
        const val WINDOW_WIDTH_RATIO = 0.85f
        const val WINDOW_HEIGHT_RATIO = 0.80f
    }
}
