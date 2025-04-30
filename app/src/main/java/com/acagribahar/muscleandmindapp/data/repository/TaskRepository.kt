package com.acagribahar.muscleandmindapp.data.repository

import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.data.remote.model.UserExercise
import com.acagribahar.muscleandmindapp.data.remote.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.firestore.CollectionReference

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

    // --- YENİ Firestore UserExercise Fonksiyonları ---
    suspend fun addCustomExercise(userId: String, exercise: UserExercise): Result<Unit> // Başarı/Hata durumu için Result kullanalım
    fun getCustomExercises(userId: String): Flow<List<UserExercise>> // Kullanıcının özel egzersizlerini Flow olarak alalım
    // suspend fun deleteCustomExercise(userId: String, exerciseId: String): Result<Unit> // Silme (Opsiyonel - Sonra eklenebilir)
    // suspend fun updateCustomExercise(userId: String, exercise: UserExercise): Result<Unit> // Güncelleme (Opsiyonel - Sonra eklenebilir)

}