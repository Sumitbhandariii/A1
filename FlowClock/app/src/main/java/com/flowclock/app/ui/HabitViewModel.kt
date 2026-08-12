package com.flowclock.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowclock.app.data.HabitDatabase
import com.flowclock.app.data.HabitEntity
import com.flowclock.app.data.HabitRepository
import com.flowclock.app.util.DateUtils
import com.flowclock.app.widget.HabitWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HabitRepository(HabitDatabase.getInstance(application).habitDao())

    val habits: StateFlow<List<HabitEntity>> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHabit(name: String, colorHex: String, target: Int) {
        viewModelScope.launch {
            repository.addHabit(name, colorHex, target)
            HabitWidgetProvider.refreshAllWidgets(getApplication())
        }
    }

    fun toggleHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.updateHabit(
                habit.copy(
                    isCompletedToday = !habit.isCompletedToday,
                    lastCompletedDate = DateUtils.today()
                )
            )
            HabitWidgetProvider.refreshAllWidgets(getApplication())
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            HabitWidgetProvider.refreshAllWidgets(getApplication())
        }
    }
}
