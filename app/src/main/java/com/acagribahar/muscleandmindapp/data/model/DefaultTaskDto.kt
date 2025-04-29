package com.acagribahar.muscleandmindapp.data.model


import kotlinx.serialization.Serializable

@Serializable
data class DefaultTaskDto(
    val type: String,
    val category: String? = null, // Kategori alanı eklendi (nullable olabilir)
    val title: String,
    val description: String
)