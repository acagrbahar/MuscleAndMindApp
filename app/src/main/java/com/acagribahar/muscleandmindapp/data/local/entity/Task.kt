package com.acagribahar.muscleandmindapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks") // Veritabanı tablo adı
data class Task(
    @PrimaryKey(autoGenerate = true) // Otomatik artan ID
    val id: Int = 0,

    val type: String, // "mind" veya "body" gibi bir değer alabilir
    val title: String,
    val description: String,

    var isCompleted: Boolean = false, // Tamamlanma durumu, varsayılan olarak false

    val date: Long // Görevin ait olduğu tarih (timestamp olarak saklamak genellikle daha kolaydır)
    // Alternatif olarak String "YYYY-MM-DD" formatında da saklanabilir,
    // ancak Long karşılaştırma ve sıralama için daha pratiktir.
)

// Not: type alanı için Enum kullanmak daha güvenli olabilir:
// enum class TaskType { MIND, BODY }
// Ancak Enum'ı Room'da saklamak için TypeConverter gerekebilir.
// Şimdilik String olarak başlayalım.