package com.acagribahar.muscleandmindapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acagribahar.muscleandmindapp.ui.screens.progress.ProgressViewModel // ViewModel import
import androidx.compose.material.icons.Icons // Icons import
import androidx.compose.material.icons.filled.CheckCircle // Örnek rozet ikonu
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun ProgressScreen(
    progressViewModel: ProgressViewModel // ViewModel'ı parametre olarak al
) {
    // ViewModel'dan günlük ilerleme durumunu State olarak al
    val dailyProgressState by progressViewModel.dailyProgressState.collectAsStateWithLifecycle()

    val currentStreak by progressViewModel.currentStreak.collectAsStateWithLifecycle()

    // Haftalık istatistikleri State olarak al
    val weeklyStats by progressViewModel.weeklyStats.collectAsStateWithLifecycle()

    // Progress bar için animasyonlu değer
    val animatedProgress by animateFloatAsState(
        targetValue = dailyProgressState.progressPercentage,
        label = "DailyProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally // İçerikleri ortala
    ) {
        Text(
            "Gelişim Takibi",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- Günlük İlerleme Bölümü ---
        Text(
            "Bugünkü İlerleme",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // İlerleme Çubuğu (Progress Bar)
        LinearProgressIndicator(
            progress = { animatedProgress }, // Animasyonlu ilerleme değeri
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp) // Kalınlığı artırabiliriz
            // .clip(RoundedCornerShape(6.dp)) // Kenarları yuvarlak yapabiliriz (opsiyonel)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Yüzde ve Sayı Olarak İlerleme
        Text(
            // Yüzdeyi formatla (ör: %75)
            text = "Tamamlanan: ${dailyProgressState.completedCount} / ${dailyProgressState.totalCount} (%${(dailyProgressState.progressPercentage * 100).toInt()})",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- Streak Bölümü (Güncellendi) ---
        Text(
            "Seri Takibi",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mevcut Seri: ",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "$currentStreak gün", // Hesaplanan seriyi göster
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold // Seri sayısını vurgula
            )
            // Örnek: 3 gün veya üzeri seriler için basit bir rozet/ikon
            if (currentStreak >= 3) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Seri Rozeti",
                    tint = MaterialTheme.colorScheme.primary // Tema rengini kullan
                )
            }
        }


        Spacer(modifier = Modifier.height(32.dp))

        // --- Haftalık Özet Bölümü (Güncellendi) ---
        Text(
            "Haftalık Özet",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (weeklyStats.isEmpty()) {
            Text("Haftalık veri henüz yok.")
        } else {
            // Veriyi basit bir Row içinde gösterelim (veya LazyRow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween // Eşit aralıklarla dağıt
            ) {
                weeklyStats.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            stat.dayLabel,
                            style = MaterialTheme.typography.bodySmall
                        ) // Pzt, Sal...
                        Spacer(modifier = Modifier.height(4.dp))
                        // Basit bir ilerleme gösterimi (opsiyonel)
                        CircularProgressIndicator(
                            progress = { if (stat.totalCount > 0) stat.completedCount.toFloat() / stat.totalCount else 0f },
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 3.dp // Daha ince çizgi
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${stat.completedCount}/${stat.totalCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp // Biraz küçültelim
                        )
                    }
                }
            }
            // Alternatif: Daha fazla gün varsa LazyRow kullanılabilir
            /*
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(weeklyStats) { stat ->
                   // Column içeriği yukarıdaki gibi
                }
            }
            */
        }
    }
}