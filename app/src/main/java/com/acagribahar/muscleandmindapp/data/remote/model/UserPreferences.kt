package com.acagribahar.muscleandmindapp.data.remote.model

import com.google.firebase.firestore.PropertyName

// Firestore'dan okuma/yazma için boş constructor gerekir,
// bu yüzden alanlara varsayılan değerler veriyoruz.
data class UserPreferences(
    val userId: String = "", // Kullanıcının Firebase Auth UID'si

    @get:PropertyName("premium")
    @set:PropertyName("premium")
    var isPremium: Boolean = false, // Premium durumu

    val notificationHour: Int = 9 // Bildirim saati (Adım 8 için ileride kullanılabilir)
    // Buraya başka kullanıcı tercihleri eklenebilir (tema, dil vb.)
)