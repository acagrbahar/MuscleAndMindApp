package com.acagribahar.muscleandmindapp.ui.screens.settings

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.biling.BillingClientWrapper
import com.acagribahar.muscleandmindapp.data.local.SettingsManager
import com.acagribahar.muscleandmindapp.data.model.ThemePreference
import com.acagribahar.muscleandmindapp.data.remote.model.UserPreferences
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
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
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams



// Ayarlar Ekranı UI Durumu
data class SettingsUiState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val currentTheme: ThemePreference = ThemePreference.SYSTEM,
    val notificationsEnabled: Boolean = SettingsManager.DEFAULT_NOTIFICATIONS_ENABLED, // <<< Bildirim durumu eklendi
    val errorMessage: String? = null,
    val isLoadingBilling: Boolean = true, // Billing client hazır mı?
    val premiumProductDetails: ProductDetails? = null, // Ürün detayları
    val premiumPrice: String? = null, // Formatlanmış fiyat
    val billingError: String? = null, // Billing hatası (opsiyonel)
    // Diğer ayarlar buraya eklenebilir (örn: notificationHour)
)

class SettingsViewModel(
    private val taskRepository: TaskRepository,
    private val firebaseAuth: FirebaseAuth,
    private val settingsManager: SettingsManager,
    private val billingClientWrapper: BillingClientWrapper


) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }


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
        Log.d("SettingsViewModel", "init block started.")

        // <<< Başlangıçta mevcut temayı yükle >>>
        _uiState.update { it.copy(currentTheme = settingsManager.getThemePreference(),
            notificationsEnabled = settingsManager.getNotificationsEnabled()

        ) }

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


        // <<< YENİ: BillingClient durumunu ve ürün detaylarını dinle >>>
        viewModelScope.launch {
            // Önce Billing Client'ın hazır olmasını dinle
            billingClientWrapper.billingClientReady.collect { isReady ->
                Log.d(TAG, "Billing client ready state: $isReady")
                _uiState.update { it.copy(isLoadingBilling = !isReady) }
                // Hazır olduğunda ürün detaylarını dinlemeye başla (zaten init'te sorgulanıyor)
                if (isReady) {
                    // productDetails Flow'unu dinle
                    billingClientWrapper.productDetails.collect { productDetails ->
                        val price = productDetails?.subscriptionOfferDetails?.firstOrNull()
                            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                            ?.formattedPrice // Formatlanmış fiyatı al (örn: ₺19,99)

                        Log.d(TAG, "Product details collected: Price=$price, Details=${productDetails?.name}")
                        _uiState.update {
                            it.copy(
                                premiumProductDetails = productDetails,
                                premiumPrice = price,
                                isLoadingBilling = false // Detaylar (veya null) geldi, yükleme bitti
                            )
                        }
                    }
                } else {
                    // Bağlantı hazır değilse veya koptuysa state'i güncelle
                    _uiState.update { it.copy(isLoadingBilling = true, premiumProductDetails = null, premiumPrice = null) }
                }
            }
        } // <<< Billing dinleme sonu >>>
        // <<< YENİ: Başarılı ve Onaylanmış Satın Alma İşlemlerini Dinle >>>
        billingClientWrapper.onPurchaseAcknowledged = { purchase ->
            Log.d(TAG, "Purchase acknowledged in ViewModel, orderId: ${purchase.orderId}. Updating premium status.")
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                viewModelScope.launch {
                    try {
                        // Firestore'da premium durumunu güncelle
                        taskRepository.updateUserPremiumStatus(userId, true)
                        Log.d(TAG, "Firestore premium status updated to true for user: $userId")
                        // UI state'ini hemen güncelle (Firestore'dan okumayı beklemeden anlık yansıma için)
                        _uiState.update {
                            it.copy(
                                isPremium = true,
                                isLoading = false, // Firestore yüklemesi bitti
                                billingError = null // Varsa eski billing hatasını temizle
                            )
                        }
                        // Opsiyonel: Kullanıcıya Toast mesajı gösterilebilir
                        // Veya UI'da "Premium'a yükseltildi!" gibi bir mesaj gösterilebilir.
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating premium status in Firestore after purchase: ${e.message}", e)
                        _uiState.update { it.copy(billingError = "Premium durumu güncellenirken hata oluştu.") }
                    }
                }
            } else {
                Log.e(TAG, "Purchase acknowledged, but current user is null. Cannot update Firestore.")
                _uiState.update { it.copy(billingError = "Kullanıcı bulunamadığı için premium durumu güncellenemedi.") }
            }
        }

        // <<< YENİ: Satın Alma Hatalarını Dinle (Opsiyonel ama önerilir) >>>
        billingClientWrapper.onPurchaseError = { billingResult ->
            Log.e(TAG, "Purchase error reported to ViewModel: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
            _uiState.update { it.copy(billingError = "Satın alma sırasında bir hata oluştu: ${billingResult.debugMessage}") }
        }
    }

    // Artık userId parametresi alıyor
    // loadUserPreferences fonksiyonu aynı kalır
    private fun loadUserPreferences(userId: String) {
        Log.d("SettingsViewModel", "loadUserPremiumStatus called for userId: $userId") // <<< EKLE

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch { // Tekrar launch açmak daha güvenli olabilir
            var finalPrefs: UserPreferences? = null // prefs değişkenini try dışında tanımla

            try {
                Log.d("SettingsViewModel", "Calling taskRepository.getUserPreferences for $userId")

                var prefs = taskRepository.getUserPreferences(userId)

                Log.d("SettingsViewModel", "Prefs fetched for $userId: $prefs")

                finalPrefs = taskRepository.getUserPreferences(userId)


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

    // <<< Tema tercihini güncelleyen fonksiyon >>>
    fun updateThemePreference(newPreference: ThemePreference) {
        settingsManager.saveThemePreference(newPreference) // Kaydet
        _uiState.update { it.copy(currentTheme = newPreference) } // State'i güncelle
    }

    // <<< YENİ: Bildirim etkinleştirme durumunu güncelleyen fonksiyon >>>
    fun updateNotificationsEnabled(enabled: Boolean) {
        settingsManager.saveNotificationsEnabled(enabled) // Ayarı SharedPreferences'a kaydet
        _uiState.update { it.copy(notificationsEnabled = enabled) } // UI State'ini güncelle
        Log.d("SettingsViewModel", "Notification enabled status updated to: $enabled") // Loglama
    }

    fun launchBillingFlow(activity: Activity) {
        // <<< LOG: Fonksiyonun başladığını logla >>>
        Log.d(TAG, "launchBillingFlow function entered.")

        val productDetails = _uiState.value.premiumProductDetails
        if (productDetails == null) {
            // <<< LOG: Ürün detayları null ise logla >>>
            Log.e(TAG, "Product details IS NULL when launching billing flow.")
            _uiState.update { it.copy(billingError = "Premium ürünü detayları alınamadı.") }
            return
        }

        // <<< LOG: Kullanılacak ürün detaylarını logla >>>
        Log.d(TAG, "Attempting to launch billing flow with ProductDetails: ID=${productDetails.productId}, Name=${productDetails.name}, Type=${productDetails.productType}")

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            // <<< LOG: Offer token null ise logla >>>
            Log.e(TAG, "Offer token IS NULL for subscription product: ${productDetails.productId}")
            _uiState.update { it.copy(billingError = "Abonelik teklifi bulunamadı.") }
            return
        }

        // <<< LOG: Bulunan offer token'ı logla >>>
        Log.d(TAG, "Offer token found: $offerToken")

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        // <<< LOG: Wrapper'ı çağırmadan hemen önce logla >>>
        Log.d(TAG, "Calling billingClientWrapper.launchPurchaseFlow...")
        billingClientWrapper.launchPurchaseFlow(activity, billingFlowParams)
    }

    // <<< Billing hatasını temizlemek için >>>
    fun clearBillingError() {
        _uiState.update { it.copy(billingError = null) }
    }
}