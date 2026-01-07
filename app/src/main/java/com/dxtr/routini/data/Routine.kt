package com.dxtr.routini.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val themeColor: Int, // Color int
    val isCompleted: Boolean = false,
    val recurringDays: List<DayOfWeek> = DayOfWeek.values().toList()
)

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}
