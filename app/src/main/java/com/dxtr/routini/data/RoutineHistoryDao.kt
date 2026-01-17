package com.dxtr.routini.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RoutineHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routineHistory: RoutineHistory)

    @Query("SELECT * FROM routine_history WHERE taskId = :taskId AND taskType = :taskType AND completionDate = :completionDate")
    suspend fun getHistoryForTaskOnDate(taskId: Int, taskType: String, completionDate: LocalDate): RoutineHistory?

    @Query("DELETE FROM routine_history WHERE taskId = :taskId AND taskType = :taskType AND completionDate = :completionDate")
    suspend fun delete(taskId: Int, taskType: String, completionDate: LocalDate)

    @Query("SELECT * FROM routine_history WHERE completionDate = :completionDate")
    fun getHistoryForDate(completionDate: LocalDate): Flow<List<RoutineHistory>>

    @Query("SELECT * FROM routine_history WHERE completionDate >= :startDate")
    fun getHistorySince(startDate: LocalDate): Flow<List<RoutineHistory>>

    @Query("DELETE FROM routine_history WHERE taskId = :taskId AND taskType = :taskType")
    suspend fun deleteHistoryForTask(taskId: Int, taskType: String)

    @Query("DELETE FROM routine_history WHERE taskType = 'ROUTINE' AND taskId IN (SELECT id FROM routine_tasks WHERE routineId = :routineId)")
    suspend fun deleteHistoryForRoutine(routineId: Int)

    @Query("""
        DELETE FROM routine_history 
        WHERE (taskType = 'ROUTINE' AND taskId NOT IN (SELECT id FROM routine_tasks))
           OR (taskType = 'STANDALONE' AND taskId NOT IN (SELECT id FROM standalone_tasks))
    """)
    suspend fun cleanupOrphanedHistory()
}
