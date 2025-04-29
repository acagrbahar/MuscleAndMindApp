package com.acagribahar.muscleandmindapp.ui.screens


import androidx.lifecycle.ViewModel // Normal ViewModel kullanacağız
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto // DTO'yu import et
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository // Repository'yi import et
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

// Artık Application yerine TaskRepository alıyor
class HomeViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    val tasks: Flow<List<Task>>
    private val todayTimestamp: Long = getStartOfDayTimestamp()

    init {
        // Görevleri Repository üzerinden al
        tasks = taskRepository.getTasksForDate(todayTimestamp)
        checkAndAddDefaultTasks()
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            // Güncellemeyi Repository üzerinden yap
            taskRepository.updateCompletionStatus(task.id, !task.isCompleted)
        }
    }

    private fun getStartOfDayTimestamp(): Long {
        // ... (Bu fonksiyon aynı kalır) ...
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Varsayılan görev ekleme mantığını güncelle (Repository ve JSON kullanarak)
    private fun checkAndAddDefaultTasks() {
        viewModelScope.launch {
            val existingTasks = tasks.firstOrNull()
            if (existingTasks.isNullOrEmpty()) {
                println("Bugün için görev yok, JSON'dan varsayılanlar yükleniyor...")
                // Varsayılan görevleri Repository üzerinden JSON'dan yükle
                val defaultTasksDto = taskRepository.loadDefaultTasks()

                if (defaultTasksDto.isNotEmpty()) {
                    // Örnek: İlk "body" ve ilk "mind" görevini seçelim
                    val taskToAdd = mutableListOf<DefaultTaskDto>()
                    defaultTasksDto.firstOrNull { it.type == "body" }?.let { taskToAdd.add(it) }
                    defaultTasksDto.firstOrNull { it.type == "mind" }?.let { taskToAdd.add(it) }

                    // Seçilen görevleri Task Entity'sine çevirip DB'ye ekle
                    taskToAdd.forEach { dto ->
                        val newTask = Task(
                            type = dto.type,
                            title = dto.title,
                            description = dto.description,
                            date = todayTimestamp, // Bugünün tarihi
                            isCompleted = false
                        )
                        taskRepository.insertTask(newTask) // Repository üzerinden ekle
                    }
                    println("${taskToAdd.size} adet varsayılan görev eklendi.")
                } else {
                    println("JSON'dan varsayılan görev yüklenemedi veya dosya boş.")
                }
            } else {
                println("Bugün için görevler zaten var.")
            }
        }
    }
}