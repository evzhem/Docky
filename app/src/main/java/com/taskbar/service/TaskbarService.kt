package com.taskbar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.taskbar.MainActivity
import com.taskbar.R
import com.taskbar.data.AppInfo
import com.taskbar.data.AppsRepository
import com.taskbar.data.PanelPosition
import com.taskbar.data.SettingsRepository
import com.taskbar.data.SettingsUiState
import com.taskbar.data.toUiState
import com.taskbar.data.taskbarDataStore
import com.taskbar.ui.theme.TaskbarOrange
import com.taskbar.ui.theme.TaskbarHeader
import com.taskbar.ui.theme.TaskbarTheme
import com.taskbar.ui.theme.TaskbarWhite
import com.taskbar.util.FreeformLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground-сервис Taskbar.
 *
 * Рисует поверх других приложений:
 *  1) плавающую панель (TYPE_APPLICATION_OVERLAY) с кнопкой меню и
 *     недавними приложениями — панель можно перетаскивать;
 *  2) окно меню приложений (сетка всех установленных программ + поиск).
 */
class TaskbarService : Service() {

    private lateinit var windowManager: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var barView: ComposeView? = null
    private var barOwner: ServiceLayoutOwner? = null
    private var drawerView: ComposeView? = null
    private var drawerOwner: ServiceLayoutOwner? = null

    private val overlay = OverlayStateHolder()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        // Следим за настройками и сразу применяем их к панели
        scope.launch {
            applicationContext.taskbarDataStore.data
                .map { it.toUiState() }
                .collect { settings ->
                    // Если положение панели изменилось — пересоздаём окно
                    // с другой гравитацией (для первичной настройки сверху тоже)
                    val previousPosition = overlay.settings?.panelPosition
                    overlay.applySettings(settings)
                    if (previousPosition != null &&
                        previousPosition != settings.panelPosition
                    ) {
                        rebuildBar()
                    }
                }
        }

