package com.acagribahar.muscleandmindapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items için import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // by için import
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // collectAsStateWithLifecycle için import
import androidx.lifecycle.viewmodel.compose.viewModel // viewModel() için import
import com.acagribahar.muscleandmindapp.data.local.entity.Task
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModelFactory // Factory import


@Composable
fun HomeScreen(
    // NavController vb. parametreler gerekirse buraya eklenebilir
    homeViewModel: HomeViewModel
) {
    // ViewModel'dan görev akışını (Flow) alıp State'e dönüştür
    // initialValue: Flow'dan ilk değer gelene kadar gösterilecek varsayılan değer
    // collectAsStateWithLifecycle: Lifecycle'a duyarlı şekilde Flow'u dinler
    val tasks by homeViewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())

    // Bugünün tarihini formatlayıp başlıkta gösterelim
    val todayDateFormatted = remember { // Sadece bir kere hesaplansın
        SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR")).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bugün Yapılacaklar",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = todayDateFormatted,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Görev Listesi
        if (tasks.isEmpty()) {
            // Görev yoksa gösterilecek mesaj
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bugün için planlanmış görev yok.")
            }
        } else {
            // Görevler varsa LazyColumn içinde listele
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Öğeler arası boşluk
            ) {
                items(
                    items = tasks,
                    key = { task -> task.id } // Performans için sabit bir key belirt
                ) { task ->
                    TaskItem(
                        task = task,
                        onToggleComplete = {
                            // Checkbox durumu değiştiğinde ViewModel'daki fonksiyonu çağır
                            homeViewModel.toggleTaskCompletion(task)
                        }
                    )
                }
            }
        }
    }
}

// Tek bir görev öğesini gösteren Composable
@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    // <<< Tamamlanma durumuna göre renk ve metin dekorasyonu belirle >>>
    val textColor = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else LocalContentColor.current
    val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null

    // <<< Görev tipine göre ikon belirle >>>
    val typeIcon = when(task.type.lowercase(Locale.getDefault())) { // Küçük harfe çevirerek kontrol
        "body" -> Icons.Filled.FitnessCenter
        "mind" -> Icons.Filled.Psychology
        else -> null // Bilinmeyen tip için ikon yok
    }

    Card( // Görevleri kart içinde göstermek daha şık olabilir
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if(task.isCompleted) 1.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant // Tamamlanmışsa biraz daha soluk arkaplan
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete(task) } // Durum değiştiğinde lambda'yı çağır
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { // Yazıların kalan alanı doldurmasını sağlar
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textDecoration = textDecoration
                )
                if (task.description.isNotBlank()) { // Açıklama varsa göster
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                        color = textColor,
                        textDecoration = textDecoration
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    typeIcon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = "Tür: ${task.type}",
                            modifier = Modifier.size(16.dp), // İkon boyutu
                            tint = textColor // İkon rengi de soluklaşsın
                        )
                        Spacer(modifier = Modifier.width(4.dp)) // İkon ve metin arası boşluk
                    }
                    Text(
                        text = task.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, // İlk harfi büyük yap
                        style = MaterialTheme.typography.labelSmall,
                        // <<< Renk uygula >>>
                        color = textColor
                    )

                }

            }
        }
    }
}