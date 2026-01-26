package com.dxtr.routini.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val themeColor: Int, // Color int
    val isCompleted: Boolean = false,
    val recurringDays: List<DayOfWeek> = DayOfWeek.entries,
    val sortOrder: Int = 0
)
