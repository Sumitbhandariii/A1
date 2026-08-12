package com.flowclock.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorHex: String,
    val targetCount: Int = 1,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: String = ""
)
