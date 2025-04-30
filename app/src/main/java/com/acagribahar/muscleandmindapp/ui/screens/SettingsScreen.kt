package com.acagribahar.muscleandmindapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import com.acagribahar.muscleandmindapp.navigation.Screen.Graph // Rota sabitleri için import
//import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acagribahar.muscleandmindapp.ui.screens.settings.SettingsViewModel
import androidx.compose.runtime.getValue

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onLogout: () -> Unit

) {
    //val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))


        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.errorMessage != null) {
            Text("Hata: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
        } else {
            // Premium Durumu
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Hesap Durumu", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (uiState.isPremium) "Premium Üye ✨" else "Ücretsiz Kullanıcı",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Premium Butonu/Mesajı
                    if (!uiState.isPremium) {
                        Button(onClick = {
                            // TODO: Play Billing akışını başlat
                            Toast.makeText(context, "Premium'a geçiş yakında!", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Premium'a Geç")
                        }
                    } else {
                        Text("Tüm premium özellikler aktif!")
                        // Buraya "Aboneliği Yönet" butonu eklenebilir
                    }
                }
            }
        }

        // Diğer ayar öğeleri buraya eklenebilir...
        Spacer(modifier = Modifier.height(16.dp))
        Text("Dil Seçimi (Yakında)")
        Text("Tema Seçimi (Yakında)")
        Text("Bildirim Ayarları (Yakında)")

        Spacer(modifier = Modifier.weight(1f))

        // Çıkış Yap Butonu
        Button(
            // <<< onClick içindeki eski kodu silip sadece onLogout() çağırın >>>
            onClick = onLogout, // MainActivity'den gelen lambda'yı çağırır (hem signOut hem navigate yapar)
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Çıkış Yap")
        }

    }

}