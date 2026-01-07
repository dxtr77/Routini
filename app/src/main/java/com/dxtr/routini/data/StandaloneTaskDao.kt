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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandaloneTask(task: StandaloneTask): Long

    @Update
    suspend fun updateStandaloneTask(task: StandaloneTask)

    @Delete
    suspend fun deleteStandaloneTask(task: StandaloneTask)
}