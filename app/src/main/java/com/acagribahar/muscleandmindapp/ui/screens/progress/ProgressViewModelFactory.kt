package com.acagribahar.muscleandmindapp.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth

class ProgressViewModelFactory(
    private val repository: TaskRepository,
    private val firebaseAuth: FirebaseAuth

) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(repository,firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}