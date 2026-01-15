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

    @Query("SELECT * FROM routine_history WHERE taskId = :taskId AND completionDate = :completionDate")
    suspend fun getHistoryForTaskOnDate(taskId: Int, completionDate: LocalDate): RoutineHistory?

    @Query("DELETE FROM routine_history WHERE taskId = :taskId AND completionDate = :completionDate")
    suspend fun delete(taskId: Int, completionDate: LocalDate)

    @Query("SELECT * FROM routine_history WHERE completionDate = :completionDate")
    fun getHistoryForDate(completionDate: LocalDate): Flow<List<RoutineHistory>>
}
