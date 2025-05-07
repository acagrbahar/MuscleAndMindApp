package com.acagribahar.muscleandmindapp.ui.screens

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acagribahar.muscleandmindapp.MindMuscleApplication
import com.acagribahar.muscleandmindapp.data.local.SettingsManager
import com.acagribahar.muscleandmindapp.data.model.ThemePreference
import com.acagribahar.muscleandmindapp.ui.screens.settings.SettingsUiState
import com.acagribahar.muscleandmindapp.ui.screens.settings.SettingsViewModel
import java.util.Calendar
import com.google.firebase.appcheck.interop.BuildConfig


// Not: NavHostController ve Graph importları kaldırıldı, çünkü bu ekranda kullanılmıyorlar.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onLogout: () -> Unit
) {
    // State'ler ve Context (Değişiklik yok)
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val initialTime = remember { settingsManager.getNotificationTime() }
    var selectedHour by remember { mutableStateOf(initialTime.first) }
    var selectedMinute by remember { mutableStateOf(initialTime.second) }
    val appVersion = BuildConfig.VERSION_NAME

    val uriHandler = LocalUriHandler.current

    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
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
    var showTimePicker by remember { mutableStateOf(false) }

    // Dialoglar (Değişiklik yok)
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.currentTheme,
            onThemeSelected = { newTheme ->
                settingsViewModel.updateThemePreference(newTheme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                settingsManager.saveNotificationTime(hourOfDay, minute)
                try {
                    (context.applicationContext as MindMuscleApplication).scheduleDailyReminder()
                    Toast.makeText(context, "Hatırlatıcı $hourOfDay:${"%02d".format(minute)} olarak ayarlandı.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Reschedule failed", e)
                    Toast.makeText(context, "Hatırlatıcı ayarlanırken hata oluştu.", Toast.LENGTH_SHORT).show()
                }
                showTimePicker = false
            },
            selectedHour,
            selectedMinute,
            true // 24 saat formatı
        )
        // Dialog'un dismiss edilmesini dinle (opsiyonel ama iyi pratik)
        DisposableEffect(Unit) {
            onDispose {
                if (timePickerDialog.isShowing) {
                    // Eğer kullanıcı dışarı tıklayarak kapattıysa state'i güncelle
                    showTimePicker = false
                }
            }
        }
        timePickerDialog.show()
    }


    // --- DÜZENLENMİŞ LAYOUT YAPISI (Box Kullanarak) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // Genel kenar boşluğu Box'a uygulandı
    ) {
        // --- Ayarlar İçeriği (Başlık ve Kartlar) için Column ---
        Column(
            modifier = Modifier
                .weight(1f).verticalScroll(rememberScrollState()) // <<< Box'ın üstüne hizala
        ) {
            Text(
                "Ayarlar",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )

            // --- Hesap Bölümü ---
            SettingsSectionCard(title = "Hesap") {
                if (uiState.isLoading || uiState.isLoadingBilling) {
                    Row( // <<< Ortalamak için Row içine alalım >>>
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ){
                        CircularProgressIndicator()
                    }
                } else if (uiState.errorMessage != null) {
                    Text("Hata: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 16.dp))
                } else if (uiState.billingError != null) { // Billing hatası
                    Text("Hata: ${uiState.billingError}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical=16.dp))
                    // <<< Hatayı temizleme butonu (opsiyonel) >>>
                    // Button(onClick = { settingsViewModel.clearBillingError() }) { Text("Tekrar Dene") }
                } else {
                    SettingItemRow(title = "Hesap Durumu") {
                        Text(
                            if (uiState.isPremium) "Premium Üye ✨" else "Ücretsiz Kullanıcı",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Premium Butonu/Mesajı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!uiState.isPremium) {
                            // <<< Buton ve Fiyatı Column içine alalım >>>
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        // <<< Satın alma akışını başlat >>>
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            settingsViewModel.launchBillingFlow(activity)
                                        } else {
                                            Log.e("SettingsScreen", "Activity context is null, cannot launch billing flow.")
                                            Toast.makeText(context, "Satın alma başlatılamadı.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    // <<< Ürün detayı yoksa butonu pasif yap >>>
                                    enabled = uiState.premiumProductDetails != null
                                ) {
                                    Text("Premium'a Geç")
                                }

                                // <<< Fiyatı göster (varsa) >>>
                                uiState.premiumPrice?.let { price ->
                                    Text(
                                        text = "$price / Ay", // Örn: ₺19,99 / Ay
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    // Ürün detayı yüklenemezse veya fiyat yoksa bilgi verilebilir
                                } ?: Text(
                                    text = "Fiyat bilgisi yüklenemedi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } // Column sonu
                        } else {
                            Text("Tüm premium özellikler aktif!", modifier = Modifier.padding(vertical = 8.dp))
                            // Aboneliği yönetme butonu buraya eklenebilir
                        }
                    } // Row sonu
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Bildirimler Bölümü ---
            SettingsSectionCard(title = "Bildirimler") {
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bildirim İzni Ver")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                SettingItemRow(
                    title = "Günlük Hatırlatıcı",
                    enabled = hasNotificationPermission
                ) {
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { newValue ->
                            settingsViewModel.updateNotificationsEnabled(newValue)
                            // Schedule/Cancel reminder immediately
                            try { (context.applicationContext as MindMuscleApplication).scheduleDailyReminder() } catch (e: Exception) {Log.e("SettingsScreen", "Reschedule failed on toggle", e)}
                        },
                        enabled = hasNotificationPermission
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingItemRow(
                    title = "Hatırlatma Saati",
                    enabled = hasNotificationPermission && uiState.notificationsEnabled,
                    onClick = {
                        if (hasNotificationPermission && uiState.notificationsEnabled) { showTimePicker = true }
                        else if (!hasNotificationPermission) { Toast.makeText(context, "Lütfen önce bildirim izni verin.", Toast.LENGTH_SHORT).show() }
                        else { Toast.makeText(context, "Önce bildirimleri açın.", Toast.LENGTH_SHORT).show() }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "%02d:%02d".format(selectedHour, selectedMinute),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Saati Değiştir",
                            modifier = Modifier.size(20.dp),
                            tint = LocalContentColor.current
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Görünüm Bölümü ---
            SettingsSectionCard(title = "Görünüm") {
                SettingItemRow(title = "Dil Seçimi", enabled = false) { Text("Türkçe (Yakında)") }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingItemRow(
                    title = "Tema",
                    onClick = { showThemeDialog = true }
                ) {
                    val themeText = when (uiState.currentTheme) {
                        ThemePreference.SYSTEM -> "Sistem Varsayılanı"
                        ThemePreference.LIGHT -> "Açık"
                        ThemePreference.DARK -> "Koyu"
                    }
                    Text(
                        text = themeText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- YENİ BÖLÜM: Hakkında ve Yasal ---
            SettingsSectionCard(title = "Hakkında ve Yasal") {
                // Gizlilik Politikası Satırı
                SettingItemRow(
                    title = "Gizlilik Politikası",
                    onClick = {
                        // <<< Yer tutucu URL'yi kendi URL'nizle değiştirin >>>
                        val privacyUrl = "https://yourdomain.com/privacy" // ÖRNEK URL
                        try { uriHandler.openUri(privacyUrl) } catch (e: Exception) {
                            Log.e("SettingsScreen", "Could not open privacy policy URL", e)
                            Toast.makeText(context, "Link açılamadı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Git")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Kullanım Koşulları Satırı
                SettingItemRow(
                    title = "Kullanım Koşulları",
                    onClick = {
                        // <<< Yer tutucu URL'yi kendi URL'nizle değiştirin >>>
                        val termsUrl = "https://yourdomain.com/terms" // ÖRNEK URL
                        try { uriHandler.openUri(termsUrl) } catch (e: Exception) {
                            Log.e("SettingsScreen", "Could not open terms URL", e)
                            Toast.makeText(context, "Link açılamadı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Git")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Uygulamayı Değerlendir Satırı (Opsiyonel)
                SettingItemRow(
                    title = "Uygulamayı Değerlendir",
                    onClick = {
                        val packageName = context.packageName
                        val playStoreUrl = "market://details?id=$packageName"
                        val webUrl = "https://play.google.com/store/apps/details?id=$packageName"
                        try {
                            // Önce Play Store uygulamasını açmayı dene
                            uriHandler.openUri(playStoreUrl)
                        } catch (e: Exception) {
                            // Play Store yoksa web sayfasını açmayı dene
                            Log.w("SettingsScreen", "Play Store app not found, trying web URL", e)
                            try { uriHandler.openUri(webUrl) } catch (e2: Exception) {
                                Log.e("SettingsScreen", "Could not open Play Store URL", e2)
                                Toast.makeText(context, "Play Store linki açılamadı.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Git")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Uygulama Versiyonu Satırı
                SettingItemRow(title = "Versiyon", enabled = false) { // Tıklanamaz
                    Text(appVersion, fontWeight = FontWeight.Bold) // Versiyonu göster
                }
            } // Hakkında Kartı Sonu
        } // --- Ayarlar İçeriği Column Sonu ---


        // --- Çıkış Yap Butonu (Box'ın altına hizalı) ---
        // <<< Butonu sarmalayan Box ve align modifier'ı >>>

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth() // Buton genişliği doldursun
            ) {
                Text("Çıkış Yap")
            }
        } // --- Çıkış Yap Butonu Box Sonu ---

    } // <<< Ana Box Sonu >>>



// --- Yardımcı Composable Fonksiyonlar ---

@Composable
private fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
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
    val currentContentColor = LocalContentColor.current
    val contentColor = if (enabled) currentContentColor else currentContentColor.copy(alpha = 0.38f)

    Row(
        modifier = clickModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                content()
            }
        }
    }
}

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
                ThemePreference.entries.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.RadioButton) { onThemeSelected(theme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = null
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