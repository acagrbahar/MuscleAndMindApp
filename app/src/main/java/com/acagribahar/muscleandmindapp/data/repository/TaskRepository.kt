package com.acagribahar.muscleandmindapp.data.repository

import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    // Room DAO fonksiyonları için arayüz
    fun getTasksForDate(date: Long): Flow<List<Task>>
    suspend fun insertTask(task: Task)
    suspend fun updateCompletionStatus(id: Int, completed: Boolean)
    // ... (Diğer DAO fonksiyonları eklenebilir) ...

    // JSON'dan varsayılan görevleri yüklemek için fonksiyon
    suspend fun loadDefaultTasks(): List<DefaultTaskDto>
}