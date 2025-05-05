package com.acagribahar.muscleandmindapp.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random // Rastgele seçim için (opsiyonel)

// Home ekranı için UI state'i
data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val dailyProgressPercentage: Float = 0f,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val motivationalQuote: String = "",
    val isLoadingTasks: Boolean = true, // Görevlerin yüklenme durumu
    val isLoadingPremium: Boolean = true, // Premium durumunun yüklenme durumu
    val isPremium: Boolean = false // Kullanıcının premium durumu
)

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest ve stateIn için
class HomeViewModel(
    private val taskRepository: TaskRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val todayTimestamp: Long = getStartOfDayTimestamp()

    // --- Motivasyon Sözleri ---
    private val quotes = listOf(
        "Bugün başla, yarın teşekkür et.", "Küçük adımlar, büyük sonuçlar doğurur.",
        "Disiplin, hedefler ve başarı arasındaki köprüdür.", "Zihnini sakinleştir, vücudun takip edecektir.",
        "Güç dışarıdan değil, içeriden gelir.", "Her gün %1 daha iyi ol."
    )
    private val dailyQuote = selectDailyQuote(todayTimestamp)

    // --- Auth Durumu Akışı ---
    // Kimlik doğrulama durumunu dinleyen ve paylaşan Flow
    private val userStateFlow: SharedFlow<FirebaseUser?> = callbackFlow {
        Log.d(TAG, "CallbackFlow starting...")
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
            Log.d(TAG, "AuthStateListener triggered: User = ${auth.currentUser?.uid}")
        }
        firebaseAuth.addAuthStateListener(listener)
        Log.d(TAG, "AuthStateListener added.")
        awaitClose {
            Log.d(TAG, "Removing AuthStateListener.")
            firebaseAuth.removeAuthStateListener(listener)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1) // Replay 1

    // --- Premium Durumu Akışı ---
    // Kullanıcı durumuna göre premium bilgisini (yükleniyor?, premiumMu?) getiren StateFlow
    private val premiumStatusStateFlow: StateFlow<Pair<Boolean, Boolean>> = userStateFlow
        .flatMapLatest { firebaseUser -> // Kullanıcı değişirse bu blok yeniden çalışır
            flow {
                emit(Pair(true, false)) // Başlangıçta: Yükleniyor=true, Premium=false
                val userId = firebaseUser?.uid
                if (userId == null) {
                    Log.d(TAG, "User is null, setting premium state to (false, false)")
                    emit(Pair(false, false)) // Kullanıcı yoksa: Yüklenmiyor, Premium=false
                } else {
                    Log.d(TAG, "User found ($userId), loading premium status...")
                    try {
                        var prefs = taskRepository.getUserPreferences(userId)
                        if (prefs == null && firebaseUser.isEmailVerified) { // Sadece doğrulanmışsa oluşturalım? (Opsiyonel)
                            Log.d(TAG, "Prefs not found for $userId, creating default.")
                            taskRepository.createUserPreferences(userId)
                            prefs = taskRepository.getUserPreferences(userId) // Tekrar okumayı dene
                            Log.d(TAG, "premiumStatusFlow: Prefs re-fetched after creation for $userId: $prefs")

                        }
                        val isPremium = prefs?.isPremium ?: false
                        Log.d(TAG, "Premium status loaded for $userId: $isPremium")
                        emit(Pair(false, isPremium)) // Yükleme bitti, sonucu emit et
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading premium status for $userId", e)
                        emit(Pair(false, false)) // Hata durumunda: Yüklenmiyor, Premium=false
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(true, false)) // StateFlow'a çevir


    // --- Görevler Akışı ---
    // Bugünün görevlerini Room'dan dinleyen ve paylaşan Flow
    private val tasksFlow: SharedFlow<List<Task>> = taskRepository.getTasksForDate(todayTimestamp)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1) // Replay 1

    // --- Varsayılan Görev Ekleme Kontrolü ---
    // Varsayılan görev ekleme işleminin yapılıp yapılmadığını takip eden bayrak
    private var defaultTasksAddCheckAttempted = false

    // <<< YENİ init Bloğu Mantığı >>>
    init {
        Log.d(TAG, "ViewModel init starting...")
        viewModelScope.launch {
            // 1. Adım: Giriş yapmış kullanıcıyı bekle (null olmayan ilk kullanıcı)
            userStateFlow.filterNotNull().firstOrNull()?.let { loggedInUser -> // Sadece bir kullanıcı varsa devam et
                Log.d(TAG, "init: Detected logged-in user: ${loggedInUser.uid}")

                // 2. Adım: Bu kullanıcı için premium durumunun yüklenmesini bekle
                Log.d(TAG, "init: Waiting for premium status to load for user ${loggedInUser.uid}...")
                val premiumStatePair = premiumStatusStateFlow.first { !it.first } // isLoading=false olan ilk durumu bekle
                val isPremium = premiumStatePair.second
                Log.d(TAG, "init: Premium status confirmed ($isPremium) for user ${loggedInUser.uid}")

                // 3. Adım: Görev listesinin ilk durumunu al (veritabanından)
                val initialTasks = tasksFlow.first()
                Log.d(TAG, "init: Task list size on premium check: ${initialTasks.size}")

                // 4. Adım: Bayrağı ve görev listesi boşluğunu kontrol et
                if (!defaultTasksAddCheckAttempted && initialTasks.isEmpty()) {
                    defaultTasksAddCheckAttempted = true // Kontrol yapıldı
                    Log.d(TAG, "init: Adding default tasks with definitive premium status: $isPremium")
                    addDefaultTasks(isPremium) // Görevleri ekle
                } else if (!defaultTasksAddCheckAttempted) {
                    defaultTasksAddCheckAttempted = true // Kontrol yapıldı
                    Log.d(TAG, "init: Tasks already exist or check previously done. Skipping add.")
                }
            } ?: run {
                // Eğer userStateFlow başlangıçta null ise veya null kalırsa burası çalışabilir
                // Bu durumda görev eklemeyeceğiz, bayrağı işaretleyebiliriz.
                if (!defaultTasksAddCheckAttempted) {
                    defaultTasksAddCheckAttempted = true
                    Log.d(TAG, "init: No logged-in user detected initially. Skipping default task add check for now.")
                }
            }
        }
        Log.d(TAG, "ViewModel init finished launching coroutine.")
    } // <<< init Bloğu Sonu >>>

    // Varsayılan görevleri ekleyen suspend fonksiyon (İçeriği aynı)
    private suspend fun addDefaultTasks(isPremium: Boolean) {
        Log.d(TAG, "addDefaultTasks function entered with isPremium = $isPremium")
        try {
            val defaultTasksDto = taskRepository.loadDefaultTasks()
            if (defaultTasksDto.isNotEmpty()) {
                val tasksToAdd = mutableListOf<DefaultTaskDto>()
                if (isPremium) {
                    Log.d(TAG, "addDefaultTasks: Selecting tasks for PREMIUM user.")
                    tasksToAdd.addAll(defaultTasksDto.filter { it.type == "body" }.take(2))
                    defaultTasksDto.firstOrNull { it.type == "mind" }?.let { tasksToAdd.add(it) }
                } else {
                    Log.d(TAG, "addDefaultTasks: Selecting tasks for FREE user.")
                    defaultTasksDto.firstOrNull { it.type == "body" || it.type == "mind" }?.let { tasksToAdd.add(it) }
                }
                Log.d(TAG,"Attempting to add ${tasksToAdd.size} default tasks (isPremium=$isPremium)")
                tasksToAdd.forEach { dto ->
                    val newTask = Task(
                        type = dto.type, title = dto.title, description = dto.description,
                        date = todayTimestamp, isCompleted = false
                    )
                    taskRepository.insertTask(newTask)
                    Log.d(TAG,"Finished attempting to insert ${tasksToAdd.size} tasks into Room.")

                }
            } else { Log.w(TAG,"Default tasks JSON is empty.") }
        } catch (e: Exception) { Log.e(TAG, "Error adding default tasks", e) }
    }


    // --- Son UI State'i Oluşturma ---
    // Kullanılacak ana StateFlow
    val uiState: StateFlow<HomeUiState> = combine(
        tasksFlow, // Güncel görev listesi Flow'u
        premiumStatusStateFlow // Güncel <isLoading, isPremium> durumu Flow'u
    ) { tasks, premiumState ->
        // Herhangi bir akıştan yeni değer geldiğinde HomeUiState'i yeniden hesapla
        val (isLoadingPremium, isPremiumStatus) = premiumState
        val completedCount = tasks.count { it.isCompleted }
        val totalCount = tasks.size
        val percentage = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

        HomeUiState(
            tasks = tasks,
            dailyProgressPercentage = percentage,
            completedTasks = completedCount,
            totalTasks = totalCount,
            motivationalQuote = dailyQuote,
            // Görevler tasksFlow'dan geldiği için isLoadingTasks'ı artık doğrudan yönetmiyoruz,
            // başlangıç değeri ile idare ediyoruz. Veya tasksFlow'a başlangıç null değeri ekleyebiliriz.
            isLoadingTasks = false, // Simplification for now
            isLoadingPremium = isLoadingPremium,
            isPremium = isPremiumStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState(isLoadingTasks = true, isLoadingPremium = true, isPremium = false, motivationalQuote = dailyQuote)
    )

    // --- Diğer Fonksiyonlar ---
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch { taskRepository.updateCompletionStatus(task.id, !task.isCompleted) }
    }

    suspend fun clearAllLocalTasks() {
        try {
            Log.d(TAG, "Clearing all local tasks from Room...")
            taskRepository.deleteAllTasks() // Repository'deki silme fonksiyonunu çağır
            Log.d(TAG, "Local tasks cleared successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local tasks", e)
        }
    }

    private fun selectDailyQuote(timestamp: Long): String {
        val calendar = Calendar.getInstance(); calendar.timeInMillis = timestamp
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val quoteIndex = dayOfYear % quotes.size
        return quotes[quoteIndex]
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance(); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0); return calendar.timeInMillis
    }
}