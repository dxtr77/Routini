package com.dxtr.routini.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "standalone_tasks")
data class StandaloneTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val customSoundUri: String? = null,
    val isDone: Boolean = false,
    val shouldPlaySound: Boolean = false
)