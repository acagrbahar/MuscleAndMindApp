package com.acagribahar.muscleandmindapp.ui.screens.mindtasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MindTasksViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    // Zihin görevleri listesini tutacak StateFlow
    private val _mindTasks = MutableStateFlow<List<DefaultTaskDto>>(emptyList())
    val mindTasks: StateFlow<List<DefaultTaskDto>> = _mindTasks.asStateFlow()

    init {
        loadMindTasks()
    }

    private fun loadMindTasks() {
        viewModelScope.launch {
            val allDefaultTasks = taskRepository.loadDefaultTasks()
            // Sadece 'mind' tipindeki görevleri filtrele
            _mindTasks.value = allDefaultTasks.filter { it.type == "mind" }
        }
    }

    // Başlığa göre zihin görevini bulan fonksiyon (Detay ekranı için)
    fun getMindTaskByTitle(title: String): DefaultTaskDto? {
        return mindTasks.value.find { it.title == title }
    }
}