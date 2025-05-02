package com.acagribahar.muscleandmindapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.acagribahar.muscleandmindapp.data.model.ThemePreference

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("MindMusclePrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIF_HOUR = "notification_hour"
        private const val KEY_NOTIF_MINUTE = "notification_minute"
        // Varsayılan saat (örn: 09:00)
        const val DEFAULT_HOUR = 9
        const val DEFAULT_MINUTE = 0

        // <<< YENİ: Tema Anahtarı ve Varsayılan >>>
        private const val KEY_THEME_PREFERENCE = "theme_preference"
        val DEFAULT_THEME = ThemePreference.SYSTEM
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

    // <<< YENİ: Tema Tercihi Fonksiyonları >>>
    fun saveThemePreference(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME_PREFERENCE, preference.name).apply() // Enum ismini String olarak kaydet
    }

    fun getThemePreference(): ThemePreference {
        // Kayıtlı String'i oku, yoksa varsayılanın ismini al
        val savedName = prefs.getString(KEY_THEME_PREFERENCE, DEFAULT_THEME.name)
        // String'den enum'a çevir (hata durumunda varsayılana dön)
        return try {
            ThemePreference.valueOf(savedName ?: DEFAULT_THEME.name)
        } catch (e: IllegalArgumentException) {
            DEFAULT_THEME
        }
    }

}