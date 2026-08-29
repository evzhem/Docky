# Правила ProGuard/R8 для Taskbar.
# Минификация для release отключена в build.gradle.kts,
# файл оставлен стандартным для совместимости со сборкой.

# Сервис и ресивер вызываются системой по имени — не переименовываем
-keep class com.taskbar.service.** { *; }
-keep class com.taskbar.MainActivity { *; }
