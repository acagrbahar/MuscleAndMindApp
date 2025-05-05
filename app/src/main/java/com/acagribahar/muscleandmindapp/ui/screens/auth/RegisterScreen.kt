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
import androidx.compose.runtime.saveable.rememberSaveable // State'i kaydetmek için
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
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    navigateToLogin: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) } // İlk şifre alanı için
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) } // Onay şifre alanı için
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val auth: FirebaseAuth = remember { FirebaseAuth.getInstance() }

    // Ana tema arka planını uygula ve kaydırma ekle
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()), // Dikey kaydırma
            verticalArrangement = Arrangement.Center, // İçeriği dikeyde ortala
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Logo veya Uygulama Adı Alanı (LoginScreen ile aynı)
            Text(
                text = "Mind & Muscle",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Text(
                "Hesap Oluştur",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // E-posta Giriş Alanı
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = {
                    Icon(Icons.Outlined.Email, contentDescription = "E-posta ikonu")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Şifre Giriş Alanı
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = "Şifre ikonu")
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Şifre Onay Alanı
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Şifreyi Onayla") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = { // <<< İkon buraya da eklenebilir (isteğe bağlı)
                    Icon(Icons.Outlined.Lock, contentDescription = "Şifre Onay ikonu")
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, if (confirmPasswordVisible) "Şifreyi Gizle" else "Şifreyi Göster")
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Hata Mesajı Alanı
            if (errorMessage != null) {
                Box(modifier = Modifier.height(40.dp)) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Center)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Kayıt Ol Butonu
            Button(
                onClick = {
                    // Kontroller... (Aynı kalır)
                    if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) { errorMessage = "..."; return@Button }
                    if (password != confirmPassword) { errorMessage = "..."; return@Button }
                    if (password.length < 6) { errorMessage = "..."; return@Button }

                    isLoading = true; errorMessage = null
                    auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                                // <<< ÖNEMLİ: Kayıt sonrası Firestore'a varsayılan tercihleri ekleme >>>
                                // Burada onRegisterSuccess çağrılmadan önce Firestore'a yazma işlemi
                                // tetiklenebilir (örn: ViewModel aracılığıyla)
                                // Şimdilik sadece navigasyonu yapıyoruz:
                                onRegisterSuccess()
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Bilinmeyen hata."
                                if (errorMessage?.contains("email address is already in use") == true) {
                                    errorMessage = "Bu e-posta zaten kullanılıyor."
                                }
                            }
                        }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Kayıt Ol")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Giriş Ekranına Gitme Butonu
            TextButton(
                onClick = navigateToLogin,
                enabled = !isLoading
            ) {
                Text("Zaten Hesabım Var (Giriş Yap)")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}