package com.acagribahar.muscleandmindapp.data.local

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("MindMusclePrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIF_HOUR = "notification_hour"
        private const val KEY_NOTIF_MINUTE = "notification_minute"
        // Varsayılan saat (örn: 09:00)
        const val DEFAULT_HOUR = 9
        const val DEFAULT_MINUTE = 0
    }

    // Seçilen saati kaydeder
    fun saveNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_NOTIF_HOUR, hour)
            .putInt(KEY_NOTIF_MINUTE, minute)
            .apply()
    }

    // Kaydedilmiş saati alır (yoksa varsayılanı döndürür)
    fun getNotificationTime(): Pair<Int, Int> {
        val hour = prefs.getInt(KEY_NOTIF_HOUR, DEFAULT_HOUR)
        val minute = prefs.getInt(KEY_NOTIF_MINUTE, DEFAULT_MINUTE)
        return Pair(hour, minute)
    }
}