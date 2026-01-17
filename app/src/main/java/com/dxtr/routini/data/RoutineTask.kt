package com.dxtr.routini.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
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
    ],
    indices = [Index("routineId")]
)
data class RoutineTask(
    @PrimaryKey(autoGenerate = true) override val id: Int = 0,
    val routineId: Int,
    override val title: String,
    override val description: String? = null,
    override val time: LocalTime? = null,
    override val date: LocalDate? = null,
    val customSoundUri: String? = null,
    override val isDone: Boolean = false,
    val shouldPlaySound: Boolean = false,
    override val shouldVibrate: Boolean = false,
    // NEW: If null, follows Routine days. If set, runs only on these days.
    val specificDays: List<DayOfWeek>? = null
) : Task
