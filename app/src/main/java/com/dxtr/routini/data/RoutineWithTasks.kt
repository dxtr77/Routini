package com.dxtr.routini.data

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineWithTasks(
    @Embedded val routine: Routine,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val tasks: List<RoutineTask>
)