package com.dxtr.routini.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StandaloneTaskDao {
    @Query("SELECT * FROM standalone_tasks ORDER BY date ASC, time ASC")
    fun getAllStandaloneTasks(): Flow<List<StandaloneTask>>

    @Query("SELECT * FROM standalone_tasks ORDER BY date ASC, time ASC")
    suspend fun getAllStandaloneTasksSuspend(): List<StandaloneTask>

    @Query("SELECT * FROM standalone_tasks WHERE date = :date ORDER BY time ASC")
    fun getStandaloneTasksForDate(date: LocalDate): Flow<List<StandaloneTask>>

    @Query("SELECT * FROM standalone_tasks WHERE date = :date ORDER BY time ASC")
    suspend fun getStandaloneTasksForDateSuspend(date: LocalDate): List<StandaloneTask>

    @Query("SELECT * FROM standalone_tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): StandaloneTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandaloneTask(task: StandaloneTask): Long

    @Update
    suspend fun update(task: StandaloneTask)

    @Query("UPDATE standalone_tasks SET isDone = 0")
    suspend fun resetAllTasks()

    @Delete
    suspend fun deleteStandaloneTask(task: StandaloneTask)
}
