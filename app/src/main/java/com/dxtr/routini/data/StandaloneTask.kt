package com.dxtr.routini.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "standalone_tasks")
data class StandaloneTask(
    @PrimaryKey(autoGenerate = true) override val id: Int = 0,
    override val title: String,
    override val description: String? = null,
    override val date: LocalDate? = null,
    override val time: LocalTime? = null,
    val customSoundUri: String? = null,
    override val isDone: Boolean = false,
    val shouldPlaySound: Boolean = false,
    override val shouldVibrate: Boolean = false
) : Task
