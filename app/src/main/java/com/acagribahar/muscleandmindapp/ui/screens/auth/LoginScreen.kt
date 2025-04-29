package com.acagribahar.muscleandmindapp.ui.screens.auth

import android.widget.Toast // Hata mesajları için Toast kullanabiliriz
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Toast için Context
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    navigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current // Toast göstermek için

    // Firebase Auth instance'ını alalım
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Giriş Yap", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Yüklenirken pasif
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Yüklenirken pasif
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

        // Giriş Butonu onClick Güncellemesi
        Button(
            onClick = {
                // Basit Kontrol
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "E-posta ve şifre boş olamaz."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                auth.signInWithEmailAndPassword(email.trim(), password.trim())
                    .addOnCompleteListener { task ->
                        isLoading = false // İşlem bitti, yüklenmeyi durdur
                        if (task.isSuccessful) {
                            // Giriş başarılı! Navigasyonu tetikle.
                            Toast.makeText(context, "Giriş başarılı!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            // Giriş başarısız. Hata mesajını göster.
                            errorMessage = task.exception?.localizedMessage ?: "Bilinmeyen bir hata oluştu."
                            // Toast.makeText(context, "Giriş başarısız: ${errorMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Giriş Yap")
        }
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = navigateToRegister,
            enabled = !isLoading
        ) {
            Text("Hesabım Yok (Kayıt Ol)")
        }
    }
}