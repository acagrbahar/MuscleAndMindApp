package com.acagribahar.muscleandmindapp.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth

class AddExerciseViewModelFactory(
    private val repository: TaskRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddExerciseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddExerciseViewModel(repository, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}