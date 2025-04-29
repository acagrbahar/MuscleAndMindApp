package com.acagribahar.muscleandmindapp.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    navigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current // Toast için

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hesap Oluştur", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Şifreyi Onayla") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
        }

        // Kayıt Ol Butonu onClick Güncellemesi
        Button(
            onClick = {
                // Kontroller
                if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    errorMessage = "Tüm alanlar doldurulmalıdır."
                    return@Button
                }
                if (password != confirmPassword) {
                    errorMessage = "Şifreler eşleşmiyor."
                    return@Button
                }
                // Firebase genellikle min 6 karakter şifre ister, kontrol eklenebilir
                if (password.length < 6) {
                    errorMessage = "Şifre en az 6 karakter olmalıdır."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                    .addOnCompleteListener { task ->
                        isLoading = false // İşlem bitti
                        if (task.isSuccessful) {
                            // Kayıt başarılı! Navigasyonu tetikle.
                            Toast.makeText(context, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                            onRegisterSuccess()
                        } else {
                            // Kayıt başarısız. Hata mesajını göster.
                            errorMessage = task.exception?.localizedMessage ?: "Bilinmeyen bir hata oluştu."
                            // Firebase'den gelen yaygın hataları daha anlaşılır hale getirebiliriz:
                            if (errorMessage?.contains("email address is already in use", ignoreCase = true) == true) {
                                errorMessage = "Bu e-posta adresi zaten kullanılıyor."
                            }
                            // Toast.makeText(context, "Kayıt başarısız: ${errorMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kayıt Ol")
        }
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = navigateToLogin,
            enabled = !isLoading
        ) {
            Text("Zaten Hesabım Var (Giriş Yap)")
        }
    }
}