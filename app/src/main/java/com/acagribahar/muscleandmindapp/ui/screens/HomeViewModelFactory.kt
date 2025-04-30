package com.acagribahar.muscleandmindapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth

// TaskRepository'yi parametre olarak alan Factory
class HomeViewModelFactory(
    private val repository: TaskRepository,
    private val firebaseAuth: FirebaseAuth

) : ViewModelProvider.Factory {

    // ViewModel oluşturma işini yapar
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Eğer istenen ViewModel HomeViewModel ise...
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            // ...Repository ile birlikte bir HomeViewModel örneği oluştur ve döndür
            @Suppress("UNCHECKED_CAST") // Cast işleminin güvenli olduğunu belirtiyoruz
            return HomeViewModel(repository,firebaseAuth) as T
        }
        // İstenen ViewModel bu değilse hata fırlat
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}