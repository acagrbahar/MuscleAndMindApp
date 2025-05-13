package com.acagribahar.muscleandmindapp.ui.screens

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.acagribahar.muscleandmindapp.ui.screens.exercises.DisplayExercise

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    exercisesViewModel: ExercisesViewModel, // ViewModel'ı parametre olarak al
    onExerciseClick: (DisplayExercise) -> Unit,
    navigateToAddExercise: () -> Unit

) {
    // ViewModel'dan gruplanmış egzersizleri State olarak al
    val groupedExercises by exercisesViewModel.groupedExercises.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // <<< YENİ: Ekran açıldığında reklamı yükle >>>
    LaunchedEffect(key1 = Unit) { // key1 = Unit: Sadece ekrana ilk girişte çalışır
        Log.d("ExercisesScreen", "LaunchedEffect: Calling loadInterstitialAd.")
        exercisesViewModel.loadInterstitialAd(context)
    }



    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = navigateToAddExercise) { // <<< FAB tıklandığında lambda'yı çağır
                Icon(Icons.Filled.Add, contentDescription = "Yeni Egzersiz Ekle")
            }
        }
    ) { paddingValues ->

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 16.dp)) {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Gruplar arası boşluk
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
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }

                        // Kategorideki Egzersizler
                        items(
                            items = group.exercises,
                            key = { exercise -> exercise.id }
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


}

// Tek bir egzersiz öğesini gösteren Composable (Checkbox vb. yok)
@Composable
fun ExerciseListItem(
    exercise: DisplayExercise,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(), // Başlığın altına biraz boşluk
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
            // <<< İsteğe Bağlı: Özel egzersizleri belirtmek için >>>
            if (exercise.isCustom) {
                Text(
                    text = "(Özel Egzersiz)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary, // Farklı renk
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        // Buraya tıklandığında detay sayfasına gitme işlevi eklenebilir (sonraki adım)
    }
}