        // Загружаем приложения и недавние программы в фоне
        scope.launch {
            val repository = AppsRepository()
            val (apps, recent) = withContext(Dispatchers.IO) {
                repository.getInstalledApps(applicationContext) to
                    repository.getRecentApps(applicationContext, limit = 6)
            }
            overlay.allApps = apps
            overlay.recentApps = recent.map { it.app }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                teardownOverlays()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // Сначала переводим службу в foreground
                try {
                    startForegroundCompat()
                } catch (t: Throwable) {
                    Log.e(TAG, "startForeground не удался", t)
                    toast("Taskbar: не удалось запустить службу — ${t.javaClass.simpleName}: ${t.message}")
                    updateNotification("Ошибка службы: ${t.message}", error = true)
                    stopSelf()
                    return START_NOT_STICKY
                }
                // Без разрешения на оверлей сервис работать не может
                if (!Settings.canDrawOverlays(this)) {
                    toast("Taskbar: нет разрешения «Поверх других приложений»")
                    stopSelf()
                    return START_NOT_STICKY
                }
                showBar()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        teardownOverlays()
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Управление окнами панели и меню
    // ------------------------------------------------------------------

    private fun showBar() {
        if (barView != null) return
        try {
            val owner = ServiceLayoutOwner()
            val view = createComposeView(owner) {
                TaskbarBar(
                    state = overlay,
                    onOpenMenu = { openDrawer() },
                    onLaunchApp = { launchFromOverlay(it) },
                    onDrag = { dx, dy -> moveBar(dx, dy) }
                )
            }
            windowManager.addView(view, barLayoutParams(overlay.position))
            barView = view
            barOwner = owner
            Log.i(TAG, "Панель Taskbar добавлена в WindowManager")
            toast("Taskbar включён: панель внизу экрана")
            updateNotification("Панель Taskbar активна", error = false)
        } catch (t: Throwable) {
            // Самая частая причина: addView/ComposeView бросают исключение
            Log.e(TAG, "Не удалось добавить панель", t)
            val msg = "${t.javaClass.simpleName}: ${t.message}"
            toast("Taskbar: панель не показана — $msg")
            updateNotification("Ошибка панели: $msg", error = true)
        }
    }

    /** Toast на главном потоке. */
    private fun toast(message: String) {
        android.os.Handler(mainLooper).post {
            runCatching { Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show() }
        }
    }

    /** Обновляет текст уведомления службы (виден даже без оверлея). */
    private fun updateNotification(text: String, error: Boolean) {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(text))
        } catch (t: Throwable) {
            Log.e(TAG, "Не удалось обновить уведомление", t)
        }
    }

    private fun rebuildBar() {
        barView?.let { view ->
            runCatching { windowManager.removeView(view) }
            barOwner?.onDestroy()
            barView = null
            barOwner = null
        }
        showBar()
    }

    /** Перетаскивание панели: меняем смещение окна в WindowManager. */
    private fun moveBar(dx: Float, dy: Float) {
        val view = barView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.x += dx.toInt()
        // При гравитации BOTTOM положительный Y уводит окно ВВЕРХ
        if (overlay.position == PanelPosition.BOTTOM) {
            params.y -= dy.toInt()
        } else {
            params.y += dy.toInt()
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun openDrawer() {
        if (drawerView != null) return
        overlay.searchQuery = ""
        overlay.drawerVisible = true

        val owner = ServiceLayoutOwner()
        val view = createComposeView(owner) {
            TaskbarDrawer(
                state = overlay,
                onClose = { closeDrawer() },
                onLaunchApp = { launchFromOverlay(it) }
            )
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Окно получает фокус (нужно для поля поиска и клавиатуры),
            // раскрывается на весь экран с затемнением-«scrim».
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            title = "TaskbarDrawer"
        }
        runCatching { windowManager.addView(view, params) }
            .onFailure {
                Log.e(TAG, "Не удалось открыть меню", it)
                toast("Taskbar: меню не открылось — ${it.javaClass.simpleName}: ${it.message}")
            }
        drawerView = view
        drawerOwner = owner
    }

    private fun closeDrawer() {
        drawerView?.let { view ->
            runCatching { windowManager.removeView(view) }
            drawerOwner?.onDestroy()
        }
        drawerView = null
        drawerOwner = null
        overlay.drawerVisible = false
    }

    /** Запуск приложения из оверлея с учётом настроек Freeform. */
    private fun launchFromOverlay(app: AppInfo) {
        val settings = overlay.settings ?: return
        scope.launch {
            var boundsOverride: android.graphics.Rect? = null
            val willUseFreeform = settings.freeformEnabled &&
                !(settings.gamesFullscreen && app.isGame)

            // Фича «Сохранять размеры окон»: запоминаем размер для пакета
            if (willUseFreeform && settings.saveWindowSizes) {
                val repository = SettingsRepository(applicationContext)
                val saved = repository.getSavedWindowSize(app.packageName)
                boundsOverride = if (saved != null) {
                    FreeformLauncher.boundsForSize(applicationContext, saved.first, saved.second)
                } else {
                    FreeformLauncher.defaultBounds(applicationContext, settings.windowSize)
                        .also { repository.saveWindowSize(app.packageName, it.width(), it.height()) }
                }
            }

            FreeformLauncher.launch(
                context = applicationContext,
                packageName = app.packageName,
                freeformEnabled = settings.freeformEnabled,
                alwaysNewWindow = settings.alwaysNewWindow,
                gamesFullscreen = settings.gamesFullscreen,
                isGame = app.isGame,
                windowSize = settings.windowSize,
                debugLogging = settings.debugLogging,
                boundsOverride = boundsOverride,
            )
            if (settings.closeAfterLaunch) {
                closeDrawer()
            }
        }
    }

    private fun teardownOverlays() {
        closeDrawer()
        barView?.let { view ->
            runCatching { windowManager.removeView(view) }
            barOwner?.onDestroy()
        }
        barView = null
        barOwner = null
    }

    // ------------------------------------------------------------------
    // Параметры окон
    // ------------------------------------------------------------------

    private fun barLayoutParams(position: PanelPosition) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // NOT_FOCUSABLE — панели не нужен клавиатурный фокус;
        // NOT_TOUCH_MODAL — касания мимо панели уходят нижнему приложению.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = if (position == PanelPosition.BOTTOM) {
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        } else {
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        // Отступ от края экрана
        y = 48
        title = "TaskbarBar"
    }

    /**
     * ComposeView вне Activity: вручную проставляем владельцев
     * Lifecycle/SavedState/ViewModelStore.
     */
    private fun createComposeView(
        owner: ServiceLayoutOwner,
        content: @Composable () -> Unit
    ): ComposeView {
        val view = ComposeView(this)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewCompositionStrategy(
            androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow
        )
        owner.onCreate()
        view.setContent {
            TaskbarTheme {
                content()
            }
        }
        return view
    }

    // ------------------------------------------------------------------
    // Уведомление foreground-сервиса
    // ------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String = "Панель Taskbar активна"): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TaskbarService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_taskbar)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_stat_taskbar, "Остановить", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        private const val TAG = "TaskbarService"
        private const val CHANNEL_ID = "taskbar_panel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.taskbar.action.START"
        const val ACTION_STOP = "com.taskbar.action.STOP"
    }
}

