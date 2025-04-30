package com.acagribahar.muscleandmindapp.ui.screens


import androidx.lifecycle.ViewModel // Normal ViewModel kullanacağız
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto // DTO'yu import et
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository // Repository'yi import et
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*

// Artık Application yerine TaskRepository alıyor
class HomeViewModel(private val taskRepository: TaskRepository,
                    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // <<< Premium durumunu tutacak StateFlow ekle >>>
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    val tasks: Flow<List<Task>>
    private val todayTimestamp: Long = getStartOfDayTimestamp()

    // <<< Auth durumunu dinlemek için callbackFlow (SettingsViewModel'dan kopyalanabilir) >>>
    private val userStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    init {
        // Görevleri Repository üzerinden al (Bu satır aynı kalır)
        tasks = taskRepository.getTasksForDate(todayTimestamp)

        // <<< Auth durumunu dinleyerek premium durumunu ve görevleri yükle >>>
        viewModelScope.launch {
            userStateFlow.collect { firebaseUser -> // Auth durumu değiştikçe çalışır
                if (firebaseUser != null) {
                    // Kullanıcı varsa premium durumunu yükle
                    loadUserPremiumStatus(firebaseUser.uid)
                    // Premium durumu yüklendikten SONRA varsayılan görevleri kontrol et
                    // (loadUserPremiumStatus içinde çağırılabilir veya burada delay/check eklenebilir)
                    // Şimdilik direkt çağıralım, isPremium state'i güncellenince checkAndAdd... doğru çalışır
                    checkAndAddDefaultTasks()
                } else {
                    // Kullanıcı yoksa premium değil varsay
                    _isPremium.value = false
                    // İsteğe bağlı: Kullanıcı çıkış yapınca o günkü görevleri temizle?
                }
            }
        }
    }

    // <<< Kullanıcının premium durumunu yükleyen fonksiyon >>>
    private fun loadUserPremiumStatus(userId: String) {
        viewModelScope.launch {
            try {
                val prefs = taskRepository.getUserPreferences(userId)
                // Kullanıcı tercihlerini bulamazsa veya oluşturamazsa bile premium değil varsayalım
                _isPremium.value = prefs?.isPremium ?: false
                Log.d("HomeViewModel", "Premium status loaded for $userId: ${isPremium.value}")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading premium status: ${e.message}", e)
                _isPremium.value = false // Hata durumunda premium değil varsay
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            // Güncellemeyi Repository üzerinden yap
            taskRepository.updateCompletionStatus(task.id, !task.isCompleted)
        }
    }

    private fun getStartOfDayTimestamp(): Long {
        // ... (Bu fonksiyon aynı kalır) ...
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Varsayılan görev ekleme mantığını premium durumuna göre güncelle
    private fun checkAndAddDefaultTasks() {
        // Bu fonksiyon artık isPremium state'i yüklendikten sonra çağrıldığı için
        // isPremium.value güncel durumu yansıtmalı.
        viewModelScope.launch {
            val existingTasks = tasks.firstOrNull() // Flow'dan ilk değeri almayı dene
            if (existingTasks.isNullOrEmpty()) {
                Log.d("HomeViewModel","No tasks for today. Loading defaults based on premium: ${isPremium.value}")
                val defaultTasksDto = taskRepository.loadDefaultTasks()

                if (defaultTasksDto.isNotEmpty()) {
                    val tasksToAdd = mutableListOf<DefaultTaskDto>()

                    // <<< Premium durumuna göre eklenecek görevleri seç >>>
                    if (isPremium.value) {
                        // Premium: Örnek olarak 2 body, 1 mind
                        tasksToAdd.addAll(defaultTasksDto.filter { it.type == "body" }.take(2))
                        defaultTasksDto.firstOrNull { it.type == "mind" }?.let { tasksToAdd.add(it) }
                        Log.d("HomeViewModel","Adding ${tasksToAdd.size} tasks for premium user.")
                    } else {
                        // Ücretsiz: Sadece 1 görev (ilk bulduğun body veya mind)
                        defaultTasksDto.firstOrNull { it.type == "body" || it.type == "mind" }?.let { tasksToAdd.add(it) }
                        Log.d("HomeViewModel","Adding 1 task for free user.")
                    }

                    // Seçilen görevleri Task Entity'sine çevirip DB'ye ekle
                    tasksToAdd.forEach { dto ->
                        val newTask = Task(
                            type = dto.type,
                            title = dto.title,
                            description = dto.description,
                            date = todayTimestamp,
                            isCompleted = false
                        )
                        taskRepository.insertTask(newTask)
                    }
                } else {
                    Log.w("HomeViewModel","Could not load default tasks from JSON.")
                }
            } else {
                Log.d("HomeViewModel","Tasks already exist for today.")
            }
        }
    }
}