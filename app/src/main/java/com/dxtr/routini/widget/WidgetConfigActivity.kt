package com.dxtr.routini.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.data.Routine
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get widget ID
        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            FilterMenuDialog()
        }
    }

    @Composable
    fun FilterMenuDialog() {
        var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val db = AppDatabase.getDatabase(applicationContext)
            routines = db.routineDao().getAllRoutinesSuspend()
        }

        Dialog(onDismissRequest = { finish() }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Filter",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn {
                        // Today option
                        item {
                            FilterMenuItem(
                                label = "Today",
                                onClick = {
                                    saveFilterSelection(WidgetPreferences.FILTER_TODAY, -1)
                                }
                            )
                        }

                        // Tomorrow option
                        item {
                            FilterMenuItem(
                                label = "Tomorrow",
                                onClick = {
                                    saveFilterSelection(WidgetPreferences.FILTER_TOMORROW, -1)
                                }
                            )
                        }

                        // Divider if there are routines
                        if (routines.isNotEmpty()) {
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }

                        // Routine options
                        items(routines) { routine ->
                            FilterMenuItem(
                                label = routine.name,
                                onClick = {
                                    saveFilterSelection(WidgetPreferences.FILTER_ROUTINE, routine.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FilterMenuItem(label: String, onClick: () -> Unit) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }

    private fun saveFilterSelection(filterMode: String, routineId: Int) {
        val prefs = WidgetPreferences(this)
        prefs.setFilterMode(widgetId, filterMode)
        prefs.setRoutineId(widgetId, routineId)

        // Update the widget
        TodayTasksWidgetProvider.refreshWidget(this)

        // Return success
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
