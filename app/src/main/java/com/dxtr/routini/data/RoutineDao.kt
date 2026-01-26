package com.dxtr.routini.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY sortOrder ASC, id ASC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines")
    suspend fun getAllRoutinesSuspend(): List<Routine>
    
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Int): Routine?

    @Query("SELECT * FROM routines WHERE recurringDays LIKE '%\"' || :day || '\"%'")
    fun getRoutinesForDay(day: String): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE recurringDays LIKE '%\"' || :day || '\"%'")
    suspend fun getRoutinesForDaySuspend(day: String): List<Routine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Query("SELECT * FROM routine_tasks WHERE routineId = :routineId ORDER BY sortOrder ASC, time ASC")
    fun getTasksForRoutine(routineId: Int): Flow<List<RoutineTask>>

    @Query("SELECT * FROM routine_tasks WHERE routineId = :routineId ORDER BY time ASC")
    suspend fun getTasksForRoutineSuspend(routineId: Int): List<RoutineTask>
    
    @Query("SELECT * FROM routine_tasks")
    suspend fun getAllRoutineTasks(): List<RoutineTask>

    @Query("SELECT * FROM routine_tasks WHERE id = :id") // Adjust table name if different
    suspend fun getTaskById(id: Int): RoutineTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineTask(task: RoutineTask): Long

    @Update
    suspend fun updateRoutineTask(task: RoutineTask)

    @Query("UPDATE routine_tasks SET isDone = 0")
    suspend fun resetAllTasks()

    @Delete
    suspend fun deleteRoutineTask(task: RoutineTask)

    @Transaction
    @Query("SELECT * FROM routines")
    fun getRoutinesWithTasks(): Flow<List<RoutineWithTasks>>
    
    // Widget-specific queries
    @Query("""
        SELECT rt.* FROM routine_tasks rt
        INNER JOIN routines r ON rt.routineId = r.id
        WHERE (r.recurringDays LIKE '%' || :dayOfWeek || '%')
        AND (rt.specificDays IS NULL OR rt.specificDays = '[]' OR rt.specificDays LIKE '%' || :dayOfWeek || '%')
        ORDER BY rt.time ASC
    """)
    suspend fun getAllRoutineTasksForDay(dayOfWeek: String): List<RoutineTask>
    
    @Query("""
        SELECT rt.* FROM routine_tasks rt
        WHERE rt.routineId = :routineId
        ORDER BY rt.time ASC
    """)
    suspend fun getRoutineTasksForRoutine(routineId: Int): List<RoutineTask>
}
