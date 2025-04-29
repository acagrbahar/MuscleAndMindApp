package com.acagribahar.muscleandmindapp.data.model

import kotlinx.serialization.Serializable // kotlinx-serialization için import

@Serializable // Bu sınıfın serileştirilebilir olduğunu belirtir
data class DefaultTaskDto(
    val type: String,
    val title: String,
    val description: String
)