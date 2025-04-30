package com.acagribahar.muscleandmindapp.data.repository

import android.content.Context // Context'e ihtiyaç var (AssetManager için)
import android.util.Log
import com.acagribahar.muscleandmindapp.data.local.dao.TaskDao
import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.data.remote.model.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json // Kotlinx Serialization Json
import java.io.IOException
import com.google.firebase.firestore.Source

// DAO ve Context'i parametre olarak alır (İleride DI ile sağlanacak)
class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val context: Context // AssetManager'a erişim için
) : TaskRepository {

    // Firestore instance'ını al
    private val db: FirebaseFirestore = Firebase.firestore

    // Kullanıcı tercihlerini tutan collection referansı
    private val usersCollection = db.collection("users")

    override suspend fun getUserPreferences(userId: String): UserPreferences? {
        Log.d("Repository", "Attempting to get preferences for user: $userId")
        return try {
            // <<< .get() yerine .get(Source.SERVER) kullan >>>
            val document = usersCollection.document(userId).get().await()
            Log.d("Repository", "Firestore document exists for $userId: ${document.exists()} (Fetched from SERVER)") // Log'a ekleme
            val prefs = document.toObject(UserPreferences::class.java)
            Log.d("Repository", "Parsed preferences for $userId: $prefs")
            prefs
        } catch (e: Exception) {
            Log.e("Repository", "Error getting user preferences for $userId: ${e.message}")
            null
        }
    }

    override suspend fun updateUserPremiumStatus(userId: String, isPremium: Boolean) {
        try {
            // Belirtilen userId'ye sahip dokümanın 'isPremium' alanını güncelle
            usersCollection.document(userId).update("isPremium", isPremium).await()
            println("User $userId premium status updated to $isPremium") // Loglama
        } catch (e: Exception) {
            println("Error updating premium status: ${e.message}") // Hata loglama
        }
    }

    override suspend fun createUserPreferences(userId: String) {
        // Kullanıcı için zaten bir doküman var mı diye kontrol et
        val existingPrefs = getUserPreferences(userId)
        if (existingPrefs == null) {
            // Yoksa, varsayılan değerlerle yeni bir UserPreferences objesi oluştur
            val defaultPrefs = UserPreferences(userId = userId, isPremium = false, notificationHour = 9)
            try {
                // Yeni dokümanı Firestore'a ekle (doküman ID'si userId olacak)
                usersCollection.document(userId).set(defaultPrefs).await()
                Log.d("Repository", "Default preferences CREATED for user $userId") // <<< EKLE
            } catch (e: Exception) {
                Log.e("Repository", "Error CREATING user preferences for $userId: ${e.message}") // <<< EKLE
            }
        } else {
            Log.d("Repository", "Preferences already exist for user $userId, skipping creation.") // <<< EKLE
        }
    }


    override fun getTasksForDate(date: Long): Flow<List<Task>> {
        return taskDao.getTasksForDate(date)
    }

    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    override suspend fun updateCompletionStatus(id: Int, completed: Boolean) {
        taskDao.updateCompletionStatus(id, completed)
    }

    // JSON dosyasını okuyup parse eden fonksiyon
    override suspend fun loadDefaultTasks(): List<DefaultTaskDto> {
        // Dosya okuma IO işlemi olduğu için Dispatchers.IO kullanıyoruz
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("default_tasks.json")
                    .bufferedReader()
                    .use { it.readText() } // Dosyayı oku ve kapat

                // JSON string'ini List<DefaultTaskDto>'ya çevir
                Json.decodeFromString<List<DefaultTaskDto>>(jsonString)
            } catch (e: IOException) {
                // Hata durumunda boş liste veya hata yönetimi
                e.printStackTrace() // Hatayı logla
                emptyList()
            } catch (e: kotlinx.serialization.SerializationException) {
                // JSON parse hatası
                e.printStackTrace() // Hatayı logla
                emptyList()
            }
        }
    }

    override suspend fun getTasksForDateSync(date: Long): List<Task> {
        return taskDao.getTasksForDateSync(date)
    }

    override fun getTasksBetweenDates(startDate: Long, endDate: Long): Flow<List<Task>> {
        return taskDao.getTasksBetweenDates(startDate, endDate)
    }
}