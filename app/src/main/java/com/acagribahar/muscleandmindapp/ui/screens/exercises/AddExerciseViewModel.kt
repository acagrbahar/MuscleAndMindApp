package com.acagribahar.muscleandmindapp.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.remote.model.UserExercise
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Kaydetme durumunu UI'a bildirmek için
sealed class SaveState {
    object Idle : SaveState() // Başlangıç / Boşta
    object Loading : SaveState() // Kaydediliyor
    object Success : SaveState() // Başarılı
    data class Error(val message: String) : SaveState() // Hatalı
}

class AddExerciseViewModel(
    private val taskRepository: TaskRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun saveExercise(title: String, description: String, category: String?) {
        if (title.isBlank()) {
            _saveState.value = SaveState.Error("Başlık boş olamaz!")
            return
        }

        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _saveState.value = SaveState.Error("Kullanıcı bulunamadı!")
            return
        }

        _saveState.value = SaveState.Loading // Yükleniyor durumuna geç

        val newExercise = UserExercise(
            // id Firestore tarafından atanacak
            userId = userId,
            title = title.trim(),
            description = description.trim(),
            category = category?.trim()?.takeIf { it.isNotBlank() } // Boş değilse al
        )

        viewModelScope.launch {
            val result = taskRepository.addCustomExercise(userId, newExercise)
            result.onSuccess {
                _saveState.value = SaveState.Success // Başarılı durumuna geç
            }.onFailure { exception ->
                _saveState.value = SaveState.Error(exception.localizedMessage ?: "Kaydetme başarısız oldu.")
            }
        }
    }

    // UI state'i Idle durumuna geri döndürmek için (örn: hata mesajı gösterildikten sonra)
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}
