package com.acagribahar.muscleandmindapp.ui.screens

import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acagribahar.muscleandmindapp.ui.screens.settings.SettingsViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.acagribahar.muscleandmindapp.MindMuscleApplication
import com.acagribahar.muscleandmindapp.data.local.SettingsManager
import java.util.Calendar
import androidx.compose.runtime.*
import androidx.compose.material3.*
import android.Manifest
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.semantics.Role
import com.acagribahar.muscleandmindapp.data.model.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onLogout: () -> Unit

) {
    //val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    // <<< Tema Seçim Dialog State'i >>>
    var showThemeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // SettingsManager'ı Composable içinde oluşturalım (DI olsa daha iyi olurdu)
    val settingsManager = remember { SettingsManager(context) }

    // Kaydedilmiş veya varsayılan saati al
    val initialTime = remember { settingsManager.getNotificationTime() }
    var selectedHour by remember { mutableStateOf(initialTime.first) }
    var selectedMinute by remember { mutableStateOf(initialTime.second) }

    // <<< Tema Seçim Dialog'u >>>
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.currentTheme,
            onThemeSelected = { newTheme ->
                settingsViewModel.updateThemePreference(newTheme)
                showThemeDialog = false // Dialog'u kapat
            },
            onDismiss = { showThemeDialog = false } // Kapatma isteği
        )
    }

    // === Bildirim İzni Kontrolü (Android 13+) ===
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true) // Eski sürümlerde izin otomatik var
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Bildirim izni olmadan hatırlatıcı ayarlanamaz.", Toast.LENGTH_LONG).show()
            }
        }
    )
    // === İzin Kontrolü Sonu ===

    // Time Picker Dialog'u göstermek için state
    var showTimePicker by remember { mutableStateOf(false) }

    // Time Picker Dialog oluşturucu
    if (showTimePicker) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
        calendar.set(Calendar.MINUTE, selectedMinute)

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                // Saat seçildiğinde:
                selectedHour = hourOfDay
                selectedMinute = minute
                settingsManager.saveNotificationTime(hourOfDay, minute) // Tercihi kaydet
                // WorkManager'ı yeniden planla
                try {
                    (context.applicationContext as MindMuscleApplication).scheduleDailyReminder() // Application üzerinden çağır
                    Toast.makeText(context, "Hatırlatıcı $hourOfDay:${"%02d".format(minute)} olarak ayarlandı.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Reschedule failed", e)
                    Toast.makeText(context, "Hatırlatıcı ayarlanırken hata oluştu.", Toast.LENGTH_SHORT).show()
                }
                showTimePicker = false // Dialog'u kapat
            },
            selectedHour,
            selectedMinute,
            true // 24 saat formatı
        ).show()
        // Dialog kapatıldığında state'i false yapalım ki tekrar açılmasın
        DisposableEffect(Unit) { onDispose { showTimePicker = false } }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp) // Başlığı ortala
        )

        // --- Hesap Bölümü ---
        SettingsSectionCard(title = "Hesap") {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally))
            } else if (uiState.errorMessage != null) {
                Text("Hata: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical=16.dp))
            } else {
                // Hesap Durumu Satırı
                SettingItemRow(title = "Hesap Durumu") {
                    Text(
                        if (uiState.isPremium) "Premium Üye ✨" else "Ücretsiz Kullanıcı",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                // Premium Butonu/Mesajı
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { // Butonu ortala
                    if (!uiState.isPremium) {
                        Button(onClick = { Toast.makeText(context, "Premium'a geçiş yakında!", Toast.LENGTH_SHORT).show() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Premium'a Geç")
                        }
                    } else {
                        Text("Tüm premium özellikler aktif!", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        } // Hesap Kartı Sonu

        Spacer(modifier = Modifier.height(16.dp))

        // --- Bildirimler Bölümü ---
        SettingsSectionCard(title = "Bildirimler") {
            // İzin isteme (gerekiyorsa)
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OutlinedButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.fillMaxWidth() // Genişliği doldur
                ) {
                    Text("Bildirim İzni Ver")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Saat Ayarı Satırı
            SettingItemRow(
                title = "Günlük Hatırlatma",
                enabled = hasNotificationPermission,
                onClick = {
                    if (hasNotificationPermission) { showTimePicker = true }
                    else { Toast.makeText(context, "Lütfen önce bildirim izni verin.", Toast.LENGTH_SHORT).show() }
                }
            ) {
                // Saati ve Düzenle İkonunu göster
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%02d:%02d".format(selectedHour, selectedMinute),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit, // <<< Düzenle ikonu
                        contentDescription = "Saati Değiştir",
                        modifier = Modifier.size(20.dp),
                        tint = LocalContentColor.current // enabled durumuna göre rengi ayarlanacak
                    )
                }
            }
        } // Bildirim Kartı Sonu
        Spacer(modifier = Modifier.height(16.dp))

        // --- Görünüm Bölümü (Placeholder) ---
        SettingsSectionCard(title = "Görünüm") {
            SettingItemRow(title = "Dil Seçimi", enabled = false) { Text("Türkçe (Yakında)") }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) // <<< Daha ince ayıraç
            SettingItemRow(
                title = "Tema",
                onClick = {showThemeDialog = true}
            ) {
                // <<< Seçili temayı gösteren Text >>>
                val themeText = when (uiState.currentTheme) {
                    ThemePreference.SYSTEM -> "Sistem Varsayılanı"
                    ThemePreference.LIGHT -> "Açık"
                    ThemePreference.DARK -> "Koyu"
                }
                Text(
                    text = themeText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold // Seçimi vurgula
                )

            }
        } // Görünüm Kartı Sonu

        Spacer(modifier = Modifier.weight(1f)) // Butonu en alta it

        // Çıkış Yap Butonu
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Çıkış Yap")
        }





    }

}

// SettingsScreen fonksiyonunun DIŞINA, dosyanın altına ekleyin:

// Ayarlar ekranındaki bölümleri sarmalamak için yardımcı Composable
@Composable
private fun SettingsSectionCard(
    modifier: Modifier = Modifier, // Dışarıdan modifier alabilmesi için
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) { // Dış modifier'ı uygula
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp) // Başlığa hafif sol padding
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content // Kart içeriğini buraya yerleştir
            )
        }
    }
}

@Composable
private fun SettingItemRow(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val clickModifier = if (onClick != null && enabled) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    // Tıklanamazsa içeriği soluklaştır
    // Eski Material kütüphanelerinden kalma, M3'te LocalContentColor'u manipüle etmek daha iyi:
    val currentContentColor = LocalContentColor.current
    val contentColor = if (enabled) currentContentColor else currentContentColor.copy(alpha = 0.38f) // Disabled alpha

    Row(
        modifier = clickModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Satırlar arası dikey boşluk
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Sol taraf (Başlık)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor // Soluklaştırma için renk
        )
        // Sağ taraf (Değer/Kontrol) - Alfa/Renk ayarı için Provider
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                content() // Sağ taraftaki içeriği çiz
            }
        }
    }
}

// <<< Tema Seçim Dialog Composable'ı >>>
@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema Seçin") },
        text = {
            Column {
                // Tüm enum değerleri için RadioButton oluştur
                ThemePreference.entries.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.RadioButton) { onThemeSelected(theme) } // Tıklayınca seçimi yap
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme), // Mevcut tema mı?
                            onClick = null // Row tıklanabilir olduğu için buna gerek yok
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (theme) {
                                ThemePreference.SYSTEM -> "Sistem Varsayılanı"
                                ThemePreference.LIGHT -> "Açık"
                                ThemePreference.DARK -> "Koyu"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}