package com.dxtr.routini.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.dxtr.routini.R
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.data.Task
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodayTasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayTasksRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TodayTasksRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private val db = AppDatabase.getDatabase(context)
    private val prefs = WidgetPreferences(context)
    private var tasks: List<TaskWrapper> = emptyList()

    data class TaskWrapper(
        val task: Task,
        val taskType: String // "ROUTINE" or "STANDALONE"
    )

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        // This is called on the UI thread, but we need to do blocking operations
        runBlocking {
            loadTasks()
        }
    }

    private suspend fun loadTasks() {
        val filterMode = prefs.getFilterMode(widgetId)
        val routineId = prefs.getRoutineId(widgetId)
        
        val date = when (filterMode) {
            WidgetPreferences.FILTER_TOMORROW -> LocalDate.now().plusDays(1)
            else -> LocalDate.now()
        }

        val taskList = mutableListOf<TaskWrapper>()

        when (filterMode) {
            WidgetPreferences.FILTER_TODAY, WidgetPreferences.FILTER_TOMORROW -> {
                // Get standalone tasks
                val standaloneTasks = db.standaloneTaskDao().getStandaloneTasksForDateSuspend(date)
                taskList.addAll(standaloneTasks.map { TaskWrapper(it, "STANDALONE") })

                // Get routine tasks using same logic as MainViewModel
                val routines = db.routineDao().getRoutinesForDaySuspend(date.dayOfWeek.name)
                routines.forEach { routine ->
                    val routineTasks = db.routineDao().getTasksForRoutineSuspend(routine.id)
                    taskList.addAll(
                        routineTasks.filter { task ->
                            task.specificDays.isNullOrEmpty() || task.specificDays.contains(date.dayOfWeek)
                        }.map { TaskWrapper(it, "ROUTINE") }
                    )
                }

                // Get completion history
                val history = db.routineHistoryDao().getHistoryForDateSuspend(date)
                val today = LocalDate.now()

                // Update completion status
                tasks = taskList.map { wrapper ->
                    val task = wrapper.task
                    val isDone = if (date == today) {
                        task.isDone
                    } else {
                        history.any { it.taskId == task.id && it.taskType == wrapper.taskType }
                    }

                    when (task) {
                        is RoutineTask -> TaskWrapper(task.copy(isDone = isDone), wrapper.taskType)
                        is StandaloneTask -> TaskWrapper(task.copy(isDone = isDone), wrapper.taskType)
                        else -> wrapper
                    }
                }.distinctBy { "${it.taskType}_${it.task.id}" } // Avoid duplicates if any
                .sortedWith(compareBy(nullsLast()) { it.task.time })
            }

            WidgetPreferences.FILTER_ROUTINE -> {
                if (routineId != -1) {
                    val routineTasks = db.routineDao().getRoutineTasksForRoutine(routineId)
                    val history = db.routineHistoryDao().getHistoryForDateSuspend(date)

                    tasks = routineTasks.filter { task ->
                        task.specificDays.isNullOrEmpty() || task.specificDays.contains(date.dayOfWeek)
                    }.map { task ->
                        val isDone = task.isDone || history.any { it.taskId == task.id && it.taskType == "ROUTINE" }
                        TaskWrapper(task.copy(isDone = isDone), "ROUTINE")
                    }.sortedWith(compareBy(nullsLast()) { it.task.time })
                } else {
                    tasks = emptyList()
                }
            }

            else -> {
                tasks = emptyList()
            }
        }
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) {
            return RemoteViews(context.packageName, R.layout.widget_task_item)
        }

        val wrapper = tasks[position]
        val task = wrapper.task
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        // Set task title
        views.setTextViewText(R.id.task_title, task.title)

        // Set task time
        val taskTime = task.time
        val timeText = if (taskTime != null) {
            com.dxtr.routini.utils.TimeUtils.formatTime(context, taskTime)
        } else {
            context.getString(R.string.any_time_label)
        }
        views.setTextViewText(R.id.task_time, timeText)

        // Set checkbox state using ImageView
        val checkboxDrawable = if (task.isDone) {
            R.drawable.baseline_check_circle_24
        } else {
            R.drawable.baseline_radio_button_unchecked_24
        }
        views.setImageViewResource(R.id.task_checkbox, checkboxDrawable)

        // Set up click intent for the checkbox
        val fillIntent = Intent().apply {
            putExtra(TodayTasksWidgetProvider.EXTRA_TASK_ID, task.id)
            putExtra(TodayTasksWidgetProvider.EXTRA_TASK_TYPE, wrapper.taskType)
            putExtra(TodayTasksWidgetProvider.EXTRA_IS_DONE, task.isDone)
        }
        views.setOnClickFillInIntent(R.id.task_checkbox, fillIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
