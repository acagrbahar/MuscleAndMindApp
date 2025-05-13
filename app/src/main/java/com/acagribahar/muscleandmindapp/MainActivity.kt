package com.acagribahar.muscleandmindapp

// --- Gerekli Importlar ---
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
import com.acagribahar.muscleandmindapp.biling.BillingClientWrapper
// --- AdMob ve UMP Importları ---
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds // MobileAds SDK başlatma için
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.ump.* // UMP SDK için tüm importlar
import java.util.concurrent.atomic.AtomicBoolean // SDK'nın bir kez başlatılması için
// --- Veri ve Repository Importları ---
import com.acagribahar.muscleandmindapp.data.local.AppDatabase
import com.acagribahar.muscleandmindapp.data.repository.TaskRepositoryImpl
// --- Navigasyon Sabitleri Importları (Doğrudan) ---
import com.acagribahar.muscleandmindapp.navigation.Screen
import com.acagribahar.muscleandmindapp.navigation.Screen.AddExerciseDestinations
import com.acagribahar.muscleandmindapp.navigation.Screen.AuthScreen
import com.acagribahar.muscleandmindapp.navigation.Screen.ExerciseDestinations
import com.acagribahar.muscleandmindapp.navigation.Screen.Graph
import com.acagribahar.muscleandmindapp.navigation.Screen.MindTaskDestinations
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
    private lateinit var billingClientWrapper: BillingClientWrapper

    // UMP SDK için değişkenler
    private lateinit var consentInformation: ConsentInformation
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val mainActivityTag = "MainActivityUMPConsent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UMP SDK Kurulumu ve İzin Formu Mantığı
        // Test parametreleri (geliştirme için)
        // TODO: YAYINLAMADAN ÖNCE DEBUG AYARLARINI KALDIRIN VEYA YORUM SATIRI YAPIN!
        val debugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA) // Testi EEA bölgesindeymiş gibi yap
            .addTestDeviceHashedId("B3EEABB8EE11C2BE770B684D95219ECB") // <<< Logcat'ten alıp buraya yapıştırın
            .build()




        val params = ConsentRequestParameters.Builder()
            //.setConsentDebugSettings(debugSettings) // <<< Test için bu satırı aktif edin
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                Log.d(mainActivityTag, "Consent info update success. Can request ads: ${consentInformation.canRequestAds()}")
                // Gerekirse ve mevcutsa formu yükle ve göster
                if (consentInformation.isConsentFormAvailable) {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { loadAndShowError ->
                        if (loadAndShowError != null) {
                            Log.e(mainActivityTag, "Error loading or showing consent form: ${loadAndShowError.message}")
                        } else {
                            Log.d(mainActivityTag, "Consent form shown and dismissed (or not needed at this specific call).")
                        }
                        // Form gösterildikten sonra (veya gerekmiyorsa) reklamları başlat.
                        initializeMobileAdsSdk()
                    }
                } else {
                    Log.d(mainActivityTag, "Consent form not available. Initializing ads.")
                    initializeMobileAdsSdk() // Form mevcut değilse direkt reklamları başlat
                }
            },
            { requestConsentError ->
                Log.e(mainActivityTag, "Consent info update failed: ${requestConsentError.message}")
                initializeMobileAdsSdk() // Hata durumunda da reklamları başlatmayı dene
            }
        )




        // Diğer başlatmalar (Firebase, DB, Repository, ViewModel'lar)
        firebaseAuth = FirebaseAuth.getInstance()
        val database = AppDatabase.getDatabase(applicationContext)
        taskRepository = TaskRepositoryImpl(database.taskDao(), applicationContext)
        billingClientWrapper = BillingClientWrapper(applicationContext)

        homeViewModelFactory = HomeViewModelFactory(taskRepository, firebaseAuth)
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]
        exercisesViewModelFactory = ExercisesViewModelFactory(taskRepository, firebaseAuth)
        exercisesViewModel = ViewModelProvider(this, exercisesViewModelFactory)[ExercisesViewModel::class.java]
        mindTasksViewModelFactory = MindTasksViewModelFactory(taskRepository)
        mindTasksViewModel = ViewModelProvider(this, mindTasksViewModelFactory)[MindTasksViewModel::class.java]
        progressViewModelFactory = ProgressViewModelFactory(taskRepository, firebaseAuth)
        progressViewModel = ViewModelProvider(this, progressViewModelFactory)[ProgressViewModel::class.java]
        settingsViewModelFactory = SettingsViewModelFactory(taskRepository, firebaseAuth, application, billingClientWrapper)
        settingsViewModel = ViewModelProvider(this, settingsViewModelFactory)[SettingsViewModel::class.java]
        addExerciseViewModelFactory = AddExerciseViewModelFactory(taskRepository, firebaseAuth)
        addExerciseViewModel = ViewModelProvider(this, addExerciseViewModelFactory)[AddExerciseViewModel::class.java]

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            MindMuscleAppTheme(themePreference = settingsState.currentTheme) {
                val navController = rememberNavController()
                val currentUser = firebaseAuth.currentUser
                val startDestination = if (currentUser != null) Graph.MAIN else AuthScreen.LOGIN

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    route = Graph.ROOT
                ) {
                    // --- Auth Ekranları ---
                    composable(AuthScreen.LOGIN) {
                        LoginScreen(
                            onLoginSuccess = { navController.navigate(Graph.MAIN) { popUpTo(Graph.ROOT) { inclusive = true } } },
                            navigateToRegister = { navController.navigate(AuthScreen.REGISTER) }
                        )
                    }
                    composable(AuthScreen.REGISTER) {
                        RegisterScreen(
                            onRegisterSuccess = { navController.navigate(Graph.MAIN) { popUpTo(Graph.ROOT) { inclusive = true } } },
                            navigateToLogin = { navController.navigate(AuthScreen.LOGIN) { popUpTo(AuthScreen.LOGIN) { inclusive = true } } }
                        )
                    }
                    // --- Ana Uygulama Grafiği ---
                    composable(Graph.MAIN) {
                        MainAppScreen(
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
                                        homeViewModel.clearAllLocalTasks()
                                        firebaseAuth.signOut()
                                        navController.navigate(AuthScreen.LOGIN) {
                                            popUpTo(Graph.ROOT) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } catch (e: Exception) { Log.e("Logout", "Error during logout process", e) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Mobile Ads SDK'sını başlatan fonksiyon
    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return // Sadece bir kere çağrıldığından emin ol
        }
        Log.d(mainActivityTag, "Attempting to initialize Mobile Ads SDK...")
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(mainActivityTag, "Adapter class: $adapterClass, state: ${status?.initializationState}, description: ${status?.description}")
            }
            Log.d(mainActivityTag, "Mobile Ads SDK initialized completely after UMP flow.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called, ending billing connection.")
        billingClientWrapper.endConnection()
    }
}


