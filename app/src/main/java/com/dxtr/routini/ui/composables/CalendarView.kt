package com.dxtr.routini.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dxtr.routini.ui.theme.AppIcons
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    tasksPerDate: Map<LocalDate, Int>,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val haptic = LocalHapticFeedback.current
    
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        // Compact Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault())),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentMonth = currentMonth.minusMonths(1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(painterResource(id = AppIcons.ArrowBack), null, modifier = Modifier.size(16.dp))
                }
                
                Text(
                    "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            currentMonth = YearMonth.now()
                            onDateSelected(LocalDate.now())
                        }
                )
                
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentMonth = currentMonth.plusMonths(1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(painterResource(id = AppIcons.ArrowForward), null, modifier = Modifier.size(16.dp))
                }
            }
        }
        
        // Day Headers
        DayOfWeekHeader()
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Compact Grid
        CalendarGrid(
            month = currentMonth,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDateSelected(date)
            },
            tasksPerDate = tasksPerDate
        )
    }
}


@Composable
private fun DayOfWeekHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    tasksPerDate: Map<LocalDate, Int>
) {
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()
    
    val dates = mutableListOf<LocalDate?>()
    repeat(firstDayOfWeek) { dates.add(null) }
    for (day in 1..daysInMonth) { dates.add(month.atDay(day)) }
    val remainingCells = (7 - (dates.size % 7)) % 7
    repeat(remainingCells) { dates.add(null) }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false
    ) {
        items(dates) { date ->
            if (date != null) {
                CalendarDateCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == LocalDate.now(),
                    taskCount = tasksPerDate[date] ?: 0,
                    onClick = { onDateSelected(date) }
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun CalendarDateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    taskCount: Int,
    onClick: () -> Unit
) {
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            if (taskCount > 0) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
