# Taskbar (Docky)

Аналог приложения Taskbar для Android: меню приложений, список недавних
приложений, режим Freeform (свободные окна), desktop mode. Само приложение
открывается как плавающее окно (~85% × 80% экрана, по центру, без
перетаскивания) и разворачивается на весь экран кнопкой fullscreen.

## Стек

- Kotlin 2.0.21 (без Java), Gradle Kotlin DSL + Version Catalog, JDK 17
- minSdk 28, targetSdk/compileSdk 36
- Jetpack Compose + Material 3 + material-icons-extended
- Single Activity + MVVM (ViewModel + StateFlow)
- DataStore Preferences, Coroutines
- AGP 8.9.2, Gradle 8.11.1

## Сборка

Открыть проект в Android Studio (Narwhal и новее) — Gradle-синхронизация
скачает дистрибутив Gradle и сгенерирует `gradle-wrapper.jar`. Либо из
консоли с установленным JDK 17 и Android SDK:

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Системные возможности

- Плавающая панель: foreground-сервис `TaskbarService`, окно
  `TYPE_APPLICATION_OVERLAY` (разрешение «Поверх других приложений» —
  `SYSTEM_ALERT_WINDOW`, экран `ACTION_MANAGE_OVERLAY_PERMISSION`).
  Панель перетаскивается, на ней — кнопка меню приложений и недавние
  приложения; меню — сетка всех установленных программ с поиском.
- Недавние приложения: `UsageStatsManager` (PACKAGE_USAGE_STATS),
  экран `ACTION_USAGE_ACCESS_SETTINGS`.
- Список приложений: `PackageManager` + `<queries>` в манифесте.
- Freeform: запуск Activity в заданных границах через reflection
  `ActivityOptions.setLaunchBounds(Rect)`; инструкция по adb-флагам
  `enable_freeform_support` / `force_resizable_activities` на экране
  «Режим Freeform» (кнопка «?»), состояние флагов читается из
  `Settings.Global`.
- Автозапуск: `BroadcastReceiver` на `BOOT_COMPLETED` /
  `MY_PACKAGE_REPLACED`.
- Уведомления: `POST_NOTIFICATIONS` + канал foreground-сервиса
  (тип `specialUse` на Android 14+).

## Дизайн

Тёмная тема: фон `#2E2E2E`, шапки `#3F51B5`, оранжевые заголовки `#FF7043`,
вторичный текст серый, основной текст и иконки белые. Скругление окна 20dp,
тень, полупрозрачный тёмный фон (`windowIsTranslucent=true`,
`windowBackground=transparent`, `backgroundDimEnabled=false`).

## Структура

```
app/src/main/java/com/taskbar/
├── MainActivity.kt              # плавающее окно 85%×80%, fullspace-переключение
├── data/                        # DataStore-настройки, репозитории приложений
├── viewmodel/                   # SettingsViewModel, AppsViewModel (StateFlow)
├── service/                     # TaskbarService (оверлей), BootReceiver
├── ui/
│   ├── theme/                   # цвета/типографика Material 3
│   ├── navigation/              # маршруты
│   ├── components/              # шапка, строки настроек, карточки
│   └── screens/                 # 7 экранов
└── util/                        # права, Freeform-launcher, контрол сервиса
```
