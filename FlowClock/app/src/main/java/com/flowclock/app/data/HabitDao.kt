package com.flowclock.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    // Synchronous accessors are required inside RemoteViewsFactory / AppWidgetProvider,
    // which do not run inside a coroutine scope.
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getHabitsSync(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun getHabitByIdSync(id: Long): HabitEntity?

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Update
    fun updateSync(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)
}
