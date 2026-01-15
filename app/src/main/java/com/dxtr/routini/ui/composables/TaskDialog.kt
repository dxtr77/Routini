package com.dxtr.routini.ui.composables

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dxtr.routini.R
import com.dxtr.routini.ui.theme.AppIcons
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    dialogTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, LocalTime?, String?, Boolean, Boolean, List<DayOfWeek>?) -> Unit,
    onDelete: (() -> Unit)?,
    initialTitle: String,
    initialDescription: String?,
    initialTime: LocalTime?,
    initialSoundUri: String?,
    initialPlaySound: Boolean,
    initialShouldVibrate: Boolean,
    initialSpecificDays: List<DayOfWeek>?,
    availableDays: List<DayOfWeek>,
    isSaving: Boolean,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    var time by remember { mutableStateOf(initialTime) }
    var soundUri by remember { mutableStateOf(initialSoundUri) }
    var playSound by remember { mutableStateOf(initialPlaySound) }
    var shouldVibrate by remember { mutableStateOf(initialShouldVibrate) }
    var specificDays by remember { mutableStateOf(initialSpecificDays ?: emptyList()) }

    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let {
                soundUri = it.toString()
            }
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
                if (onDelete != null) {
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
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(painter = painterResource(id = AppIcons.Schedule), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Set Time")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (time != null) {
                        IconButton(onClick = { time = null }) {
                            Icon(painter = painterResource(id = AppIcons.Close), contentDescription = "Clear Time")
                        }
                    }
                }

                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Repeat on", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        availableDays.forEach { day ->
                            val isSelected = specificDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { specificDays = if (isSelected) specificDays - day else specificDays + day },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.name.take(1),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Play Sound")
                    Spacer(Modifier.weight(1f))
                    Switch(checked = playSound, onCheckedChange = { playSound = it })
                }
                AnimatedVisibility(playSound) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm sound")
                            soundUri?.let { intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                it.toUri()) }
                            ringtonePickerLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(painter = painterResource(id = AppIcons.MusicNote), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = soundUri?.let { RingtoneManager.getRingtone(context, it.toUri()).getTitle(context) } ?: "Default Sound",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Vibrate")
                    Spacer(Modifier.weight(1f))
                    Switch(checked = shouldVibrate, onCheckedChange = { shouldVibrate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, description.ifBlank { null }, time, soundUri, playSound, shouldVibrate, specificDays)
                },
                enabled = title.isNotBlank() && !isSaving && specificDays.isNotEmpty()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(id = R.string.save_action))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel_action))
            }
        }
    )

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = time?.hour ?: 0, initialMinute = time?.minute ?: 0)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(
                    state = timeState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
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
            }
        )
    }
}
