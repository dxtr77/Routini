package com.dxtr.routini.ui.composables

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.dxtr.routini.R
import com.dxtr.routini.ui.theme.AppIcons
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneTaskDialog(
    dialogTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, LocalDate?, LocalTime?, String?, Boolean, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    initialTitle: String,
    initialDescription: String?,
    initialDate: LocalDate?,
    initialTime: LocalTime?,
    initialSoundUri: String?,
    initialPlaySound: Boolean,
    initialShouldVibrate: Boolean,
    isSaving: Boolean,
    isNewTask: Boolean,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }
    var soundUri by remember { mutableStateOf(initialSoundUri) }
    var playSound by remember { mutableStateOf(initialPlaySound) }
    var shouldVibrate by remember { mutableStateOf(initialShouldVibrate) }
    var isDateTimeEnabled by remember { mutableStateOf(initialDate != null) }
    var showError by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    // Character limits
    val titleMaxLength = 50
    val descriptionMaxLength = 200
    

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            uri?.let { soundUri = it.toString() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dialogTitle)
                if (onDelete != null && !isNewTask) {
                    IconButton(onClick = onDelete) {
                        Icon(painter = painterResource(id = AppIcons.Delete), contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= titleMaxLength) title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && title.isBlank(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (showError && title.isBlank()) {
                                Text("Title is required", color = MaterialTheme.colorScheme.error)
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Text("${title.length}/$titleMaxLength")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= descriptionMaxLength) description = it },
                    label = { Text(stringResource(R.string.task_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    supportingText = {
                        Text(
                            "${description.length}/$descriptionMaxLength",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Set Date/Time")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = isDateTimeEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            isDateTimeEnabled = it
                            if (isDateTimeEnabled) {
                                if (date == null) date = LocalDate.now()
                                if (time == null) time = LocalTime.now()
                            } else {
                                date = null
                                time = null
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = isDateTimeEnabled) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        painter = painterResource(id = AppIcons.CalendarMonth),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        date?.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)) ?: "Set Date",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (date != null) {
                                    Icon(
                                        painter = painterResource(id = AppIcons.Close),
                                        contentDescription = "Clear Date",
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 8.dp)
                                            .size(18.dp)
                                            .clickable {
                                                date = null
                                                time = null
                                            }
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = date != null
                                ) {
                                    Icon(
                                        painter = painterResource(id = AppIcons.Schedule),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        time?.let { com.dxtr.routini.utils.TimeUtils.formatTime(context, it) } ?: "Set Time",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (time != null) {
                                    Icon(
                                        painter = painterResource(id = AppIcons.Close),
                                        contentDescription = "Clear Time",
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 8.dp)
                                            .size(18.dp)
                                            .clickable { time = null }
                                    )
                                }
                            }
                        }
                        if (showError) {
                            Text("Cannot create task in the past", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            val isReadyForAlarm = date != null && time != null
                            Text(
                                text = "Play Sound",
                                color = if (isReadyForAlarm) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = playSound && isReadyForAlarm, 
                                onCheckedChange = { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    playSound = it 
                                },
                                enabled = isReadyForAlarm
                            )
                        }
                        AnimatedVisibility(playSound && date != null && time != null) {
                            OutlinedButton(
                                onClick = {
// FIX: Wrap in try-catch to prevent crash if activity not found
                                    try {
                                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm sound")
                                        soundUri?.let { intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it.toUri()) }
                                        ringtonePickerLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        // Fallback or log
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = AppIcons.MusicNote),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = soundUri?.let { 
                                        try { RingtoneManager.getRingtone(context, it.toUri()).getTitle(context) } catch(e:Exception) { "Unknown" }
                                    } ?: "Default Sound",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            val isReadyForAlarm = date != null && time != null
                            Text(
                                text = "Vibrate",
                                color = if (isReadyForAlarm) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = shouldVibrate && isReadyForAlarm, 
                                onCheckedChange = { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    shouldVibrate = it 
                                },
                                enabled = isReadyForAlarm
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            GradientButton(
                onClick = {
                    if (isDateTimeEnabled && date != null && time != null) {
                        val taskDateTime = LocalDateTime.of(date, time)
                        if (taskDateTime.isBefore(LocalDateTime.now())) {
                            return@GradientButton
                        }
                    }
                    val finalDate = if (isDateTimeEnabled) date else null
                    val finalTime = if (isDateTimeEnabled) time else null
                    onConfirm(title, description.ifBlank { null }, finalDate, finalTime, soundUri, playSound, shouldVibrate)
                },
                text = if (isSaving) "Saving..." else stringResource(id = R.string.save_action),
                enabled = title.isNotBlank() && !isSaving,
                modifier = Modifier.width(100.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel_action))
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = time?.hour ?: 0, 
            initialMinute = time?.minute ?: 0,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        )
        // Replaced TimePickerDialog with AlertDialog as it is not part of standard Material3 yet
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timeState)
            }
        )
    }
}
