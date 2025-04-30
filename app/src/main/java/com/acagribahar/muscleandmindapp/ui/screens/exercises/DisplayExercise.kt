package com.acagribahar.muscleandmindapp.ui.screens.exercises

// Hem DefaultTaskDto hem de UserExercise'i UI'da göstermek için ortak model
data class DisplayExercise(
    val id: String, // Benzersiz bir kimlik (UserExercise için Firestore ID, DTO için title olabilir)
    val title: String,
    val description: String,
    val category: String?, // Kategori (nullable olabilir)
    val isCustom: Boolean // Bu egzersiz kullanıcı tarafından mı eklendi?
)