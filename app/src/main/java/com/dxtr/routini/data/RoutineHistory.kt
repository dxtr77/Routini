package com.dxtr.routini.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "routine_history")
data class RoutineHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val completionDate: LocalDate
)
