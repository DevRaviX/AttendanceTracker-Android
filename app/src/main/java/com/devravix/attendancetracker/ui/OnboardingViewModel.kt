package com.devravix.attendancetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devravix.attendancetracker.data.DatabaseSeeder
import com.devravix.attendancetracker.data.StudentGroup
import com.devravix.attendancetracker.data.UserPreferencesRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val userPrefs: UserPreferencesRepository,
    private val seeder: DatabaseSeeder
) : ViewModel() {

    fun completeOnboarding(semester: String, group: StudentGroup, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Save preferences
            userPrefs.completeOnboarding(semester, group)
            // Seed the database with the timetable logic for this specific group
            seeder.seedDatabaseIfNeeded(group)
            // Navigate away
            onComplete()
        }
    }
}