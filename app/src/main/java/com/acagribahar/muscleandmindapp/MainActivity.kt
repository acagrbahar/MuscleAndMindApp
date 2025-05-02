package com.acagribahar.muscleandmindapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding // Padding import
import androidx.compose.material3.* // Material 3 importları
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // getValue import
import androidx.compose.ui.Modifier // Modifier import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy // hierarchy import
import androidx.navigation.NavGraph.Companion.findStartDestination // findStartDestination import
import androidx.navigation.NavHostController
import androidx.navigation.compose.* // navigation-compose importları
import androidx.navigation.navigation // nested navigation import
import com.acagribahar.muscleandmindapp.data.local.AppDatabase
import com.acagribahar.muscleandmindapp.data.repository.TaskRepositoryImpl
import com.acagribahar.muscleandmindapp.navigation.* // Rota sabitleri import
import com.acagribahar.muscleandmindapp.navigation.Screen.AddExerciseDestinations
import com.acagribahar.muscleandmindapp.navigation.Screen.AuthScreen
import com.acagribahar.muscleandmindapp.navigation.Screen.ExerciseDestinations
import com.acagribahar.muscleandmindapp.navigation.Screen.Graph
import com.acagribahar.muscleandmindapp.navigation.Screen.MindTaskDestinations
import com.acagribahar.muscleandmindapp.ui.screens.* // Ekranlar import
import com.acagribahar.muscleandmindapp.ui.screens.auth.* // Auth Ekranları import
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModel
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModelFactory
import com.acagribahar.muscleandmindapp.ui.screens.exercises.AddExerciseScreen
import com.acagribahar.muscleandmindapp.ui.screens.exercises.AddExerciseViewModel
import com.acagribahar.muscleandmindapp.ui.screens.exercises.AddExerciseViewModelFactory
import com.acagribahar.muscleandmindapp.ui.screens.exercises.ExerciseDetailScreen
import com.acagribahar.muscleandmindapp.ui.theme.MindMuscleAppTheme
import com.acagribahar.muscleandmindapp.ui.screens.exercises.ExercisesViewModel // ViewModel import
import com.acagribahar.muscleandmindapp.ui.screens.exercises.ExercisesViewModelFactory // Factory import
import com.acagribahar.muscleandmindapp.ui.screens.mindtasks.MindTaskDetailScreen
import com.acagribahar.muscleandmindapp.ui.screens.mindtasks.MindTasksViewModel // ViewModel import
import com.acagribahar.muscleandmindapp.ui.screens.mindtasks.MindTasksViewModelFactory // Factory import
import com.acagribahar.muscleandmindapp.ui.screens.progress.ProgressViewModel // ViewModel import
import com.acagribahar.muscleandmindapp.ui.screens.progress.ProgressViewModelFactory // Factory import
import com.acagribahar.muscleandmindapp.ui.screens.settings.SettingsUiState
import com.acagribahar.muscleandmindapp.ui.screens.settings.SettingsViewModel // ViewModel import
import com.acagribahar.muscleandmindapp.ui.screens.settings.SettingsViewModelFactory // Factory import
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var taskRepository: TaskRepositoryImpl

    //ViewModel'lar
    private lateinit var exercisesViewModel: ExercisesViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mindTasksViewModel: MindTasksViewModel
    private lateinit var progressViewModel: ProgressViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var addExerciseViewModel: AddExerciseViewModel




    //Factory'ler
    private lateinit var homeViewModelFactory: HomeViewModelFactory
    private lateinit var exercisesViewModelFactory: ExercisesViewModelFactory
    private lateinit var mindTasksViewModelFactory: MindTasksViewModelFactory
    private lateinit var progressViewModelFactory: ProgressViewModelFactory
    private lateinit var settingsViewModelFactory: SettingsViewModelFactory
    private lateinit var addExerciseViewModelFactory: AddExerciseViewModelFactory

    private lateinit var firebaseAuth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()


        // Repository, Factory ve ViewModel'ı burada başlat
        val database = AppDatabase.getDatabase(applicationContext)
        taskRepository = TaskRepositoryImpl(database.taskDao(), applicationContext)

        homeViewModelFactory = HomeViewModelFactory(taskRepository,firebaseAuth)
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

        // ExercisesViewModel için de aynı işlemi yap
        exercisesViewModelFactory = ExercisesViewModelFactory(taskRepository,firebaseAuth)
        exercisesViewModel = ViewModelProvider(this, exercisesViewModelFactory)[ExercisesViewModel::class.java]

        // MindTasksViewModel için de aynı işlemi yap
        mindTasksViewModelFactory = MindTasksViewModelFactory(taskRepository)
        mindTasksViewModel = ViewModelProvider(this, mindTasksViewModelFactory)[MindTasksViewModel::class.java]

        // ProgressViewModel için de aynı işlemi yap
        progressViewModelFactory = ProgressViewModelFactory(taskRepository,firebaseAuth)
        progressViewModel = ViewModelProvider(this, progressViewModelFactory)[ProgressViewModel::class.java]

        // <<< SettingsViewModelFactory başlatmasını güncelle (firebaseAuth'ı geç) >>>
        settingsViewModelFactory = SettingsViewModelFactory(taskRepository, firebaseAuth)
        settingsViewModel = ViewModelProvider(this, settingsViewModelFactory)[SettingsViewModel::class.java]


        addExerciseViewModelFactory = AddExerciseViewModelFactory(taskRepository, firebaseAuth)
        addExerciseViewModel = ViewModelProvider(this, addExerciseViewModelFactory)[AddExerciseViewModel::class.java]



        setContent {
            MindMuscleAppTheme {
                // Üst seviye NavController
                val navController = rememberNavController()

                val currentUser = firebaseAuth.currentUser
                // Başlangıç noktasını belirle (Auth mu Main mi?)
                val startDestination = if (currentUser != null) {
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
                    composable(Graph.MAIN) {
                        MainAppScreen(
                            navController = navController,
                            homeViewModel = homeViewModel,
                            exercisesViewModel = exercisesViewModel,
                            mindTasksViewModel = mindTasksViewModel,
                            progressViewModel = progressViewModel,
                            settingsViewModel = settingsViewModel,
                            addExerciseViewModel = addExerciseViewModel,
                            // <<< SettingsViewModel'ın UI state'ini de geçelim >>>
                            settingsUiState = settingsViewModel.uiState.collectAsStateWithLifecycle().value, // <<< State'i burada toplayıp değeri geç
                            // <<< onLogout lambda'sında firebaseAuth instance'ını kullan >>>
                            onLogout = {
                                firebaseAuth.signOut() // getInstance() ile alınan instance
                                navController.navigate(Graph.AUTHENTICATION) {
                                    popUpTo(Graph.MAIN) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
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
    mindTasksViewModel: MindTasksViewModel,
    progressViewModel: ProgressViewModel,
    settingsViewModel: SettingsViewModel,
    addExerciseViewModel : AddExerciseViewModel,
    settingsUiState: SettingsUiState,
    onLogout: () -> Unit




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

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = mainNavController, // İÇ Controller kullanılır
                startDestination = Screen.Home.route, // Başlangıç ekranı Home
                modifier = Modifier.weight(1f) // Padding uygulanır
            ) {
                composable(Screen.Home.route) {
                    // HomeScreen'e ViewModel iletilir
                    HomeScreen(homeViewModel = homeViewModel)
                }


                composable(Screen.Exercises.route) {
                    ExercisesScreen(
                        exercisesViewModel = exercisesViewModel,
                        onExerciseClick = { displayExercise ->
                            // Navigasyonda argüman olarak başlığı gönderirken URL encode etmek iyi olabilir
                            val encodedTitle = java.net.URLEncoder.encode(displayExercise.title, "UTF-8")
                            mainNavController.navigate("${ExerciseDestinations.EXERCISE_DETAIL_ROUTE}/${displayExercise.id}")
                        },
                        // <<< Yeni egzersiz ekleme ekranına gitmek için lambda >>>
                        navigateToAddExercise = {
                            mainNavController.navigate(AddExerciseDestinations.ROUTE) // Yeni rotaya git
                        }

                    )
                }


                composable(
                    route = ExerciseDestinations.routeWithArgs, // "exercise_detail/{exerciseId}" olmalı
                    arguments = ExerciseDestinations.arguments // ARG_EXERCISE_ID tanımını içeriyor olmalı
                ) { backStackEntry ->
                    // <<< Argümanı doğru isimle (ARG_EXERCISE_ID) alın >>>
                    val exerciseId = backStackEntry.arguments?.getString(ExerciseDestinations.ARG_EXERCISE_ID)

                    if (exerciseId != null) {
                        // <<< ExerciseDetailScreen'i doğru parametre (exerciseId) ile çağırın >>>
                        ExerciseDetailScreen(
                            exerciseId = exerciseId, // exerciseTitle yerine exerciseId
                            exercisesViewModel = exercisesViewModel,
                            navController = mainNavController // Geri gitmek için iç controller
                        )
                    } else {
                        // Başlık/ID yoksa geri dön (iç controller ile)
                        mainNavController.popBackStack() // <<< navController yerine mainNavController olmalı
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


                composable(Screen.Progress.route) {
                    ProgressScreen(progressViewModel = progressViewModel)
                }

                composable(AddExerciseDestinations.ROUTE) {
                    AddExerciseScreen(
                        navController = mainNavController, // Geri gitmek için iç controller
                        addExerciseViewModel = addExerciseViewModel
                    )
                }


                composable(Screen.Settings.route) {

                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onLogout = onLogout


                    )
                }
            }

            // <<< Banner Reklam Alanı (Sadece ücretsiz kullanıcılar için) >>>
            if (!settingsUiState.isPremium && !settingsUiState.isLoading) { // Yüklenmiyorsa ve premium değilse göster
                // AndroidView kullanarak AdView'ı Compose'a ekle
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth().height(50.dp),
                    factory = { context ->
                        Log.d("AdMob", "Creating AdView...") // <<< Log Ekleyin

                        AdView(context).apply {
                            // <<< BURAYA TEST REKLAM BİRİMİ KİMLİĞİNİZİ YAZIN >>>
                            adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID'si
                            setAdSize(AdSize.BANNER) // Veya AdSize.FULL_BANNER vb.

                            // <<< AdListener Ekleyin >>>
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    // Reklam başarıyla yüklendiğinde logla
                                    Log.d("AdMob", "Banner Ad loaded successfully.")
                                }

                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                    // Reklam yüklenemediğinde hatayı logla
                                    Log.e("AdMob", "Banner Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                                    // Hatanın domain'ini ve diğer detayları da loglayabilirsiniz: loadAdError.toString()
                                }

                                override fun onAdOpened() {
                                    // Reklam açıldığında (tıklandığında) logla
                                    Log.d("AdMob", "Banner Ad opened.")
                                }

                                override fun onAdClicked() {
                                    // Reklam tıklandığında logla
                                    Log.d("AdMob", "Banner Ad clicked.")
                                }

                                override fun onAdClosed() {
                                    // Reklam kapatıldığında (genellikle tam ekran reklamlarda olur)
                                    Log.d("AdMob", "Banner Ad closed.")
                                }
                            }

                            // Reklamı yükle
                            Log.d("AdMob", "Requesting Ad...") // <<< Log Ekleyin
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }






        }

         // --- İÇ NavHost Sonu ---
    }
} // --- MainAppScreen Sonu ---

// Not: HomeScreen, SettingsScreen vb. Composable fonksiyonlarının tanımları
// bu dosyanın dışında (kendi dosyalarında) olabilir. Sadece MainAppScreen içindeki
// çağrıların doğru parametreleri aldığından emin olun.