// ======================================================================
// Compose-состояние и UI оверлея
// ======================================================================

/**
 * Состояние оверлея, наблюдаемое Compose-композицией.
 */
class OverlayStateHolder {
    var settings: SettingsUiState? by mutableStateOf(null)
        private set

    var opacity: Float by mutableStateOf(0.9f)
    var iconSizeDp: Int by mutableStateOf(48)
    var position: PanelPosition by mutableStateOf(PanelPosition.BOTTOM)
    var closeAfterLaunch: Boolean by mutableStateOf(true)

    var recentApps: List<AppInfo> by mutableStateOf(emptyList())
    var allApps: List<AppInfo> by mutableStateOf(emptyList())

    var drawerVisible: Boolean by mutableStateOf(false)
    var searchQuery: String by mutableStateOf("")

    fun applySettings(value: SettingsUiState) {
        settings = value
        opacity = value.panelOpacity / 100f
        iconSizeDp = value.panelIconSize
        position = value.panelPosition
        closeAfterLaunch = value.closeAfterLaunch
    }
}

/**
 * Плавающая панель Taskbar: кнопка «меню приложений» + недавние приложения.
 * Корневой элемент — Row с WRAP_CONTENT: окно занимает только саму панель,
 * касания мимо неё проходят к приложению под оверлеем.
 */
@Composable
private fun TaskbarBar(
    state: OverlayStateHolder,
    onOpenMenu: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            // Прозрачность панели настраивается в «Внешний вид»
            .background(Color(0xFF2E2E2E).copy(alpha = state.opacity))
            // Перетаскивание всей панели
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка открытия меню приложений
        Box(
            modifier = Modifier
                .size(state.iconSizeDp.dp)
                .clip(CircleShape)
                .background(TaskbarHeader)
                .clickable { onOpenMenu() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = "Меню приложений",
                tint = TaskbarWhite,
                modifier = Modifier.size((state.iconSizeDp * 0.55f).dp)
            )
        }

        // Разделитель
        Box(
            modifier = Modifier
                .width(1.dp)
                .height((state.iconSizeDp * 0.8f).dp)
                .background(TaskbarWhite.copy(alpha = 0.25f))
        )

        // Недавние приложения
        state.recentApps.take(5).forEach { app ->
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier
                    .size(state.iconSizeDp.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onLaunchApp(app) }
            )
        }
    }
}

/**
 * Меню приложений: затемнение-«scrim» + панель снизу с поиском и сеткой
 * всех установленных приложений.
 */
@Composable
private fun TaskbarDrawer(
    state: OverlayStateHolder,
    onClose: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
) {
    val noIndication = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            // Клик по затемнению закрывает меню
            .clickable(
                interactionSource = noIndication,
                indication = null,
                onClick = onClose
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF2E2E2E))
                // Клик внутри панели не должен закрывать меню
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .imePadding()
                .padding(16.dp)
        ) {
            // Шапка меню
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Меню приложений",
                    color = TaskbarWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Закрыть меню",
                        tint = TaskbarWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Поиск
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { state.searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TaskbarWhite)
                },
                placeholder = { Text("Поиск приложений", color = Color(0xFFB0B0B0)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TaskbarWhite,
                    unfocusedTextColor = TaskbarWhite,
                    cursorColor = TaskbarOrange,
                    focusedBorderColor = TaskbarOrange,
                    unfocusedBorderColor = TaskbarWhite.copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF3A3A3A),
                    unfocusedContainerColor = Color(0xFF3A3A3A),
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Сетка приложений
            val query = state.searchQuery.trim()
            val apps = if (query.isEmpty()) {
                state.allApps
            } else {
                state.allApps.filter {
                    it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(68.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items = apps, key = { it.packageName }) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLaunchApp(app) }
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            color = TaskbarWhite,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
