package com.acagribahar.muscleandmindapp.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.remote.model.UserPreferences
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
//import com.google.firebase.auth.ktx.auth // Firebase.auth için
//import com.google.firebase.ktx.Firebase // Firebase.auth için
import com.google.firebase.auth.FirebaseUser // Tipi kullanmak için
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
//import com.google.firebase.auth.ktx.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose // awaitClose için



// Ayarlar Ekranı UI Durumu
data class SettingsUiState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val errorMessage: String? = null
    // Diğer ayarlar buraya eklenebilir (örn: notificationHour)
)

class SettingsViewModel(
    private val taskRepository: TaskRepository,
    private val firebaseAuth: FirebaseAuth

) : ViewModel() {


    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Hata veren authStateFlow yerine callbackFlow kullanacağız
    private val userStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser) // Mevcut kullanıcıyı Flow'a gönder
            Log.d("SettingsViewModel", "AuthStateListener: User = ${auth.currentUser?.uid}")
        }
        firebaseAuth.addAuthStateListener(listener) // Dinleyiciyi ekle
        Log.d("SettingsViewModel", "AuthStateListener added.")
        // Flow iptal edildiğinde dinleyiciyi kaldır
        awaitClose {
            Log.d("SettingsViewModel", "Removing AuthStateListener.")
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    init {
        // callbackFlow ile oluşturduğumuz userStateFlow'u dinle
        userStateFlow
            .onEach { firebaseUser ->
                if (firebaseUser != null) {
                    Log.d("SettingsViewModel", "User state collected: User found (${firebaseUser.uid}). Loading prefs...")
                    loadUserPreferences(firebaseUser.uid)
                } else {
                    Log.d("SettingsViewModel", "User state collected: User is null.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPremium = false,
                            errorMessage = "Giriş yapmış kullanıcı bulunamadı."
                        )
                    }
                }
            }
            .launchIn(viewModelScope) // viewModelScope içinde dinlemeyi başlat
    }

    // Artık userId parametresi alıyor
    // loadUserPreferences fonksiyonu aynı kalır
    private fun loadUserPreferences(userId: String) {
        Log.d("SettingsViewModel", "loadUserPremiumStatus called for userId: $userId") // <<< EKLE

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch { // Tekrar launch açmak daha güvenli olabilir
            try {
                Log.d("SettingsViewModel", "Calling taskRepository.getUserPreferences for $userId")

                var prefs = taskRepository.getUserPreferences(userId)

                Log.d("SettingsViewModel", "Prefs fetched for $userId: $prefs")

                if (prefs == null) {
                    Log.d("SettingsViewModel", "Preferences not found for $userId, attempting to create default...")
                    taskRepository.createUserPreferences(userId)
                    Log.d("SettingsViewModel", "Re-calling taskRepository.getUserPreferences for $userId after creation attempt.")

                    prefs = taskRepository.getUserPreferences(userId)
                    Log.d("SettingsViewModel", "Prefs re-fetched for $userId: $prefs") // <<< Tekrar loglayalım
                }

                if (prefs != null) {
                    Log.d("SettingsViewModel", "Updating UI state for $userId. isPremium value from prefs: ${prefs.isPremium}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPremium = prefs.isPremium,
                            errorMessage = null
                        )
                    }
                } else {
                    Log.e("SettingsViewModel", "Failed to load or create preferences for $userId even after retry.")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Kullanıcı tercihleri yüklenemedi.")
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error in loadUserPreferences for $userId: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Tercihler yüklenirken hata oluştu: ${e.message}")
                }
            }
        } // viewModelScope.launch sonu
    }

    // Çıkış yapma işlevini buraya taşıyabiliriz (veya MainActivity'de kalabilir)
    // fun signOut() {
    //     firebaseAuth.signOut()
    //     // Navigasyon tetikleme olayı gönderilebilir (event flow vb.)
    // }
}