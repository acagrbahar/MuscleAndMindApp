package com.acagribahar.muscleandmindapp.ui.screens.settings


import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.acagribahar.muscleandmindapp.data.local.SettingsManager
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth

// Artık FirebaseAuth parametresi almıyor
class SettingsViewModelFactory(
    private val repository: TaskRepository,
    private val firebaseAuth: FirebaseAuth,
    private val application: Application

) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")

            return SettingsViewModel(repository, firebaseAuth,SettingsManager(application)) as T // <<< FirebaseAuth'ı SİLİN
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}