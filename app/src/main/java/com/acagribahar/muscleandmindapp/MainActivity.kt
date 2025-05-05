package com.acagribahar.muscleandmindapp

// --- Gerekli Importlar (Temizlenmiş ve Gerekli Olanlar) ---
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
// --- AdMob Importları ---
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
// --- Veri ve Repository Importları ---
import com.acagribahar.muscleandmindapp.data.local.AppDatabase
import com.acagribahar.muscleandmindapp.data.repository.TaskRepositoryImpl
import com.acagribahar.muscleandmindapp.navigation.Screen
import com.acagribahar.muscleandmindapp.navigation.Screen.AddExerciseDestinations
import com.acagribahar.muscleandmindapp.navigation.Screen.AuthScreen
import com.acagribahar.muscleandmindapp.navigation.Screen.ExerciseDestinations
import com.acagribahar.muscleandmindapp.navigation.Screen.Graph
import com.acagribahar.muscleandmindapp.navigation.Screen.MindTaskDestinations
// --- Navigasyon Sabitleri Importları (Doğrudan) ---

// --- Ekranlar ve ViewModel Importları ---
import com.acagribahar.muscleandmindapp.ui.screens.ExercisesScreen
import com.acagribahar.muscleandmindapp.ui.screens.HomeScreen
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModel
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModelFactory
import com.acagribahar.muscleandmindapp.ui.screens.MindTasksScreen
import com.acagribahar.muscleandmindapp.ui.screens.ProgressScreen
import com.acagribahar.muscleandmindapp.ui.screens.SettingsScreen
import com.acagribahar.muscleandmindapp.ui.screens.auth.*
import com.acagribahar.muscleandmindapp.ui.screens.exercises.*
import com.acagribahar.muscleandmindapp.ui.theme.MindMuscleAppTheme
import com.acagribahar.muscleandmindapp.ui.screens.mindtasks.*
import com.acagribahar.muscleandmindapp.ui.screens.progress.*
import com.acagribahar.muscleandmindapp.ui.screens.settings.*
// --- Firebase ve Coroutines ---
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    // --- Değişken tanımları ---
    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var exercisesViewModel: ExercisesViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mindTasksViewModel: MindTasksViewModel
    private lateinit var progressViewModel: ProgressViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var addExerciseViewModel: AddExerciseViewModel
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
        val database = AppDatabase.getDatabase(applicationContext)
        taskRepository = TaskRepositoryImpl(database.taskDao(), applicationContext)

        // --- Factory ve ViewModel başlatmaları (Sizin kodunuzdaki gibi) ---
        homeViewModelFactory = HomeViewModelFactory(taskRepository, firebaseAuth)
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]
        exercisesViewModelFactory = ExercisesViewModelFactory(taskRepository, firebaseAuth)
        exercisesViewModel = ViewModelProvider(this, exercisesViewModelFactory)[ExercisesViewModel::class.java]
        mindTasksViewModelFactory = MindTasksViewModelFactory(taskRepository)
        mindTasksViewModel = ViewModelProvider(this, mindTasksViewModelFactory)[MindTasksViewModel::class.java]
        progressViewModelFactory = ProgressViewModelFactory(taskRepository, firebaseAuth)
        progressViewModel = ViewModelProvider(this, progressViewModelFactory)[ProgressViewModel::class.java]
        settingsViewModelFactory = SettingsViewModelFactory(taskRepository, firebaseAuth, application)
        settingsViewModel = ViewModelProvider(this, settingsViewModelFactory)[SettingsViewModel::class.java]
        addExerciseViewModelFactory = AddExerciseViewModelFactory(taskRepository, firebaseAuth)
        addExerciseViewModel = ViewModelProvider(this, addExerciseViewModelFactory)[AddExerciseViewModel::class.java]
        // --- ---

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            MindMuscleAppTheme(themePreference = settingsState.currentTheme) {
                val navController = rememberNavController()
                val currentUser = firebaseAuth.currentUser

                // <<< Başlangıç Hedefi (Düzleştirilmiş Yapıya Göre) >>>
                val startDestination = if (currentUser != null) Graph.MAIN else AuthScreen.LOGIN

                // #############################################################
                // ### ÜST SEVİYE NavHost (DÜZELTİLMİŞ - Flattened Auth) ###
                // #############################################################
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    route = Graph.ROOT
                ) {
                    // --- Auth Ekranları (Doğrudan NavHost altında) ---
                    composable(AuthScreen.LOGIN) {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(Graph.MAIN) {
                                    popUpTo(Graph.ROOT) { inclusive = true } // Kök'e kadar temizle
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
                                    popUpTo(Graph.ROOT) { inclusive = true } // Kök'e kadar temizle
                                }
                            },
                            navigateToLogin = {
                                navController.navigate(AuthScreen.LOGIN) {
                                    popUpTo(AuthScreen.LOGIN) { inclusive = true } // Login'e kadar temizle
                                }
                            }
                        )
                    }
                    // <<< navigation(Graph.AUTHENTICATION) bloğu SİLİNDİ >>>

                    // --- Ana Uygulama Grafiği ---
                    composable(Graph.MAIN) {
                        MainAppScreen(
                            // navController parametresi artık geçilmiyor
                            homeViewModel = homeViewModel,
                            exercisesViewModel = exercisesViewModel,
                            mindTasksViewModel = mindTasksViewModel,
                            progressViewModel = progressViewModel,
                            settingsViewModel = settingsViewModel,
                            addExerciseViewModel = addExerciseViewModel,
                            settingsUiState = settingsState,
                            onLogout = {
                                scope.launch {
                                    try {
                                        Log.d("Logout", "Clearing local tasks...")
                                        homeViewModel.clearAllLocalTasks()
                                        Log.d("Logout", "Local tasks cleared. Signing out...")
                                        firebaseAuth.signOut()
                                        // <<< Login ekranına git ve köke kadar temizle >>>
                                        navController.navigate(AuthScreen.LOGIN) { // <<< Hedef güncellendi
                                            popUpTo(Graph.ROOT) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        Log.d("Logout", "Navigation triggered.")
                                    } catch (e: Exception) {
                                        Log.e("Logout", "Error during logout process", e)
                                    }
                                }
                            }
                        )
                    } // --- Ana Uygulama Grafiği Sonu ---

                } // --- ÜST SEVİYE NavHost Sonu ---
            } // MindMuscleAppTheme Sonu
        } // setContent Sonu
    } // onCreate Sonu
} // MainActivity Sonu


