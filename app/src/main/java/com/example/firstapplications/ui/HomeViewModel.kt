package com.example.firstapplications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapplications.data.AttendanceRecordEntity
import com.example.firstapplications.data.AttendanceStatus
import com.example.firstapplications.data.StudentGroup
import com.example.firstapplications.data.SubjectStats
import com.example.firstapplications.data.TodayClass
import com.example.firstapplications.data.TrackerDao
import com.example.firstapplications.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val todayClasses: List<TodayClass> = emptyList(),
    val overallPercentage: Float = 0f,
    val subjectStats: List<SubjectStats> = emptyList()
)

class HomeViewModel(
    private val dao: TrackerDao,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val selectedGroup = userPrefs.selectedGroup

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = selectedGroup.flatMapLatest { group ->
        val groupStr = group?.name ?: StudentGroup.G1_G3.name
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 1 = Monday, 7 = Sunday
        
        kotlinx.coroutines.flow.combine(
            dao.getTodayClasses(dayOfWeek, groupStr, today),
            dao.getAllSubjectStats()
        ) { todayClasses, stats ->
            val totalPresent = stats.sumOf { it.presentCount }
            val totalConducted = stats.sumOf { it.totalConducted }
            val overallPercentage = if (totalConducted > 0) (totalPresent.toFloat() / totalConducted) * 100 else 0f

            HomeUiState(
                todayClasses = todayClasses,
                overallPercentage = overallPercentage,
                subjectStats = stats
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun markAttendance(timetableId: Int, status: AttendanceStatus) {
        viewModelScope.launch {
            dao.insertAttendance(
                AttendanceRecordEntity(
                    timetableId = timetableId,
                    date = LocalDate.now(),
                    status = status
                )
            )
        }
    }

    // Mathematical logic to be used by the UI
    companion object {
        fun calculateRequiredClasses(present: Int, conducted: Int): Int {
            if (conducted == 0) return 0
            val currentPercent = (present.toFloat() / conducted) * 100
            if (currentPercent >= 75f) return 0
            return (3 * conducted - 4 * present) // Will always be positive if < 75%
        }

        fun calculateSafeBunks(present: Int, conducted: Int): Int {
            if (conducted == 0) return 0
            val currentPercent = (present.toFloat() / conducted) * 100
            if (currentPercent <= 75f) return 0
            return (4 * present - 3 * conducted) / 3 // Rounded down automatically by integer division
        }
    }
}