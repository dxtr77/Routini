package com.dxtr.routini.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StandaloneTaskDao {
    @Query("SELECT * FROM standalone_tasks ORDER BY time ASC")
    fun getAllStandaloneTasks(): Flow<List<StandaloneTask>>

    @Query("SELECT * FROM standalone_tasks")
    fun getAllStandaloneTasksSync(): List<StandaloneTask>

    @Query("SELECT * FROM standalone_tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): StandaloneTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandaloneTask(task: StandaloneTask): Long

    @Update
    suspend fun update(task: StandaloneTask)

    @Delete
    suspend fun deleteStandaloneTask(task: StandaloneTask)
}