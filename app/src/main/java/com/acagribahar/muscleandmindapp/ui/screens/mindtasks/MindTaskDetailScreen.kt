package com.acagribahar.muscleandmindapp.ui.screens.mindtasks

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause // Pause ikonu kullanabiliriz (şimdilik?)
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import com.acagribahar.muscleandmindapp.R

@Composable
fun MindTaskDetailScreen(
    taskTitle: String,
    mindTasksViewModel: MindTasksViewModel, // Görev detaylarını almak için
    navController: NavHostController // Geri gitmek için
) {
    // ViewModel'dan görev detaylarını al
    val task = remember(taskTitle) {
        mindTasksViewModel.getMindTaskByTitle(taskTitle)
    }

    // Sayaç state'leri
    var elapsedTimeSeconds by remember { mutableStateOf(0) }
    var isTaskRunning by remember { mutableStateOf(false) }

    // <<< Ses Oynatıcı State'leri >>>
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    // Hangi görevlerde ses dosyası olduğunu belirle (Basit kontrol)
    val audioResId: Int? = remember(task?.title) {
        when (task?.title) {
            "Odaklı Nefes" -> R.raw.meditasyon_giris // <<< Kendi dosya adınızı yazın
            // "Başka Görev" -> R.raw.baska_ses
            else -> null // Diğerleri için ses yok
        }
    }

    // Sayaç etkisi (geçen süreyi artır)
    LaunchedEffect(key1 = isTaskRunning) {
        while (isTaskRunning) { // Görev çalıştığı sürece
            delay(1000L) // 1 saniye bekle
            elapsedTimeSeconds++ // Geçen süreyi artır
        }
    }

    // <<< MediaPlayer'ı temizlemek için DisposableEffect >>>
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop() // Durdur
            mediaPlayer?.release() // Kaynakları serbest bırak
            mediaPlayer = null
            Log.d("MindTaskDetail", "MediaPlayer released on dispose")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (task == null) {
            Text("Zihin görevi bulunamadı.")
        } else {
            // Görev Bilgileri
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // <<< Sesli Rehber Butonu (varsa) >>>
            if (audioResId != null) {
                Button(
                    onClick = {
                        if (isAudioPlaying) {
                            // Çalıyorsa durdur
                            mediaPlayer?.stop()
                            mediaPlayer?.release() // Kaynakları bırak
                            mediaPlayer = null
                            isAudioPlaying = false
                            Log.d("MindTaskDetail", "Audio stopped and released")
                        } else {
                            // Duruyorsa başlat
                            try {
                                // Önce varsa eskiyi temizle (güvenlik için)
                                mediaPlayer?.release()
                                // Yenisini oluştur ve başlat
                                mediaPlayer = MediaPlayer.create(context, audioResId).apply {
                                    setOnCompletionListener { // Bitince state'i güncelle
                                        isAudioPlaying = false
                                        Log.d("MindTaskDetail", "Audio completed")
                                    }
                                    start() // Başlat
                                }
                                isAudioPlaying = true
                                Log.d("MindTaskDetail", "Audio started")
                            } catch (e: Exception) {
                                Log.e("MindTaskDetail", "Error playing audio", e)
                                // Hata mesajı gösterilebilir
                            }
                        }
                    },
                    // Butonun rengini veya ikonunu duruma göre değiştir
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(isAudioPlaying) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.MusicOff else Icons.Default.MusicNote,
                        contentDescription = "Sesli Rehber"
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(if (isAudioPlaying) "Rehberi Durdur" else "Sesli Rehberi Başlat")
                }
                Spacer(modifier = Modifier.height(16.dp)) // <<< Boşluk
            }

            // Geçen Süre Göstergesi
            Text(
                text = formatTime(elapsedTimeSeconds), // Zamanı formatla
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // Başlat / Bitir Butonu
            Button(
                onClick = {
                    isTaskRunning = !isTaskRunning // Durumu tersine çevir
                    // Eğer durdurulduysa, sayacı sıfırla (isteğe bağlı)
                    // if (!isTaskRunning) {
                    //     elapsedTimeSeconds = 0
                    // }
                },
                modifier = Modifier.size(width = 150.dp, height = 50.dp) // Buton boyutunu ayarla
            ) {
                Icon(
                    imageVector = if (isTaskRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isTaskRunning) "Bitir" else "Başlat"
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(if (isTaskRunning) "Bitir" else "Başlat", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.weight(1f)) // Butonları aşağı it

            // Geri Butonu (Opsiyonel)
            Button(onClick = { navController.popBackStack() }) {
                Text("Geri Dön")
            }
        }
    }
}

// Saniyeyi MM:SS formatına çeviren yardımcı fonksiyon (ExerciseDetail'den kopyalanabilir)
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}