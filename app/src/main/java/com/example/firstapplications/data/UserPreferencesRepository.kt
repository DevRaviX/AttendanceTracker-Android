package com.example.firstapplications.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

enum class StudentGroup { ALL, G1_G3, G2_G4 }

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
    private val SELECTED_SEMESTER = stringPreferencesKey("selected_semester")
    private val SELECTED_GROUP = stringPreferencesKey("selected_group")

    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_COMPLETE] ?: false
    }

    val selectedGroup: Flow<StudentGroup?> = dataStore.data.map { preferences ->
        preferences[SELECTED_GROUP]?.let { StudentGroup.valueOf(it) }
    }

    suspend fun completeOnboarding(semester: String, group: StudentGroup) {
        dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETE] = true
            preferences[SELECTED_SEMESTER] = semester
            preferences[SELECTED_GROUP] = group.name
        }
    }
}