package com.acagribahar.muscleandmindapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding // Padding import
import androidx.compose.material3.* // Material 3 importları
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // getValue import
import androidx.compose.ui.Modifier // Modifier import
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hierarchy // hierarchy import
import androidx.navigation.NavGraph.Companion.findStartDestination // findStartDestination import
import androidx.navigation.NavHostController
import androidx.navigation.compose.* // navigation-compose importları
import androidx.navigation.navigation // nested navigation import
import com.acagribahar.muscleandmindapp.data.local.AppDatabase
import com.acagribahar.muscleandmindapp.data.repository.TaskRepositoryImpl
import com.acagribahar.muscleandmindapp.navigation.* // Rota sabitleri import
import com.acagribahar.muscleandmindapp.navigation.Screen.AuthScreen
import com.acagribahar.muscleandmindapp.navigation.Screen.Graph
import com.acagribahar.muscleandmindapp.ui.screens.* // Ekranlar import
import com.acagribahar.muscleandmindapp.ui.screens.auth.* // Auth Ekranları import
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModel
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModelFactory
import com.acagribahar.muscleandmindapp.ui.theme.MindMuscleAppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var homeViewModelFactory: HomeViewModelFactory
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Repository, Factory ve ViewModel'ı burada başlat
        val database = AppDatabase.getDatabase(applicationContext)
        taskRepository = TaskRepositoryImpl(database.taskDao(), applicationContext)
        homeViewModelFactory = HomeViewModelFactory(taskRepository)
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

        setContent {
            MindMuscleAppTheme {
                // Üst seviye NavController
                val navController = rememberNavController()

                // Başlangıç noktasını belirle (Auth mu Main mi?)
                val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                    Graph.MAIN
                } else {
                    Graph.AUTHENTICATION
                }

                // --- ÜST SEVİYE NavHost ---
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    route = Graph.ROOT // Bu NavHost'un kök rotası
                ) {
                    // --- Kimlik Doğrulama Grafiği ---
                    navigation(
                        startDestination = AuthScreen.LOGIN,
                        route = Graph.AUTHENTICATION // Bu grafiğin rotası
                    ) {
                        composable(AuthScreen.LOGIN) {
                            LoginScreen(
                                onLoginSuccess = {
                                    // Ana grafiğe git, auth grafiğini temizle
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
                                    // Ana grafiğe git, auth grafiğini temizle
                                    navController.navigate(Graph.MAIN) {
                                        popUpTo(Graph.AUTHENTICATION) { inclusive = true }
                                    }
                                },
                                navigateToLogin = {
                                    navController.popBackStack() // Login'e geri dön
                                }
                            )
                        }
                    } // --- Kimlik Doğrulama Grafiği Sonu ---

                    // --- Ana Uygulama Grafiği ---
                    composable(Graph.MAIN) { // Ana rota çağrıldığında...
                        // ...MainAppScreen'i çağır ve gerekli parametreleri ilet
                        MainAppScreen(
                            navController = navController, // Üst seviye controller (Logout için)
                            homeViewModel = homeViewModel   // HomeViewModel
                        )
                    } // --- Ana Uygulama Grafiği Sonu ---

                } // --- ÜST SEVİYE NavHost Sonu ---
            }
        }
    }
}

// --- MainAppScreen Composable (Scaffold ve İç Navigasyonu içerir) ---
@OptIn(ExperimentalMaterial3Api::class) // Scaffold için
@Composable
fun MainAppScreen(
    navController: NavHostController, // Üst seviye Controller (Settings'e gidecek)
    homeViewModel: HomeViewModel    // HomeScreen'e gidecek ViewModel
) {
    // İç navigasyon (alt sekmeler arası) için ayrı bir Controller
    val mainNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar { // Material 3 Bottom Navigation Bar
                val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            // Alt sekmeler arası geçiş için İÇ mainNavController kullanılır
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
        // --- İÇ NavHost (Ana ekranlar arası geçiş) ---
        NavHost(
            navController = mainNavController, // İÇ Controller kullanılır
            startDestination = Screen.Home.route, // Başlangıç ekranı Home
            modifier = Modifier.padding(innerPadding) // Padding uygulanır
        ) {
            composable(Screen.Home.route) {
                // HomeScreen'e ViewModel iletilir
                HomeScreen(homeViewModel = homeViewModel)
            }
            composable(Screen.Exercises.route) { ExercisesScreen() } // Henüz parametre almıyor
            composable(Screen.MindTasks.route) { MindTasksScreen() } // Henüz parametre almıyor
            composable(Screen.Progress.route) { ProgressScreen() }   // Henüz parametre almıyor
            composable(Screen.Settings.route) {
                // SettingsScreen'e ÜST SEVİYE navController iletilir (Logout için)
                SettingsScreen(navController = navController)
            }
        } // --- İÇ NavHost Sonu ---
    }
} // --- MainAppScreen Sonu ---

// Not: HomeScreen, SettingsScreen vb. Composable fonksiyonlarının tanımları
// bu dosyanın dışında (kendi dosyalarında) olabilir. Sadece MainAppScreen içindeki
// çağrıların doğru parametreleri aldığından emin olun.