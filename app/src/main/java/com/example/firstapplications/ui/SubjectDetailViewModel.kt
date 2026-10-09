package com.example.firstapplications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapplications.data.AttendanceRecordEntity
import com.example.firstapplications.data.SubjectEntity
import com.example.firstapplications.data.TrackerDao
import com.example.firstapplications.data.AttendanceStatus
import com.example.firstapplications.data.TimetableEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubjectDetailViewModel(
    private val dao: TrackerDao,
    private val subjectId: Int
) : ViewModel() {
    
    val subject: StateFlow<SubjectEntity?> = dao.getSubjectById(subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val records: StateFlow<List<AttendanceRecordEntity>> = dao.getAttendanceForSubject(subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val timetables: StateFlow<List<TimetableEntity>> = dao.getTimetablesForSubject(subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRecord(timetableId: Int, date: java.time.LocalDate, status: AttendanceStatus) {
        viewModelScope.launch {
            dao.insertAttendance(
                AttendanceRecordEntity(
                    timetableId = timetableId,
                    date = date,
                    status = status
                )
            )
        }
    }

    fun updateRecord(record: AttendanceRecordEntity, newStatus: AttendanceStatus) {
        viewModelScope.launch {
            dao.updateAttendance(record.copy(status = newStatus))
        }
    }

    fun deleteRecord(record: AttendanceRecordEntity) {
        viewModelScope.launch {
            dao.deleteAttendance(record)
        }
    }
}