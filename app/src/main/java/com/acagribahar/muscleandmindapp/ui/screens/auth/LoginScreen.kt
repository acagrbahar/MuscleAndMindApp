package com.acagribahar.muscleandmindapp.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Kaydırma için
import androidx.compose.foundation.text.KeyboardOptions // Klavye tipi için
import androidx.compose.foundation.verticalScroll // Kaydırma için
import androidx.compose.material.icons.Icons // İkonlar için
import androidx.compose.material.icons.filled.Visibility // Şifre görünürlük ikonları
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email // Email ikonu
import androidx.compose.material.icons.outlined.Lock // Kilit ikonu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // Şifre görünürlük durumunu kaydetmek için
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType // Klavye tipi
import androidx.compose.ui.text.input.PasswordVisualTransformation // Şifre gizleme
import androidx.compose.ui.text.input.VisualTransformation // Şifre gösterme
import androidx.compose.ui.text.style.TextAlign // Metin hizalama
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    navigateToRegister: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") } // rememberSaveable state'i korur
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) } // Şifre görünürlük state'i
    var isLoading by remember { mutableStateOf(false) } // Yüklenme durumu - remember yeterli
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val auth: FirebaseAuth = remember { FirebaseAuth.getInstance() } // remember içinde instance alalım

    // Ana tema arka planını uygula ve kaydırma ekle
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp) // Yan boşlukları artır
                // Dikey kaydırma ekle (küçük ekranlarda taşmayı önler)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center, // İçeriği dikeyde ortala
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // <<< Logo veya Uygulama Adı Alanı (Placeholder) >>>
            // Buraya bir Image veya stilize edilmiş Text eklenebilir
            Text(
                text = "Mind & Muscle", // Örnek Uygulama Adı
                style = MaterialTheme.typography.displaySmall, // Daha büyük bir stil
                modifier = Modifier.padding(bottom = 48.dp) // Altına daha fazla boşluk
            )

            Text(
                "Giriş Yap",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // E-posta Giriş Alanı (İkonlu)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = { // <<< Başa ikon ekle >>>
                    Icon(Icons.Outlined.Email, contentDescription = "E-posta ikonu")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email) // <<< Klavye tipini ayarla >>>
            )
            Spacer(modifier = Modifier.height(16.dp)) // <<< Boşluğu artır >>>

            // Şifre Giriş Alanı (İkonlu ve Görünürlük Butonlu)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = { // <<< Başa ikon ekle >>>
                    Icon(Icons.Outlined.Lock, contentDescription = "Şifre ikonu")
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), // <<< Görünürlüğü state'e bağla >>>
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // <<< Klavye tipini ayarla >>>
                trailingIcon = { // <<< Sona ikon ekle (görünürlük için) >>>
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) { // <<< Tıklayınca state'i değiştir >>>
                        Icon(imageVector = image, description)
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp)) // <<< Boşluğu artır >>>

            // Hata Mesajı Alanı
            if (errorMessage != null) { // <<< Hata varsa göster (Box ile yükseklik ayarı) >>>
                Box(modifier = Modifier.height(40.dp)) { // Hata mesajı yokken boşluk kaplamasın diye
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Center)
                    )
                }
            } else {
                // Hata yoksa da aynı boşluğu koru (opsiyonel, hizalama için)
                Spacer(modifier = Modifier.height(40.dp))
            }


            // Giriş Butonu
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "E-posta ve şifre boş olamaz."
                        return@Button
                    }
                    isLoading = true; errorMessage = null
                    auth.signInWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Giriş başarılı!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Bilinmeyen bir hata oluştu."
                            }
                        }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp), // <<< Buton yüksekliğini ayarla >>>
                shape = MaterialTheme.shapes.medium // <<< Köşe yuvarlaklığı (veya large/extraLarge)
            ) {
                if (isLoading) { // <<< Yüklenme göstergesini buton içine al >>>
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary, // Buton üzerindeki renk
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Giriş Yap")
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // <<< Boşluğu artır >>>

            // Kayıt Ekranına Gitme Butonu
            TextButton(
                onClick = navigateToRegister,
                enabled = !isLoading
            ) {
                Text("Hesabım Yok (Kayıt Ol)")
            }
            Spacer(modifier = Modifier.height(24.dp)) // <<< Alt boşluk ekle >>>
        }
    }
}