// --- MainAppScreen Composable (İç Navigasyonu içerir - Sizin gönderdiğiniz gibi) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
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
    val context = LocalContext.current // showInterstitialAdThenNavigate içinde kullanılacak
    val isPremium by exercisesViewModel.isPremium.collectAsStateWithLifecycle()
    val loadedInterstitialAd by exercisesViewModel.interstitialAd.collectAsStateWithLifecycle()

    // Reklamı gösterip sonra navigate eden yardımcı fonksiyon
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
            NavHost(
                navController = mainNavController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Home.route) { HomeScreen(homeViewModel = homeViewModel) }
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
                composable(Screen.Progress.route) { ProgressScreen(progressViewModel = progressViewModel) }
                composable(AddExerciseDestinations.ROUTE) { AddExerciseScreen(mainNavController, addExerciseViewModel) }
                composable(Screen.Settings.route) {
                    SettingsScreen(settingsViewModel = settingsViewModel, onLogout = onLogout)
                }
            } // İç NavHost Sonu

            // Banner Reklam
            if (!settingsUiState.isPremium && !settingsUiState.isLoading) { // isLoadingBilling yerine isLoading (SettingsUiState'den)
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            adUnitId = "ca-app-pub-1292674096792479/2220600505" // GERÇEK BANNER ID'NİZ
                            setAdSize(AdSize.BANNER)
                            adListener = object : AdListener() {
                                override fun onAdLoaded() { Log.d("AdMob", "Banner Ad loaded successfully.") }
                                override fun onAdFailedToLoad(loadAdError: LoadAdError) { Log.e("AdMob", "Banner Ad failed: ${loadAdError.message}") }
                            }
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        } // Column Sonu
    } // Scaffold Sonu
} // MainAppScreen Sonu