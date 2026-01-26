package com.dxtr.routini.widget

import android.content.Context
import android.content.SharedPreferences

class WidgetPreferences(context: Context) {
    companion object {
        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_FILTER_MODE = "filter_mode_"
        private const val KEY_ROUTINE_ID = "routine_id_"
        private const val KEY_MENU_OPEN = "menu_open_"
        
        const val FILTER_TODAY = "today"
        const val FILTER_TOMORROW = "tomorrow"
        const val FILTER_ROUTINE = "routine"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun getFilterMode(widgetId: Int): String {
        return prefs.getString(KEY_FILTER_MODE + widgetId, FILTER_TODAY) ?: FILTER_TODAY
    }
    
    fun setFilterMode(widgetId: Int, mode: String) {
        prefs.edit().putString(KEY_FILTER_MODE + widgetId, mode).apply()
    }
    
    fun getRoutineId(widgetId: Int): Int {
        return prefs.getInt(KEY_ROUTINE_ID + widgetId, -1)
    }
    
    fun setRoutineId(widgetId: Int, routineId: Int) {
        prefs.edit().putInt(KEY_ROUTINE_ID + widgetId, routineId).apply()
    }
    
    fun clearWidgetData(widgetId: Int) {
        prefs.edit()
            .remove(KEY_FILTER_MODE + widgetId)
            .remove(KEY_ROUTINE_ID + widgetId)
            .remove(KEY_MENU_OPEN + widgetId)
            .apply()
    }
    
    fun isMenuOpen(widgetId: Int): Boolean {
        return prefs.getBoolean(KEY_MENU_OPEN + widgetId, false)
    }
    
    fun setMenuOpen(widgetId: Int, isOpen: Boolean) {
        prefs.edit().putBoolean(KEY_MENU_OPEN + widgetId, isOpen).apply()
    }
}
