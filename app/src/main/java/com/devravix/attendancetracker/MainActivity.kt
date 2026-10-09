package com.devravix.attendancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devravix.attendancetracker.data.AppDatabase
import com.devravix.attendancetracker.data.DatabaseSeeder
import com.devravix.attendancetracker.data.UserPreferencesRepository
import com.devravix.attendancetracker.data.dataStore
import com.devravix.attendancetracker.ui.HomeViewModel
import com.devravix.attendancetracker.ui.OnboardingViewModel
import com.devravix.attendancetracker.ui.SubjectDetailViewModel
import com.devravix.attendancetracker.ui.screens.AllSubjectsScreen
import com.devravix.attendancetracker.ui.screens.HomeScreen
import com.devravix.attendancetracker.ui.screens.OnboardingScreen
import com.devravix.attendancetracker.ui.screens.SubjectDetailScreen
import com.devravix.attendancetracker.ui.theme.FirstApplicationsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val dao = database.trackerDao()
        val userPrefs = UserPreferencesRepository(this.dataStore)
        val seeder = DatabaseSeeder(dao)

        enableEdgeToEdge()
        setContent {
            FirstApplicationsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isOnboardingComplete by userPrefs.isOnboardingComplete.collectAsState(initial = null)
                    
                    if (isOnboardingComplete == null) {
                        // Loading state while DataStore is read
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AttendanceTrackerApp(
                            startDestination = if (isOnboardingComplete == true) "home" else "onboarding",
                            userPrefs = userPrefs,
                            seeder = seeder,
                            dao = dao
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceTrackerApp(
    startDestination: String,
    userPrefs: UserPreferencesRepository,
    seeder: DatabaseSeeder,
    dao: com.devravix.attendancetracker.data.TrackerDao
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(animationSpec = tween(400), initialOffsetX = { it }) + fadeIn(animationSpec = tween(400)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { -it }) + fadeOut(animationSpec = tween(400)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(400), initialOffsetX = { -it }) + fadeIn(animationSpec = tween(400)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { it }) + fadeOut(animationSpec = tween(400)) }
    ) {
        composable("onboarding") {
            val viewModel: OnboardingViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return OnboardingViewModel(userPrefs, seeder) as T
                    }
                }
            )
            OnboardingScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            val viewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(dao, userPrefs) as T
                    }
                }
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSubject = { subjectId ->
                    navController.navigate("subject/$subjectId")
                },
                onNavigateToAllSubjects = {
                    navController.navigate("all_subjects")
                }
            )
        }
        
        composable("all_subjects") {
            val viewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(dao, userPrefs) as T
                    }
                }
            )
            AllSubjectsScreen(
                viewModel = viewModel,
                onNavigateToSubject = { subjectId ->
                    navController.navigate("subject/$subjectId")
                }
            )
        }
        
        composable(
            route = "subject/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.IntType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: return@composable
            val viewModel: SubjectDetailViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SubjectDetailViewModel(dao, subjectId) as T
                    }
                }
            )
            SubjectDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}