package com.acagribahar.muscleandmindapp.ui.screens.progress

import java.util.Locale // Locale import

data class WeeklyStat(
    val dayLabel: String, // Örn: "Pzt", "Sal" veya "29/4"
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val timestamp: Long = 0L // O günün başlangıç timestamp'i
)