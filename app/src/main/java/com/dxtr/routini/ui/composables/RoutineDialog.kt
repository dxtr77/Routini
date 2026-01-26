package com.dxtr.routini.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dxtr.routini.R
import com.dxtr.routini.data.Routine
import com.dxtr.routini.ui.theme.AppIcons
import java.time.DayOfWeek

@Composable
fun RoutineDialog(
    routine: Routine?,
    onConfirm: (String, List<DayOfWeek>) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = routine != null
    var name by remember { mutableStateOf(routine?.name ?: "") }
    var selectedDays by remember { mutableStateOf(routine?.recurringDays ?: emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val nameMaxLength = 30

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Routine?") },
            text = { Text("This will delete the routine and all its tasks permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Routine" else stringResource(R.string.new_routine_title),
                    style = MaterialTheme.typography.titleLarge
                )
                if (isEditing) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            painter = painterResource(id = AppIcons.Delete),
                            contentDescription = "Delete Routine",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            RoutineDialogContent(
                name = name,
                onNameChange = { name = it },
                selectedDays = selectedDays,
                onSelectedDaysChange = { selectedDays = it },
                nameMaxLength = nameMaxLength,
                haptic = haptic
            )
        },
        confirmButton = {
            GradientButton(
                onClick = {
                    if (name.isNotBlank() && !isSaving) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        isSaving = true
                        onConfirm(name, selectedDays)
                    }
                },
                text = if (isSaving) "Saving..." else stringResource(R.string.save_action),
                enabled = name.isNotBlank() && selectedDays.isNotEmpty() && !isSaving,
                modifier = Modifier.height(48.dp)
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss, 
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}

@Composable
fun RoutineDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
    selectedDays: List<DayOfWeek>,
    onSelectedDaysChange: (List<DayOfWeek>) -> Unit,
    nameMaxLength: Int,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= nameMaxLength) onNameChange(it) },
            label = { Text(stringResource(R.string.routine_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    "${name.length}/$nameMaxLength",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        )
        Text(stringResource(R.string.recurring_days_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "All" to DayOfWeek.entries.toList(),
                "Weekdays" to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                "Weekend" to listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            )
            
            presets.forEach { (label, days) ->
                val isSelected = selectedDays.size == days.size && selectedDays.containsAll(days)
                FilterChip(
                    selected = isSelected,
                    onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onSelectedDaysChange(days) 
                    },
                    label = { 
                        Text(
                            label, 
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        ) 
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Individual Day Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = selectedDays.contains(day)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onSelectedDaysChange(
                                if (isSelected) selectedDays - day else selectedDays + day
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
