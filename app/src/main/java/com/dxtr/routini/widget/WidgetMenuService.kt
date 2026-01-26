package com.dxtr.routini.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.dxtr.routini.R
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.data.Routine
import kotlinx.coroutines.runBlocking

class WidgetMenuService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetMenuRemoteViewsFactory(this.applicationContext, intent)
    }
}

class WidgetMenuRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private val db = AppDatabase.getDatabase(context)
    private var menuItems: List<MenuItem> = emptyList()

    data class MenuItem(
        val filterMode: String,
        val routineId: Int = -1,
        val label: String
    )

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        runBlocking {
            loadMenuItems()
        }
    }

    private suspend fun loadMenuItems() {
        val items = mutableListOf<MenuItem>()
        
        // Today option
        items.add(MenuItem(
            filterMode = WidgetPreferences.FILTER_TODAY,
            label = context.getString(R.string.widget_filter_today)
        ))
        
        // Tomorrow option
        items.add(MenuItem(
            filterMode = WidgetPreferences.FILTER_TOMORROW,
            label = context.getString(R.string.widget_filter_tomorrow)
        ))
        
        // Get all routines
        val routines = db.routineDao().getAllRoutinesSuspend()
        
        // Add routine options
        routines.forEach { routine ->
            items.add(MenuItem(
                filterMode = WidgetPreferences.FILTER_ROUTINE,
                routineId = routine.id,
                label = routine.name
            ))
        }
        
        menuItems = items
    }

    override fun onDestroy() {
        menuItems = emptyList()
    }

    override fun getCount(): Int = menuItems.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= menuItems.size) {
            return RemoteViews(context.packageName, R.layout.widget_menu_item)
        }

        val item = menuItems[position]
        val views = RemoteViews(context.packageName, R.layout.widget_menu_item)

        // Set menu item text
        views.setTextViewText(R.id.menu_item_text, item.label)

        // Set up click intent
        val fillIntent = Intent().apply {
            putExtra(TodayTasksWidgetProvider.EXTRA_WIDGET_ID, widgetId)
            putExtra(TodayTasksWidgetProvider.EXTRA_FILTER_MODE, item.filterMode)
            putExtra(TodayTasksWidgetProvider.EXTRA_ROUTINE_ID, item.routineId)
        }
        views.setOnClickFillInIntent(R.id.menu_item_text, fillIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
