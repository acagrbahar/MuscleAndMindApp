package com.acagribahar.muscleandmindapp.navigation


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.acagribahar.muscleandmindapp.ui.screens.*


@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route, // Başlangıç ekranı
        modifier = modifier
    ) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.Exercises.route) { ExercisesScreen() }
        composable(Screen.MindTasks.route) { MindTasksScreen() }
        composable(Screen.Progress.route) { ProgressScreen() }
        composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
        // Gelecekte başka ekranlar eklenirse buraya tanımlayacağız.
        // Örn: composable("exercise_detail/{exerciseId}") { ... }
    }
}