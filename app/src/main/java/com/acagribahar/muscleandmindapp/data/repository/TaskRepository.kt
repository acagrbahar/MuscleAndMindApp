package com.acagribahar.muscleandmindapp.data.repository

import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.data.remote.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    // Room DAO fonksiyonları için arayüz
    fun getTasksForDate(date: Long): Flow<List<Task>>
    suspend fun insertTask(task: Task)
    suspend fun updateCompletionStatus(id: Int, completed: Boolean)
    // ... (Diğer DAO fonksiyonları eklenebilir) ...

    // JSON'dan varsayılan görevleri yüklemek için fonksiyon
    suspend fun loadDefaultTasks(): List<DefaultTaskDto>
    suspend fun getTasksForDateSync(date: Long): List<Task>
    fun getTasksBetweenDates(startDate: Long, endDate: Long): Flow<List<Task>>

    suspend fun getUserPreferences(userId: String): UserPreferences?
    suspend fun updateUserPremiumStatus(userId: String, isPremium: Boolean)
    // Kullanıcı ilk kez girdiğinde veya veri yoksa varsayılan tercihleri oluşturmak için:
    suspend fun createUserPreferences(userId: String)
}