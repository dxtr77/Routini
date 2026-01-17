package com.dxtr.routini.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "routine_history",
    indices = [
        Index("taskId"),
        Index("completionDate")
    ]
)
data class RoutineHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val taskType: String, // "ROUTINE" or "STANDALONE"
    val completionDate: LocalDate
)
