# Routini - Daily Routine & Task Manager

**Routini** is a modern Android application designed to help users manage their daily routines and standalone tasks with a clean, intuitive interface. It leverages the latest Android development technologies to provide a robust and efficient user experience.

---

## ✨ Features

- **Routine Management**: Create multiple routines (e.g., "Morning Routine", "Workout") with specific recurring days.
- **Task Management**: 
    - Add tasks to routines with titles, descriptions, and specific times.
    - Create standalone tasks with specific dates and times.
- **Flexible Alarms & Notifications**:
    - For each task, choose between a silent **Notification** or a full **Alarm** with sound.
    - Select from system ringtones or pick a custom audio file from your device for alarms.
- **Smart Scheduling**: Alarms for recurring routines automatically reschedule for the next valid day, even if you mark today's task as complete.
- **Daily Reset**: A background worker runs every midnight to reset the completion status of your daily tasks, ensuring you start each day fresh.
- **Modern UI**: A swipeable interface allows you to switch between the Routines and Standalone Tasks screens, built entirely with Jetpack Compose.
---

## 🛠️ Technology Stack

This project is a showcase of modern Android development practices:

- **UI**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) for a declarative, modern UI.
- **Architecture**: Follows a standard ViewModel architecture pattern.
- **Navigation**: [Compose Navigation](https://developer.android.com/jetpack/compose/navigation) for all in-app screen transitions.
- **Persistence**: [Room](https://developer.android.com/training/data-storage/room) for local database storage of routines and tasks.
- **Asynchronous Operations**: Kotlin [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://kotlinlang.org/docs/flow.html) for managing background threads and data streams.
- **Background Processing**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for reliable, guaranteed background work (daily task resets).
- **Alarm Management**: `AlarmManager` for precise scheduling of task notifications and alarms.
- **Build System**: [Gradle](https://gradle.org/) with Kotlin DSL (`build.gradle.kts`) and Version Catalogs (`libs.versions.toml`).

---

## 📥 Download

If you just want to try the app, you can download the latest pre-built APK from the **[Releases](https://github.com/dxtr77/Routini/releases/tag/0.2-beta)** page.

