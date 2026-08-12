# FlowClock — Aesthetic Task & Habit Tracker Widget

A minimal, Material 3 Android app + home-screen widget for tracking daily
habits, built with Kotlin, Jetpack Compose, Room, and WorkManager.

## Requirements
- Android Studio Ladybug or newer
- JDK 17
- compileSdk / targetSdk 36, minSdk 26

## How it's compliant with strict Play policy
- **No exact alarms, no foreground service.** Widget interactivity is driven
  by `PendingIntent`s (tap-to-toggle) and a `PeriodicWorkRequest` (every 15
  minutes) via WorkManager.
- **Only permission requested:** `RECEIVE_BOOT_COMPLETED`, used solely to
  re-arm the WorkManager job after a reboot.
- **Dynamic theming:** the app uses Material 3 Dynamic Color on Android 12+;
  the widget's colors come from `values` / `values-night` resource
  qualifiers, so it automatically matches system light/dark mode.

## Project structure
```
app/src/main/java/com/flowclock/app/
├── FlowClockApp.kt              Application class — schedules WorkManager
├── MainActivity.kt              Compose UI: habit list, add/edit dialog
├── data/                        Room entity, DAO, database, repository
├── ui/                          ViewModel + Material 3 theme
├── util/DateUtils.kt            Daily-reset date helper
├── widget/
│   ├── HabitWidgetProvider.kt   AppWidgetProvider — layout + toggle logic
│   ├── HabitWidgetService.kt    RemoteViewsService entry point
│   ├── HabitRemoteViewsFactory.kt  Builds each widget row from Room data
│   └── BootReceiver.kt          Re-schedules refresh after reboot
└── work/
    ├── WidgetUpdateWorker.kt    CoroutineWorker, runs every 15 minutes
    └── WidgetUpdateScheduler.kt Enqueues the unique periodic work request
```

## Building locally
```bash
gradle assembleDebug
# or, once you've generated a wrapper:
# gradle wrapper --gradle-version 8.9
# ./gradlew assembleDebug
```

## Building via GitHub Actions
Push to `main` (or run the workflow manually) and `.github/workflows/build.yml`
will produce debug and unsigned-release APKs as downloadable workflow
artifacts — no committed Gradle wrapper jar required, since the workflow
uses `gradle/actions/setup-gradle` to provision Gradle directly.
