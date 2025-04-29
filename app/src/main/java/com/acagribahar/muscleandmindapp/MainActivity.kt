package com.acagribahar.muscleandmindapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation // nested navigation için import
import com.acagribahar.muscleandmindapp.navigation.* // Rotaları import et
import com.acagribahar.muscleandmindapp.navigation.Screen.AuthScreen
import com.acagribahar.muscleandmindapp.navigation.Screen.Graph
import com.acagribahar.muscleandmindapp.ui.screens.* // Ana ekranları import et
import com.acagribahar.muscleandmindapp.ui.screens.auth.* // Auth ekranlarını import et
import com.acagribahar.muscleandmindapp.ui.theme.MindMuscleAppTheme
import com.google.firebase.auth.FirebaseAuth // Firebase Auth import et
import androidx.compose.runtime.getValue // Bu importu ekleyin
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation // nested navigation için import

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MindMuscleAppTheme {
                val navController = rememberNavController()
                val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                    Graph.MAIN
                } else {
                    Graph.AUTHENTICATION
                }

                NavHost( // Üst Seviye NavHost
                    navController = navController,
                    startDestination = startDestination,
                    route = Graph.ROOT
                ) {
                    // Kimlik Doğrulama Grafiği (Değişiklik yok)
                    navigation(
                        startDestination = AuthScreen.LOGIN,
                        route = Graph.AUTHENTICATION
                    ) {
                        composable(AuthScreen.LOGIN) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(Graph.MAIN) {
                                        popUpTo(Graph.AUTHENTICATION) { inclusive = true }
                                    }
                                },
                                navigateToRegister = {
                                    navController.navigate(AuthScreen.REGISTER)
                                }
                            )
                        }
                        composable(AuthScreen.REGISTER) {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate(Graph.MAIN) {
                                        popUpTo(Graph.AUTHENTICATION) { inclusive = true }
                                    }
                                },
                                navigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    // Ana Uygulama Ekranı (BURAYI GÜNCELLEDİK)
                    // navigation(...) yerine doğrudan composable kullanıyoruz.
                    composable(Graph.MAIN) { // Bu rota çağrıldığında...
                        MainAppScreen(navController = navController) // ... MainAppScreen'i göster.
                        // MainAppScreen kendi Scaffold ve iç NavHost'unu yönetir.
                    }
                }
            }
        }
    }
}

// MainAppScreen fonksiyonunda değişiklik yapmaya gerek yok, önceki haliyle kalabilir.
@Composable
fun MainAppScreen(navController: NavHostController)
 {
    val mainNavController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            mainNavController.navigate(screen.route) {
                                popUpTo(mainNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost( // MainAppScreen içindeki NavHost
            navController = mainNavController, // İç Controller'ı kullanır
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Exercises.route) { ExercisesScreen() }
            composable(Screen.MindTasks.route) { MindTasksScreen() }
            composable(Screen.Progress.route) { ProgressScreen() }
            composable(Screen.Settings.route) { SettingsScreen(
                navController = navController // Üst seviye navController'ı SettingsScreen'e iletiyoruz

            ) }
        }
    }
}