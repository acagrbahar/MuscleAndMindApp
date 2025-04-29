package com.acagribahar.muscleandmindapp.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DailyProgressUiState(
    val progressPercentage: Float = 0f, // 0.0 ile 1.0 arası
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

class ProgressViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    private val todayTimestamp: Long = getStartOfDayTimestamp()

    // Bugünün görevlerini alıp günlük ilerleme durumunu hesaplayan StateFlow
    val dailyProgressState: StateFlow<DailyProgressUiState> =
        taskRepository.getTasksForDate(todayTimestamp) // Repository'den bugünün görevlerini Flow olarak al
            .map { todaysTasks -> // Flow'daki her liste için dönüşüm yap
                calculateDailyProgress(todaysTasks)
            }
            // Flow'u StateFlow'a çevir. scope, started, initialValue gerekir.
            .stateIn(
                scope = viewModelScope, // ViewModel'ın Coroutine scope'u
                // Ekran görünür olduğunda Flow'u aktif et, 5sn sonra durdur
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = DailyProgressUiState() // Başlangıç değeri
            )

    // Mevcut seriyi tutacak StateFlow
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    // --- YENİ KISIM: Haftalık İstatistikler ---
    private val _weeklyStats = MutableStateFlow<List<WeeklyStat>>(emptyList())
    val weeklyStats: StateFlow<List<WeeklyStat>> = _weeklyStats.asStateFlow()
    // Alternatif olarak stateIn ile:
    /*
    val weeklyStats: StateFlow<List<WeeklyStat>> = flow {
         emit(loadAndProcessWeeklyStats()) // Başlangıçta bir kere yükle
         // Veya doğrudan repository flow'unu map/transform et
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    */
    // Şimdilik init içinde tetikleyelim:

    init {
        // ViewModel başlatıldığında seriyi hesapla
        calculateCurrentStreak()
        loadWeeklyStats() // Haftalık istatistikleri yükle

    }

    // Haftalık istatistikleri yükleyip işleyen fonksiyon
    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val endDateTs = todayTimestamp // Bugünün başlangıcı
            val startDateTs = getStartOfDayTimestamp(daysAgo = 6) // 6 gün öncesinin başlangıcı (toplam 7 gün)
            val dateFormat = SimpleDateFormat("EEE", Locale("tr", "TR")) // Gün etiketi için (Pzt, Sal...)

            taskRepository.getTasksBetweenDates(startDateTs, endDateTs)
                .map { tasksInDateRange ->
                    // Son 7 gün için istatistikleri oluştur
                    val statsList = mutableListOf<WeeklyStat>()
                    for (i in 6 downTo 0) { // 6 gün önceden bugüne kadar
                        val dayTimestamp = getStartOfDayTimestamp(daysAgo = i)
                        // O güne ait görevleri filtreden geçir
                        val tasksForDay = tasksInDateRange.filter { it.date == dayTimestamp }
                        val completed = tasksForDay.count { it.isCompleted }
                        val total = tasksForDay.size
                        statsList.add(
                            WeeklyStat(
                                dayLabel = dateFormat.format(Date(dayTimestamp)).replaceFirstChar { it.titlecase(Locale.getDefault()) }, // Örn: "Pzt"
                                completedCount = completed,
                                totalCount = total,
                                timestamp = dayTimestamp
                            )
                        )
                    }
                    statsList // Oluşturulan listeyi döndür
                }
                .collect { processedStats -> // İşlenmiş istatistikleri al
                    _weeklyStats.value = processedStats // StateFlow'u güncelle
                }
        }
    }
    // --- HAFTALIK İSTATİSTİK SONU ---

    // Seriyi hesaplayan fonksiyon
    private fun calculateCurrentStreak() {
        viewModelScope.launch {
            var streak = 0
            var currentCheckDateTs = getStartOfDayTimestamp(daysAgo = 1) // Dünden başla

            while (true) { // Geriye doğru günleri kontrol et
                val tasksForDay = taskRepository.getTasksForDateSync(currentCheckDateTs)
                // O gün görev var mıydı VE en az biri tamamlandı mı?
                if (tasksForDay.isNotEmpty() && tasksForDay.any { it.isCompleted }) {
                    streak++ // Seriyi artır
                    // Bir önceki güne geç
                    currentCheckDateTs -= TimeUnit.DAYS.toMillis(1) // 1 günün milisaniyesini çıkar
                } else {
                    // Tamamlanmış görev yoksa veya o gün hiç görev atanmamışsa seri bozulur
                    break // Döngüden çık
                }
            }
            _currentStreak.value = streak // Hesaplanan seriyi StateFlow'a ata
            println("Hesaplanan Seri: $streak") // Log
        }
    }


    // Verilen görev listesine göre günlük ilerlemeyi hesaplayan fonksiyon
    private fun calculateDailyProgress(tasks: List<Task>): DailyProgressUiState {
        if (tasks.isEmpty()) {
            return DailyProgressUiState() // Görev yoksa ilerleme 0
        }
        val completedCount = tasks.count { it.isCompleted }
        val totalCount = tasks.size
        val percentage = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
        return DailyProgressUiState(
            progressPercentage = percentage,
            completedCount = completedCount,
            totalCount = totalCount
        )
    }

    // Belirtilen gün kadar önceki günün başlangıç timestamp'ini verir
    private fun getStartOfDayTimestamp(daysAgo: Int = 0): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo) // Günü geri al
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Haftalık veri ve streak hesaplama sonraki adımlarda buraya eklenecek
}