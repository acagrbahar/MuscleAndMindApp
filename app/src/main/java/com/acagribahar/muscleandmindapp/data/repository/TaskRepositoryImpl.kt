package com.acagribahar.muscleandmindapp.data.repository

import android.content.Context // Context'e ihtiyaç var (AssetManager için)
import com.acagribahar.muscleandmindapp.data.local.dao.TaskDao
import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json // Kotlinx Serialization Json
import java.io.IOException

// DAO ve Context'i parametre olarak alır (İleride DI ile sağlanacak)
class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val context: Context // AssetManager'a erişim için
) : TaskRepository {

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