package com.acagribahar.muscleandmindapp.ui.screens.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

@Composable
fun ExerciseDetailScreen(
    exerciseTitle: String,
    exercisesViewModel: ExercisesViewModel, // Egzersiz detaylarını almak için
    navController: NavHostController // Geri gitmek için
) {
    // ViewModel'dan egzersiz detaylarını al (veya hata yönetimi)
    val exercise = remember(exerciseTitle) {
        exercisesViewModel.getExerciseByTitle(exerciseTitle)
    }

    // Zamanlayıcı state'leri
    val totalSeconds = 60 // Varsayılan süre (örn: 1 dakika) - Egzersize göre değişebilir
    var remainingSeconds by remember { mutableStateOf(totalSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Zamanlayıcı etkisi
    LaunchedEffect(key1 = remainingSeconds, key2 = isTimerRunning) {
        // Timer çalışıyorsa ve süre bitmediyse...
        if (isTimerRunning && remainingSeconds > 0) {
            delay(1000L) // 1 saniye bekle
            remainingSeconds-- // Süreyi azalt
        }
        // Süre bittiğinde otomatik durdur (isteğe bağlı)
        // else if (remainingSeconds == 0) {
        //     isTimerRunning = false
        // }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (exercise == null) {
            Text("Egzersiz bulunamadı.")
            // Geri dön butonu eklenebilir
        } else {
            // Egzersiz Bilgileri
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Zamanlayıcı Göstergesi
            Text(
                text = formatTime(remainingSeconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // Zamanlayıcı Kontrolleri
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Başlat / Duraklat Butonu
                Button(onClick = { isTimerRunning = !isTimerRunning }) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isTimerRunning) "Duraklat" else "Başlat"
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(if (isTimerRunning) "Duraklat" else "Başlat")
                }

                // Sıfırla Butonu
                Button(
                    onClick = {
                        isTimerRunning = false
                        remainingSeconds = totalSeconds
                    },
                    enabled = remainingSeconds < totalSeconds // Sadece süre ilerlediğinde aktif
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "Sıfırla"
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Sıfırla")
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Butonları aşağı itmek için

            // Geri Butonu (Opsiyonel)
            Button(onClick = { navController.popBackStack() }) {
                Text("Geri Dön")
            }
        }
    }
}

// Saniyeyi MM:SS formatına çeviren yardımcı fonksiyon
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

// ViewModel'a egzersizi başlığa göre bulma fonksiyonunu eklememiz lazım
// ExercisesViewModel.kt içine:
/*
fun getExerciseByTitle(title: String): DefaultTaskDto? {
    // _groupedExercises StateFlow'undaki güncel değeri alıp arama yap
    return groupedExercises.value
        .flatMap { it.exercises } // Tüm egzersizleri tek listeye çevir
        .find { it.title == title } // Başlığa göre bul
}
*/