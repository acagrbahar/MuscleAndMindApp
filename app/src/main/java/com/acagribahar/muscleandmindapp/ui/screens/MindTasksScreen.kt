package com.acagribahar.muscleandmindapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.ui.screens.mindtasks.MindTasksViewModel // ViewModel import
import androidx.compose.foundation.clickable // clickable import

@Composable
fun MindTasksScreen(
    mindTasksViewModel: MindTasksViewModel,
    onMindTaskClick: (DefaultTaskDto) -> Unit

) {
    // ViewModel'dan zihin görevlerini State olarak al
    val mindTasks by mindTasksViewModel.mindTasks.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Zihin Görevleri",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (mindTasks.isEmpty()) {
            // Yükleniyor veya görev yoksa mesaj göster
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Kullanılabilir zihin görevi bulunamadı veya yükleniyor...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Öğeler arası boşluk
            ) {
                items(
                    items = mindTasks,
                    key = { task -> task.title } // Benzersiz key
                ) { task ->
                    MindTaskListItem(
                        task = task,
                        modifier = Modifier.clickable { // Tıklanabilir yap
                            onMindTaskClick(task)
                        }                    )
                }
            }
        }
    }
}

// Tek bir zihin görevi öğesini gösteren Composable
@Composable
fun MindTaskListItem(
    task: DefaultTaskDto,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        // Buraya tıklandığında detay sayfasına gitme işlevi eklenecek
    }
}