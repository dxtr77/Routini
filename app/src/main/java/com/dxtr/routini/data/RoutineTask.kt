package com.dxtr.routini.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(
    tableName = "routine_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routineId: Int,
    val title: String,
    val description: String? = null,
    val time: LocalTime? = null,
    val customSoundUri: String? = null,
    val isDone: Boolean = false,
    val shouldPlaySound: Boolean = false,
    // NEW: If null, follows Routine days. If set, runs only on these days.
    val specificDays: List<DayOfWeek>? = null
)