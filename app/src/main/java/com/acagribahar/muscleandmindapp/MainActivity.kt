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
import com.acagribahar.muscleandmindapp.navigation.Screen.ExerciseDestinations
import com.acagribahar.muscleandmindapp.navigation.Screen.Graph
import com.acagribahar.muscleandmindapp.navigation.Screen.MindTaskDestinations
import com.acagribahar.muscleandmindapp.ui.screens.* // Ekranlar import
import com.acagribahar.muscleandmindapp.ui.screens.auth.* // Auth Ekranları import
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModel
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModelFactory
import com.acagribahar.muscleandmindapp.ui.screens.exercises.ExerciseDetailScreen
import com.acagribahar.muscleandmindapp.ui.theme.MindMuscleAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.acagribahar.muscleandmindapp.ui.screens.exercises.ExercisesViewModel // ViewModel import
import com.acagribahar.muscleandmindapp.ui.screens.exercises.ExercisesViewModelFactory // Factory import
import com.acagribahar.muscleandmindapp.ui.screens.mindtasks.MindTaskDetailScreen
import com.acagribahar.muscleandmindapp.ui.screens.mindtasks.MindTasksViewModel // ViewModel import
import com.acagribahar.muscleandmindapp.ui.screens.mindtasks.MindTasksViewModelFactory // Factory import


class MainActivity : ComponentActivity() {

    private lateinit var taskRepository: TaskRepositoryImpl

    //ViewModel'lar
    private lateinit var exercisesViewModel: ExercisesViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mindTasksViewModel: MindTasksViewModel


    //Factory'ler
    private lateinit var homeViewModelFactory: HomeViewModelFactory
    private lateinit var exercisesViewModelFactory: ExercisesViewModelFactory
    private lateinit var mindTasksViewModelFactory: MindTasksViewModelFactory // Yeni Factory



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Repository, Factory ve ViewModel'ı burada başlat
        val database = AppDatabase.getDatabase(applicationContext)
        taskRepository = TaskRepositoryImpl(database.taskDao(), applicationContext)
        homeViewModelFactory = HomeViewModelFactory(taskRepository)
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

        // ExercisesViewModel için de aynı işlemi yap
        exercisesViewModelFactory = ExercisesViewModelFactory(taskRepository)
        exercisesViewModel = ViewModelProvider(this, exercisesViewModelFactory)[ExercisesViewModel::class.java]

        // MindTasksViewModel için de aynı işlemi yap
        mindTasksViewModelFactory = MindTasksViewModelFactory(taskRepository)
        mindTasksViewModel = ViewModelProvider(this, mindTasksViewModelFactory)[MindTasksViewModel::class.java]


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
                            homeViewModel = homeViewModel,
                            exercisesViewModel = exercisesViewModel,
                            mindTasksViewModel = mindTasksViewModel


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
    homeViewModel: HomeViewModel,
    exercisesViewModel: ExercisesViewModel,
    mindTasksViewModel: MindTasksViewModel


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


            composable(Screen.Exercises.route) {
                ExercisesScreen(
                    exercisesViewModel = exercisesViewModel,
                    onExerciseClick = { exerciseDto ->
                        // Navigasyonda argüman olarak başlığı gönderirken URL encode etmek iyi olabilir
                        val encodedTitle = java.net.URLEncoder.encode(exerciseDto.title, "UTF-8")
                        mainNavController.navigate("${ExerciseDestinations.EXERCISE_DETAIL_ROUTE}/$encodedTitle")
                    }

                    )
            }


            composable(
                route = ExerciseDestinations.routeWithArgs, // "exercise_detail/{exerciseTitle}"
                arguments = ExerciseDestinations.arguments // Argüman tanımı
            ) { backStackEntry ->
                // Navigasyondan argümanı al
                val encodedTitle = backStackEntry.arguments?.getString(ExerciseDestinations.ARG_EXERCISE_TITLE)
                val exerciseTitle = encodedTitle?.let { java.net.URLDecoder.decode(it, "UTF-8") }

                if (exerciseTitle != null) {
                    ExerciseDetailScreen(
                        exerciseTitle = exerciseTitle,
                        exercisesViewModel = exercisesViewModel, // ViewModel'ı detay ekranına da iletiyoruz
                        navController = mainNavController // Geri gitmek için iç controller
                    )
                } else {
                    // Hata durumu - başlık alınamadı (opsiyonel: Hata ekranı gösterilebilir)
                    navController.popBackStack() // Veya basitçe geri dön
                }
            }


            composable(Screen.MindTasks.route) {
                // MindTasksScreen'e tıklama olayını iletiyoruz
                MindTasksScreen(
                    mindTasksViewModel = mindTasksViewModel,
                    onMindTaskClick = { mindTaskDto ->
                        // Navigasyonda argüman olarak başlığı gönder
                        val encodedTitle = java.net.URLEncoder.encode(mindTaskDto.title, "UTF-8")
                        mainNavController.navigate("${MindTaskDestinations.MIND_TASK_DETAIL_ROUTE}/$encodedTitle")
                    }
                )
            }

            composable(
                route = MindTaskDestinations.routeWithArgs,
                arguments = MindTaskDestinations.arguments
            ) { backStackEntry ->
                // Navigasyondan argümanı al
                val encodedTitle = backStackEntry.arguments?.getString(MindTaskDestinations.ARG_TASK_TITLE)
                val taskTitle = encodedTitle?.let { java.net.URLDecoder.decode(it, "UTF-8") }

                if (taskTitle != null) {
                    MindTaskDetailScreen(
                        taskTitle = taskTitle,
                        mindTasksViewModel = mindTasksViewModel, // ViewModel'ı detay ekranına iletiyoruz
                        navController = mainNavController // Geri gitmek için iç controller
                    )
                } else {
                    navController.popBackStack() // Başlık yoksa geri dön
                }
            }


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