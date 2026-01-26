package com.dxtr.routini.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.dxtr.routini.MainActivity
import com.dxtr.routini.R
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.data.RoutineHistory
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.data.StandaloneTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodayTasksWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TASK_TOGGLE = "com.dxtr.routini.widget.TASK_TOGGLE"
        const val ACTION_FILTER_CLICK = "com.dxtr.routini.widget.FILTER_CLICK"
        const val ACTION_MENU_ITEM_CLICK = "com.dxtr.routini.widget.MENU_ITEM_CLICK"
        const val ACTION_ADD_TASK = "com.dxtr.routini.widget.ADD_TASK"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TYPE = "task_type"
        const val EXTRA_IS_DONE = "is_done"
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_FILTER_MODE = "filter_mode"
        const val EXTRA_ROUTINE_ID = "routine_id"

        fun refreshWidget(context: Context) {
            val intent = Intent(context, TodayTasksWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, TodayTasksWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TASK_TOGGLE -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                val taskType = intent.getStringExtra(EXTRA_TASK_TYPE) ?: return
                val isDone = intent.getBooleanExtra(EXTRA_IS_DONE, false)
                handleTaskToggle(context, taskId, taskType, isDone)
            }
            ACTION_FILTER_CLICK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    handleFilterClick(context, widgetId)
                }
            }
            ACTION_MENU_ITEM_CLICK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val filterMode = intent.getStringExtra(EXTRA_FILTER_MODE)
                val routineId = intent.getIntExtra(EXTRA_ROUTINE_ID, -1)
                if (widgetId != -1 && filterMode != null) {
                    handleMenuItemClick(context, widgetId, filterMode, routineId)
                }
            }
            ACTION_ADD_TASK -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(launchIntent)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = WidgetPreferences(context)
        appWidgetIds.forEach { widgetId ->
            prefs.clearWidgetData(widgetId)
        }
    }

    @Suppress("DEPRECATION")
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val prefs = WidgetPreferences(context)
        val filterMode = prefs.getFilterMode(widgetId)
        val routineId = prefs.getRoutineId(widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_today_tasks)

        // Update filter label - load routine name if in routine mode
        val filterLabel = when (filterMode) {
            WidgetPreferences.FILTER_TOMORROW -> context.getString(R.string.widget_filter_tomorrow)
            WidgetPreferences.FILTER_ROUTINE -> {
                if (routineId != -1) {
                    // Load routine name from database
                    val db = AppDatabase.getDatabase(context)
                    val routine = kotlinx.coroutines.runBlocking {
                        db.routineDao().getRoutineById(routineId)
                    }
                    routine?.name ?: context.getString(R.string.widget_filter_today)
                } else {
                    context.getString(R.string.widget_filter_today)
                }
            }
            else -> context.getString(R.string.widget_filter_today)
        }
        views.setTextViewText(R.id.widget_filter_label, filterLabel)

        // Set up filter button click - toggle menu visibility
        val filterIntent = Intent(context, TodayTasksWidgetProvider::class.java).apply {
            action = ACTION_FILTER_CLICK
            putExtra(EXTRA_WIDGET_ID, widgetId)
        }
        val filterPendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId * 1000,
            filterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_filter_button, filterPendingIntent)

        // Check if menu is open
        val isMenuOpen = prefs.isMenuOpen(widgetId)
        
        if (isMenuOpen) {
            // Show menu list
            val menuServiceIntent = Intent(context, WidgetMenuService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_task_list, menuServiceIntent)
            
            // Set up menu item click template
            val menuClickIntent = Intent(context, TodayTasksWidgetProvider::class.java).apply {
                action = ACTION_MENU_ITEM_CLICK
            }
            val menuClickPendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId * 1000 + 2,
                menuClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_task_list, menuClickPendingIntent)
            
            // Hide progress bar when menu is open
            views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_progress_text, android.view.View.GONE)
        } else {
            // Show tasks list
            val serviceIntent = Intent(context, TodayTasksWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_task_list, serviceIntent)

            // Set up item click template for task toggles
            val toggleIntent = Intent(context, TodayTasksWidgetProvider::class.java).apply {
                action = ACTION_TASK_TOGGLE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_task_list, togglePendingIntent)
            
            // Update task count and progress
            updateProgressAndCount(context, widgetId, views, filterMode, routineId)
        }

        // Set up add task button and title to open app
        val openAppIntent = Intent(context, TodayTasksWidgetProvider::class.java).apply {
            action = ACTION_ADD_TASK
        }
        val openAppPendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId * 1000 + 1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_add_task_button, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_task_list)
    }

    private fun handleFilterClick(context: Context, widgetId: Int) {
        val prefs = WidgetPreferences(context)
        val isMenuOpen = prefs.isMenuOpen(widgetId)
        
        // Toggle menu state
        prefs.setMenuOpen(widgetId, !isMenuOpen)
        
        // Update widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, widgetId)
    }
    
    private fun handleMenuItemClick(context: Context, widgetId: Int, filterMode: String, routineId: Int) {
        val prefs = WidgetPreferences(context)
        
        // Save filter selection
        prefs.setFilterMode(widgetId, filterMode)
        prefs.setRoutineId(widgetId, routineId)
        
        // Close menu
        prefs.setMenuOpen(widgetId, false)
        
        // Update widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, widgetId)
    }
    
    private fun updateProgressAndCount(context: Context, widgetId: Int, views: RemoteViews, filterMode: String, routineId: Int) {
        val db = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val date = if (filterMode == WidgetPreferences.FILTER_TOMORROW) {
                LocalDate.now().plusDays(1)
            } else {
                LocalDate.now()
            }
            
            // Get tasks based on filter
            val tasks = when (filterMode) {
                WidgetPreferences.FILTER_ROUTINE -> {
                    if (routineId != -1) {
                        db.routineDao().getRoutineTasksForRoutine(routineId)
                    } else {
                        emptyList()
                    }
                }
                else -> {
                    val standalone = db.standaloneTaskDao().getStandaloneTasksForDateSuspend(date)
                    val routineTasks = mutableListOf<com.dxtr.routini.data.RoutineTask>()
                    val routines = db.routineDao().getRoutinesForDaySuspend(date.dayOfWeek.name)
                    routines.forEach { routine ->
                        val tasksForRoutine = db.routineDao().getTasksForRoutineSuspend(routine.id)
                        routineTasks.addAll(
                            tasksForRoutine.filter { task ->
                                task.specificDays.isNullOrEmpty() || task.specificDays.contains(date.dayOfWeek)
                            }
                        )
                    }
                    standalone + routineTasks
                }
            }
            
            val history = db.routineHistoryDao().getHistoryForDateSuspend(date)
            val completedCount = tasks.count { task ->
                val taskType = if (task is com.dxtr.routini.data.RoutineTask) "ROUTINE" else "STANDALONE"
                if (date == LocalDate.now()) {
                    task.isDone
                } else {
                    history.any { it.taskId == task.id && it.taskType == taskType }
                }
            }
            
            val totalCount = tasks.size
            val progress = if (totalCount > 0) (completedCount * 100 / totalCount) else 0
            
            // Update on main thread
            CoroutineScope(Dispatchers.Main).launch {
                views.setTextViewText(R.id.widget_task_count, "$totalCount tasks")
                views.setTextViewText(R.id.widget_progress_text, "$progress%")
                views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
            }
        }
    }

    private fun handleTaskToggle(context: Context, taskId: Int, taskType: String, currentIsDone: Boolean) {
        val newIsDone = !currentIsDone
        val db = AppDatabase.getDatabase(context)
        val today = LocalDate.now()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (taskType) {
                    "ROUTINE" -> {
                        val task = db.routineDao().getTaskById(taskId)
                        if (task != null) {
                            val updatedTask = task.copy(isDone = newIsDone)
                            db.routineDao().updateRoutineTask(updatedTask)

                            if (newIsDone) {
                                db.routineHistoryDao().insert(
                                    RoutineHistory(
                                        taskId = taskId,
                                        taskType = "ROUTINE",
                                        completionDate = today
                                    )
                                )
                            } else {
                                db.routineHistoryDao().delete(taskId, "ROUTINE", today)
                            }
                        }
                    }
                    "STANDALONE" -> {
                        val task = db.standaloneTaskDao().getTaskById(taskId)
                        if (task != null) {
                            val updatedTask = task.copy(isDone = newIsDone)
                            db.standaloneTaskDao().update(updatedTask)

                            if (task.date != null) {
                                if (newIsDone) {
                                    db.routineHistoryDao().insert(
                                        RoutineHistory(
                                            taskId = taskId,
                                            taskType = "STANDALONE",
                                            completionDate = task.date
                                        )
                                    )
                                } else {
                                    db.routineHistoryDao().delete(taskId, "STANDALONE", task.date)
                                }
                            }
                        }
                    }
                }

                // Refresh widget
                refreshWidget(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
