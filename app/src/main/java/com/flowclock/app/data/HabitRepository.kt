package com.flowclock.app.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitDao) {

    fun observeHabits(): Flow<List<HabitEntity>> = dao.observeHabits()

    suspend fun addHabit(name: String, colorHex: String, targetCount: Int) {
        dao.insert(HabitEntity(name = name, colorHex = colorHex, targetCount = targetCount))
    }

    suspend fun updateHabit(habit: HabitEntity) = dao.update(habit)

    suspend fun deleteHabit(habit: HabitEntity) = dao.delete(habit)
}
