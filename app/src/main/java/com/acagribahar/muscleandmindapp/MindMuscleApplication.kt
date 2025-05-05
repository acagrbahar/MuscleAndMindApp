package com.acagribahar.muscleandmindapp

import android.app.Application
import android.util.Log
import androidx.work.* // WorkManager importları
import com.acagribahar.muscleandmindapp.data.local.SettingsManager
import com.acagribahar.muscleandmindapp.worker.ReminderWorker
import com.google.android.gms.ads.MobileAds
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MindMuscleApplication : Application() {

    // SettingsManager örneğini burada oluşturabiliriz
    private lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)

        // <<< Mobile Ads SDK'sını başlat >>>
        MobileAds.initialize(this) {}
        Log.d("MindMuscleApp", "Mobile Ads SDK Initialized.")

        // Uygulama ilk açıldığında periyodik bildirim işini planla
        scheduleDailyReminder()
    }

    // <<< Fonksiyonu public yapalım ki dışarıdan çağrılabilsin >>>
    fun scheduleDailyReminder() {
        val workManager = WorkManager.getInstance(this)
        val uniqueWorkName = "DailyReminderWork"

        val notificationsEnabled = settingsManager.getNotificationsEnabled()

        if (notificationsEnabled){
            Log.d("MindMuscleApp", "Notifications are ENABLED. Scheduling reminder...")

            // <<< Kullanıcının seçtiği saati SharedPreferences'dan oku >>>
            val (targetHour, targetMinute) = settingsManager.getNotificationTime()

            val currentTime = Calendar.getInstance()
            val targetTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                if (before(currentTime)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

            val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            // <<< Politikayı REPLACE yapalım ki yeniden planlama yapılabilsin >>>
            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.REPLACE, // <<< KEEP yerine REPLACE
                reminderRequest
            )

            Log.d("MindMuscleApp", "Daily reminder scheduled. Target: $targetHour:$targetMinute. Initial delay: ${initialDelay / 1000 / 60} min.") // <<< Loglama güncellendi
            // Önceki Log.d importunu eklemeyi unutmayın: import android.util.Log

        }else{
            // --- Bildirimler KAPALI: Mevcut işi iptal et ---
            Log.d("MindMuscleApp", "Notifications are DISABLED. Cancelling existing reminder work...")
            workManager.cancelUniqueWork(uniqueWorkName) // <<< Benzersiz isimle işi iptal et
        }


    }
}