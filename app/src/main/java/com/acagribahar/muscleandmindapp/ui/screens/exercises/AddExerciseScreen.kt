package com.acagribahar.muscleandmindapp.ui.screens.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) // Scaffold ve Snackbar için
@Composable
fun AddExerciseScreen(
    navController: NavHostController, // Geri gitmek için
    addExerciseViewModel: AddExerciseViewModel // Kaydetme işlemini yapmak için
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") } // Kategori için state

    // Kaydetme durumunu ve Snackbar state'ini yönet
    val saveState by addExerciseViewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Kaydetme başarılı olduğunda geri gitmek için Effect
    LaunchedEffect(saveState) {
        if (saveState == SaveState.Success) {
            // Başarılı mesajı gösterilebilir (opsiyonel)
            // snackbarHostState.showSnackbar("Egzersiz kaydedildi!")
            navController.popBackStack() // Geri git
            addExerciseViewModel.resetSaveState() // State'i sıfırla
        }
    }

    // Hata mesajını göstermek için Effect
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Error) {
            scope.launch {
                snackbarHostState.showSnackbar("Hata: ${(saveState as SaveState.Error).message}")
                addExerciseViewModel.resetSaveState() // State'i sıfırla
            }
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) } // Snackbar'ı göstermek için
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Yeni Egzersiz Ekle", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Egzersiz Başlığı *") }, // Zorunlu alan işareti
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState != SaveState.Loading // Yükleniyorsa pasif
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Açıklama") },
                modifier = Modifier.fillMaxWidth().height(120.dp), // Çok satırlı alan
                enabled = saveState != SaveState.Loading
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategori (Opsiyonel)") }, // Opsiyonel alan
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState != SaveState.Loading
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Kaydet Butonu
            Button(
                onClick = {
                    addExerciseViewModel.saveExercise(title, description, category)
                },
                enabled = saveState != SaveState.Loading, // Yükleniyorsa pasif
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saveState == SaveState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Kaydet")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // İptal Butonu (Opsiyonel)
            OutlinedButton(
                onClick = { navController.popBackStack() }, // Sadece geri git
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("İptal")
            }
        }
    }
}