// #############################################################
// ### MainAppScreen Composable (Sizin Gönderdiğiniz Koddaki Haliyle) ###
// #############################################################
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    // navController: NavHostController, // <<< KALDIRILMIŞTI (Doğru)
    homeViewModel: HomeViewModel,
    exercisesViewModel: ExercisesViewModel,
    mindTasksViewModel: MindTasksViewModel,
    progressViewModel: ProgressViewModel,
    settingsViewModel: SettingsViewModel,
    addExerciseViewModel : AddExerciseViewModel,
    settingsUiState: SettingsUiState,
    onLogout: () -> Unit
) {
    val mainNavController = rememberNavController()
    val context = LocalContext.current
    val isPremium by exercisesViewModel.isPremium.collectAsStateWithLifecycle()
    val loadedInterstitialAd by exercisesViewModel.interstitialAd.collectAsStateWithLifecycle()

    // Reklamı gösterip sonra navigate eden yardımcı fonksiyon (Değişiklik yok)
    fun showInterstitialAdThenNavigate(destinationRoute: String, activity: Activity?, adToShow: InterstitialAd?) {
        if (!isPremium && adToShow != null && activity != null) {
            Log.d("AdMob", "Attempting to show Interstitial Ad before navigating to $destinationRoute")
            adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdMob", "Interstitial Ad dismissed.")
                    exercisesViewModel.interstitialAdShown()
                    mainNavController.navigate(destinationRoute)
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e("AdMob", "Interstitial Ad failed to show: ${adError.message}")
                    exercisesViewModel.interstitialAdShown()
                    mainNavController.navigate(destinationRoute)
                }
                override fun onAdShowedFullScreenContent() { Log.d("AdMob", "Interstitial Ad showed.") }
            }
            adToShow.show(activity)
        } else {
            Log.d("AdMob", "Not showing Interstitial Ad (isPremium=$isPremium, adLoaded=${adToShow != null}). Navigating directly.")
            mainNavController.navigate(destinationRoute)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // --- İÇ NavHost (Sizin Kodunuzdaki Haliyle) ---
            NavHost(
                navController = mainNavController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(homeViewModel = homeViewModel)
                }
                composable(Screen.Exercises.route) {
                    ExercisesScreen(
                        exercisesViewModel = exercisesViewModel,
                        onExerciseClick = { displayExercise ->
                            val activity = context as? Activity
                            val destination = "${ExerciseDestinations.EXERCISE_DETAIL_ROUTE}/${displayExercise.id}"
                            showInterstitialAdThenNavigate(destination, activity, loadedInterstitialAd)
                        },
                        navigateToAddExercise = { mainNavController.navigate(AddExerciseDestinations.ROUTE) }
                    )
                }
                composable(ExerciseDestinations.routeWithArgs) { backStackEntry ->
                    val exerciseId = backStackEntry.arguments?.getString(ExerciseDestinations.ARG_EXERCISE_ID)
                    if (exerciseId != null) {
                        ExerciseDetailScreen(exerciseId, exercisesViewModel, mainNavController)
                    } else { mainNavController.popBackStack() }
                }
                composable(Screen.MindTasks.route) {
                    MindTasksScreen(
                        mindTasksViewModel = mindTasksViewModel,
                        onMindTaskClick = { mindTaskDto ->
                            val encodedTitle = URLEncoder.encode(mindTaskDto.title, "UTF-8")
                            mainNavController.navigate("${MindTaskDestinations.MIND_TASK_DETAIL_ROUTE}/$encodedTitle")
                        }
                    )
                }
                composable(MindTaskDestinations.routeWithArgs) { backStackEntry ->
                    val encodedTitle = backStackEntry.arguments?.getString(MindTaskDestinations.ARG_TASK_TITLE)
                    val taskTitle = encodedTitle?.let { URLDecoder.decode(it, "UTF-8") }
                    if (taskTitle != null) {
                        MindTaskDetailScreen(taskTitle, mindTasksViewModel, mainNavController)
                    } else { mainNavController.popBackStack() }
                }
                composable(Screen.Progress.route) {
                    ProgressScreen(progressViewModel = progressViewModel)
                }
                composable(AddExerciseDestinations.ROUTE) {
                    AddExerciseScreen(mainNavController, addExerciseViewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(settingsViewModel = settingsViewModel, onLogout = onLogout)
                }
            } // --- İç NavHost Sonu ---

            // Banner Reklam (Sizin Kodunuzdaki Haliyle)
            if (!settingsUiState.isPremium && !settingsUiState.isLoading) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            adUnitId = "ca-app-pub-3940256099942544/6300978111"
                            //Gercek id : ca-app-pub-1292674096792479/2220600505 yayinlanmadan önce degistir.
                            setAdSize(AdSize.BANNER)
                            adListener = object : AdListener() { /* ... */ }
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            } // Reklam Sonu
        } // Column Sonu
    } // Scaffold Sonu
} // MainAppScreen Sonu