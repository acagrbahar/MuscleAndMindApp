package com.acagribahar.muscleandmindapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.acagribahar.muscleandmindapp.navigation.Screen.Graph // Rota sabitleri için import
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(
    navController: NavHostController // Üst seviye NavController parametresi
) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Butonu ortalamak için
    ) {
        Text("Ayarlar Ekranı", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        // Çıkış Yap Butonu
        Button(onClick = {
            // 1. Firebase'den çıkış yap
            auth.signOut()

            // 2. Kullanıcıyı Login ekranına yönlendir (Auth Grafiğine)
            //    ve Main Grafiği geri yığından temizle
            navController.navigate(Graph.AUTHENTICATION) {
                popUpTo(Graph.MAIN) { // Ana grafiğe kadar olan her şeyi temizle
                    inclusive = true // Ana grafik de dahil olmak üzere temizle
                }
                launchSingleTop = true // Auth grafiğinden zaten varsa yenisini açma
            }
        }) {
            Text("Çıkış Yap")
        }

        // Diğer ayar öğeleri buraya eklenebilir...
        Spacer(modifier = Modifier.height(16.dp))
        Text("Dil Seçimi (Yakında)")
        Text("Tema Seçimi (Yakında)")
        Text("Bildirim Ayarları (Yakında)")

    }
}