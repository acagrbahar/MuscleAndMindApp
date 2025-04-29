package com.acagribahar.muscleandmindapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi // stickyHeader için
import androidx.compose.foundation.background // Header arkaplanı için
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items için
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.ui.screens.exercises.ExercisesViewModel // ViewModel import
import androidx.compose.foundation.clickable // clickable import

@OptIn(ExperimentalFoundationApi::class) // stickyHeader için gerekli
@Composable
fun ExercisesScreen(
    exercisesViewModel: ExercisesViewModel, // ViewModel'ı parametre olarak al
    onExerciseClick: (DefaultTaskDto) -> Unit // Yeni lambda parametresi

) {
    // ViewModel'dan gruplanmış egzersizleri State olarak al
    val groupedExercises by exercisesViewModel.groupedExercises.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Tüm Egzersizler",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (groupedExercises.isEmpty()) {
            // Yükleniyor veya egzersiz yoksa mesaj göster
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Kullanılabilir egzersiz bulunamadı veya yükleniyor...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Gruplar arası boşluk
            ) {
                groupedExercises.forEach { group ->
                    // Kategori Başlığı (Sticky Header)
                    stickyHeader {
                        Text(
                            text = group.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant) // Başlık arkaplanı
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }

                    // Kategorideki Egzersizler
                    items(
                        items = group.exercises,
                        key = { exercise -> "${group.category}_${exercise.title}" }
                    ) { exercise ->
                        // ExerciseListItem'a tıklama işlevini ekle
                        ExerciseListItem(
                            exercise = exercise,
                            modifier = Modifier.clickable { // Tıklanabilir yap
                                onExerciseClick(exercise) // Lambda'yı çağır
                            }
                        )
                    }
                }
            }
        }
    }
}

// Tek bir egzersiz öğesini gösteren Composable (Checkbox vb. yok)
@Composable
fun ExerciseListItem(
    exercise: DefaultTaskDto,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp), // Başlığın altına biraz boşluk
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (exercise.description.isNotBlank()) {
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        // Buraya tıklandığında detay sayfasına gitme işlevi eklenebilir (sonraki adım)
    }
}