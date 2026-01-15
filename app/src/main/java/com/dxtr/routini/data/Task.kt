package com.dxtr.routini.data

import java.time.LocalDate
import java.time.LocalTime

interface Task {
    val id: Int
    val title: String
    val description: String?
    val time: LocalTime?
    val date: LocalDate?
    val isDone: Boolean
    val shouldVibrate: Boolean
}
