package com.acagribahar.muscleandmindapp.data.remote.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp // Sunucu zaman damgası için (opsiyonel)
import java.util.Date // Zaman damgası için

// Kullanıcının özel egzersizlerini temsil eden data class
data class UserExercise(
    @DocumentId // Firestore'un otomatik oluşturduğu doküman ID'sini bu alana atar
    val id: String = "",

    // Güvenlik ve sorgulama için egzersizin hangi kullanıcıya ait olduğunu belirtir.
    // Firestore kurallarında bu alanı kontrol edebiliriz.
    val userId: String = "",

    var title: String = "", // Kullanıcının girdiği başlık (var yapalım, güncellenebilir?)
    var description: String = "", // Kullanıcının girdiği açıklama
    var category: String? = null, // Kullanıcının seçtiği kategori (opsiyonel)

    // @ServerTimestamp // Bu annotation, Firestore'un eklenme zamanını otomatik atamasını sağlar (opsiyonel)
    // val createdAt: Date? = null // Oluşturulma zamanı (opsiyonel)

    // Gelecekte eklenebilecek diğer alanlar:
    // val durationSeconds: Int? = null, // Süre (saniye)
    // val sets: Int? = null, // Set sayısı
    // val reps: Int? = null, // Tekrar sayısı
) {
    // Firestore'un data class'ları düzgün işlemesi için genellikle boş bir constructor gerekir.
    // Kotlin data class'ları varsayılan değerlerle bunu otomatik sağlar.
}