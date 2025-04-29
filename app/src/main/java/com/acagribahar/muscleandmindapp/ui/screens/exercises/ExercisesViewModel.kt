package com.acagribahar.muscleandmindapp.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Grup yapısını temsil etmek için
data class ExerciseGroup(
    val category: String,
    val exercises: List<DefaultTaskDto>
)

class ExercisesViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    // Gruplanmış egzersiz listesini tutacak StateFlow
    private val _groupedExercises = MutableStateFlow<List<ExerciseGroup>>(emptyList())
    val groupedExercises: StateFlow<List<ExerciseGroup>> = _groupedExercises.asStateFlow()

    init {
        loadAndGroupExercises()
    }

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

    // Başlığa göre egzersizi bulan fonksiyon
    fun getExerciseByTitle(title: String): DefaultTaskDto? {
        return groupedExercises.value // StateFlow'un güncel değerini al
            .flatMap { it.exercises } // Tüm gruplardaki egzersizleri tek bir listeye düzleştir
            .find { it.title == title } // Başlığa göre eşleşeni bul
    }
}