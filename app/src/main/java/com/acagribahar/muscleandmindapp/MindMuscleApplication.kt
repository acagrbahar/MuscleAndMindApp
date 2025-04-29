package com.acagribahar.muscleandmindapp

import android.app.Application
import androidx.work.* // WorkManager importları
import com.acagribahar.muscleandmindapp.worker.ReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MindMuscleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Uygulama ilk açıldığında periyodik bildirim işini planla
        scheduleDailyReminder()
    }

    private fun scheduleDailyReminder() {
        val workManager = WorkManager.getInstance(this)

        // Benzersiz iş adı
        val uniqueWorkName = "DailyReminderWork"

        // Hedef saat (örneğin sabah 9)
        val targetHour = 9
        val targetMinute = 0

        // Şu anki zaman ve hedef zaman arasındaki farkı hesapla
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            // Eğer hedef saat zaten geçtiyse, yarının aynı saatini hedefle
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // İlk çalıştırma için gecikme süresi (milisaniye cinsinden)
        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        // Periyodik iş isteğini oluştur (24 saatte bir tekrarla)
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            repeatInterval = 24, // Tekrar aralığı
            repeatIntervalTimeUnit = TimeUnit.HOURS // Aralık birimi
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS) // İlk gecikme
            // .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()) // Opsiyonel kısıtlamalar
            .build()

        // İşi benzersiz olarak sıraya ekle (aynı işin tekrar eklenmesini önler)
        // KEEP: Eğer aynı isimde bir iş zaten varsa, yenisini ekleme, eskisini koru.
        // REPLACE: Eğer aynı isimde bir iş varsa, eskisini iptal et, yenisini ekle.
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.KEEP, // veya REPLACE
            reminderRequest
        )

        println("Günlük hatırlatıcı işi planlandı. İlk çalışma ${initialDelay / 1000 / 60} dakika sonra.") // Loglama
    }
}