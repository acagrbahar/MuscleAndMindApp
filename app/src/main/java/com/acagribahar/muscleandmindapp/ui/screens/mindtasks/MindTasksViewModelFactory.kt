package com.acagribahar.muscleandmindapp.ui.screens.mindtasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository

class MindTasksViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MindTasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MindTasksViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}