package com.acagribahar.muscleandmindapp.ui.screens

// --- Gerekli Importlar ---
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items için import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline // Boş durum ikonu için
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // by için import
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Renk için
import androidx.compose.ui.graphics.vector.ImageVector // İkon tipi için
import androidx.compose.ui.text.font.FontStyle // İtalik için
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Metin hizalama için
import androidx.compose.ui.text.style.TextDecoration // Üstünü çizmek için
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // State toplama için
import com.acagribahar.muscleandmindapp.data.local.entity.Task


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// <<< HomeViewModelFactory importu burada gerekli değil >>>
// import com.acagribahar.muscleandmindapp.ui.screens.HomeViewModelFactory // Factory import

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel // <<< Parametre doğru
) {
    // <<< ViewModel'dan uiState'i al (tasks yerine) >>>
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    // Bugünün tarihini formatlama (Değişiklik yok)
    val todayDateFormatted = remember {
        SimpleDateFormat("dd MMMM YYYY, EEEE", Locale("tr", "TR")).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Başlık ve Tarih (Değişiklik yok) ---
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

        // --- YENİ: Günlük İlerleme Özeti ---
        // Yükleme bitmeden gösterme (isteğe bağlı)
        if (!uiState.isLoadingTasks && !uiState.isLoadingPremium) {
            DailyProgressSummary(uiState = uiState)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Yükleniyor / Boş Durum / Görev Listesi ---
        // Hem görevler hem premium durumu yüklenene kadar bekle
        if (uiState.isLoadingTasks || uiState.isLoadingPremium) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.tasks.isEmpty()) {
            // Görev yoksa boş durum görünümü
            EmptyTasksView() // <<< İyileştirilmiş boş durum görünümü
        } else {
            // Görevler varsa Liste ve Motivasyon Sözü
            LazyColumn(
                // <<< Modifier'ı fillMaxSize yerine weight(1f) yapalım ki alttaki söz için yer kalsın (eğer söz dışarıda olacaksa)
                // <<< VEYA Sözü listenin son item'ı yapalım (şimdilik böyle yapıyoruz) >>>
                modifier = Modifier.fillMaxSize(), // Söz listenin içinde olduğu için fillMaxSize kalabilir
                verticalArrangement = Arrangement.spacedBy(12.dp) // Boşluğu biraz artıralım
            ) {
                items(
                    // <<< Görev listesi uiState içinden alınır >>>
                    items = uiState.tasks,
                    key = { task -> task.id }
                ) { task ->
                    TaskItem( // <<< TaskItem Composable'ı aynı >>>
                        task = task,
                        onToggleComplete = { homeViewModel.toggleTaskCompletion(task) }
                    )
                }

                // <<< YENİ: Motivasyon Sözü (Listenin son elemanı) >>>
                item {
                    Spacer(modifier = Modifier.height(16.dp)) // Üstüne boşluk
                    MotivationalQuoteCard(quote = uiState.motivationalQuote)
                    Spacer(modifier = Modifier.height(16.dp)) // Altına boşluk (opsiyonel)
                }
            }
        }
    } // Ana Column Sonu
}

// =============================================
// === YARDIMCI COMPOSABLE FONKSİYONLAR ===
// =============================================

// --- TaskItem (Mevcut güncellenmiş hali) ---
@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else LocalContentColor.current
    val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
    val typeIcon = when(task.type.lowercase(Locale.getDefault())) {
        "body" -> Icons.Filled.FitnessCenter
        "mind" -> Icons.Filled.Psychology
        else -> null
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if(task.isCompleted) 1.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleComplete(task) })
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor, textDecoration = textDecoration)
                if (task.description.isNotBlank()) {
                    Text(text = task.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp), color = textColor, textDecoration = textDecoration)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    typeIcon?.let {
                        Icon(imageVector = it, contentDescription = "Tür: ${task.type}", modifier = Modifier.size(16.dp), tint = textColor)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(text = task.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, style = MaterialTheme.typography.labelSmall, color = textColor)
                }
            }
        }
    }
}

// --- YENİ: Günlük İlerleme Özetini Gösteren Composable ---
@Composable
private fun DailyProgressSummary(uiState: HomeUiState) {
    // Yükleme bitmeden veya görev yoksa gösterme
    if (uiState.isLoadingTasks || uiState.isLoadingPremium || uiState.totalTasks == 0) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // Hafif gölge
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp) // İç boşluk
        ) {
            Box(contentAlignment = Alignment.Center) {
                // İlerlemeyi animasyonlu gösterelim (opsiyonel ama şık)
                val animatedProgress by animateFloatAsState(
                    targetValue = uiState.dailyProgressPercentage,
                    label = "DailyProgressCircleAnimation"
                )
                CircularProgressIndicator(
                    progress = { animatedProgress }, // <<< AnimateFloatAsState'den gelen değer
                    modifier = Modifier.size(50.dp),
                    strokeWidth = 4.dp
                )
                Text(
                    text = "%${(uiState.dailyProgressPercentage * 100).toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${uiState.completedTasks} / ${uiState.totalTasks} görev tamamlandı.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// --- YENİ: Motivasyon Sözünü Gösteren Composable ---
@Composable
private fun MotivationalQuoteCard(quote: String) {
    // Söz boşsa gösterme
    if (quote.isBlank()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) // Farklı bir renk
    ) {
        Text(
            text = "\"$quote\"",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        )
    }
}

// --- YENİ: Boş Liste Durumunu Gösteren Composable ---
@Composable
private fun EmptyTasksView() {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircleOutline, // Onay veya takvim ikonu da olabilir
                contentDescription = null,
                modifier = Modifier.size(60.dp).padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Harika!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                "Bugün için planlanmış başka görev yok.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Dinlenmenin veya serbest çalışmanın tadını çıkarın!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}