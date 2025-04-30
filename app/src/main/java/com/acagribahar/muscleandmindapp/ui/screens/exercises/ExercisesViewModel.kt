package com.acagribahar.muscleandmindapp.ui.screens.exercises

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.data.remote.model.UserExercise
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

// Grup yapısını temsil etmek için
data class ExerciseGroup(
    val category: String,
    val exercises: List<DisplayExercise>
)

class ExercisesViewModel(private val taskRepository: TaskRepository,
                         private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // Gruplanmış egzersiz listesini tutacak StateFlow
    private val _groupedExercises = MutableStateFlow<List<ExerciseGroup>>(emptyList())
    val groupedExercises: StateFlow<List<ExerciseGroup>> = _groupedExercises.asStateFlow()

    // Auth durumunu dinleyen Flow (callbackFlow ile)
    private val userStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    init {
        // Kullanıcı durumu değiştikçe egzersizleri yükle/birleştir
        viewModelScope.launch {
            userStateFlow.flatMapLatest { firebaseUser -> // Kullanıcı değişirse veya ilk geldiğinde çalışır
                val userId = firebaseUser?.uid
                if (userId == null) {
                    // Kullanıcı yoksa sadece varsayılanları yükle (veya boş liste göster)
                    Log.d("ExercisesVM", "User is null, loading only default exercises.")
                    loadAndCombineExercises(null) // Custom exercises için null geç
                } else {
                    // Kullanıcı varsa hem varsayılanları hem özelleri yükle
                    Log.d("ExercisesVM", "User found ($userId), loading default & custom exercises.")
                    loadAndCombineExercises(userId)
                }
            }.collect { combinedGroupedList -> // Birleştirilmiş ve gruplanmış listeyi al
                _groupedExercises.value = combinedGroupedList // StateFlow'u güncelle
                Log.d("ExercisesVM", "Updated grouped exercises state.")
            }
        }
    }

    // Hem varsayılan hem de özel egzersizleri alıp birleştiren Flow döndüren fonksiyon
    private fun loadAndCombineExercises(userId: String?): Flow<List<ExerciseGroup>> {
        // Varsayılan egzersizleri bir kere yükle (Flow değil, direkt liste)
        val defaultExercisesFlow: Flow<List<DefaultTaskDto>> = flow {
            emit(taskRepository.loadDefaultTasks())
        }

        // Özel egzersizleri dinleyen Flow (kullanıcı varsa)
        val customExercisesFlow: Flow<List<UserExercise>> = userId?.let {
            taskRepository.getCustomExercises(it)
        } ?: flowOf(emptyList()) // Kullanıcı yoksa boş liste Flow'u

        // İki Flow'u birleştir
        return combine(defaultExercisesFlow, customExercisesFlow) { defaultTasks, customTasks ->
            Log.d("ExercisesVM", "Combining: ${defaultTasks.size} default, ${customTasks.size} custom")
            // Varsayılanları DisplayExercise'e map et
            val defaultDisplay = defaultTasks
                .filter { it.type == "body" } // Sadece body olanları al
                .map { dto ->
                    DisplayExercise(
                        id = "default_${dto.title}", // Basit bir ID oluşturalım
                        title = dto.title,
                        description = dto.description,
                        category = dto.category,
                        isCustom = false
                    )
                }
            // Özelleri DisplayExercise'e map et
            val customDisplay = customTasks.map { userEx ->
                DisplayExercise(
                    id = userEx.id, // Firestore ID'si
                    title = userEx.title,
                    description = userEx.description,
                    category = userEx.category,
                    isCustom = true
                )
            }

            // İki listeyi birleştir, kategoriye göre grupla, ExerciseGroup listesi oluştur
            (defaultDisplay + customDisplay)
                .groupBy { it.category ?: "Diğer" } // Kategorisi olmayanları "Diğer" grubuna ata
                .map { (category, exercises) ->
                    ExerciseGroup(
                        category = category,
                        // Egzersizleri başlığa göre sırala (isteğe bağlı)
                        exercises = exercises.sortedBy { it.title }
                    )
                }
                // Grupları kategori adına göre sırala (isteğe bağlı)
                .sortedBy { it.category }
        }
    }

    /*
    private fun loadAndGroupExercises() {
        viewModelScope.launch {
            val allDefaultTasks = taskRepository.loadDefaultTasks()
            val bodyExercises = allDefaultTasks.filter { it.type == "body" && !it.category.isNullOrBlank() }

            // Kategoriye göre grupla ve ExerciseGroup listesi oluştur
            val grouped = bodyExercises
                .groupBy { it.category!! } // Null olmayan kategoriye göre grupla
                .map { (category, exercises) -> // Her grup için ExerciseGroup oluştur
                    ExerciseGroup(category = category, exercises = exercises)
                }
                .sortedBy { it.category } // Kategoriye göre sırala (isteğe bağlı)

            _groupedExercises.value = grouped // StateFlow'u güncelle
        }
    }

     */

    // Başlığa göre egzersizi bulan fonksiyon
    /*
    fun getExerciseByTitle(title: String): DefaultTaskDto? {
        return groupedExercises.value // StateFlow'un güncel değerini al
            .flatMap { it.exercises } // Tüm gruplardaki egzersizleri tek bir listeye düzleştir
            .find { it.title == title } // Başlığa göre eşleşeni bul
    }

     */
    // ID'ye göre DisplayExercise bulan fonksiyon
    fun getDisplayExerciseById(id: String): DisplayExercise? {
        // _groupedExercises yerine groupedExercises (StateFlow) kullanmak daha doğru
        // .value ile güncel listeyi al
        return groupedExercises.value
            .flatMap { exerciseGroup -> exerciseGroup.exercises } // Tüm egzersizleri tek listeye çevir
            .find { displayExercise -> displayExercise.id == id } // ID'ye göre bul
    }
}