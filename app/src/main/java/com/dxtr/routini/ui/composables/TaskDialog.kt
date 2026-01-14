package com.dxtr.routini.ui.composables

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun TaskDialog(
    dialogTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean, List<DayOfWeek>) -> Unit,
    initialTitle: String = "",
    initialDescription: String = "",
    initialTime: String = "",
    initialSoundUri: String = "",
    initialPlaySound: Boolean = true,
    initialSpecificDays: List<DayOfWeek> = emptyList(),
    availableDays: List<DayOfWeek> = emptyList(),
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var time by remember { mutableStateOf(initialTime) }
    var soundUri by remember { mutableStateOf(initialSoundUri) }
    var playSound by remember { mutableStateOf(initialPlaySound) }
    var specificDays by remember { mutableStateOf(initialSpecificDays) }
    val context = LocalContext.current

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let {
                soundUri = it.toString()
            }
        }
    )

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
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text(stringResource(R.string.task_time_label)) },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.task_days_label), style = MaterialTheme.typography.labelLarge)
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

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = playSound, onCheckedChange = { playSound = it })
                    Text(stringResource(R.string.play_sound_label))
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        ringtonePickerLauncher.launch(intent)
                    }) {
                        Text(text = stringResource(R.string.select_sound_action), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (soundUri.isNotEmpty()) {
                    val ringtone = RingtoneManager.getRingtone(context, Uri.parse(soundUri))
                    Text(
                        stringResource(R.string.selected_sound_label, ringtone.getTitle(context)),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, time, soundUri, playSound, specificDays) },
                enabled = title.isNotBlank() && specificDays.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel_action))
            }
        }
    )
}
