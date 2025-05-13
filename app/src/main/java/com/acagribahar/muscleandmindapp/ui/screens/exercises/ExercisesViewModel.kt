package com.acagribahar.muscleandmindapp.ui.screens.exercises

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acagribahar.muscleandmindapp.data.model.DefaultTaskDto
import com.acagribahar.muscleandmindapp.data.remote.model.UserExercise
import com.acagribahar.muscleandmindapp.data.repository.TaskRepository
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

// Grup yapısını temsil etmek için
data class ExerciseGroup(
    val category: String,
    val exercises: List<DisplayExercise>
)

class ExercisesViewModel(
    private val taskRepository: TaskRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val TAG = "ExercisesViewModel"
        // <<< Geçiş Reklamı Test Birimi Kimliği >>>
        //private const val INTERSTITIAL_TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1292674096792479/9324300564"

        // <<< Gerçek ID'nizi buraya ekleyin (Yayınlamadan önce) >>>
        // private const val INTERSTITIAL_PROD_AD_UNIT_ID = "ca-app-pub-1292674096792479/9324300564"
    }

    // Gruplanmış egzersiz listesini tutacak StateFlow
    private val _groupedExercises = MutableStateFlow<List<ExerciseGroup>>(emptyList())
    val groupedExercises: StateFlow<List<ExerciseGroup>> = _groupedExercises.asStateFlow()

    // Auth durumunu dinleyen Flow (callbackFlow ile)
    private val userStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    // <<< YENİ: Premium Durumu StateFlow'u >>>
    private val _isPremium = MutableStateFlow(false) // Başlangıçta false
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    // <<< YENİ: Yüklenmiş Geçiş Reklamını tutan StateFlow >>>
    private val _interstitialAd = MutableStateFlow<InterstitialAd?>(null)
    val interstitialAd: StateFlow<InterstitialAd?> = _interstitialAd.asStateFlow()


    init {
        // Kullanıcı durumu değiştikçe egzersizleri ve premium durumunu yükle
        viewModelScope.launch {
            userStateFlow.collect { firebaseUser ->
                val userId = firebaseUser?.uid
                // Premium durumunu yükle/güncelle
                loadUserPremiumStatus(userId)
                // Egzersizleri yükle/birleştir (bu zaten flatMapLatest ile tetiklenmeli, tekrar kontrol edelim)
                // loadAndCombineExercises(userId) // Bu flatMapLatest içinde zaten var, tekrar çağırmaya gerek yok
                Log.d(TAG, "User state changed: $userId. Premium status loading initiated. Exercise flow will update.")
            }
        }

        // <<< flatMapLatest ile egzersizleri ve premium durumunu birleştirelim (daha temiz) >>>
        // Bu, groupedExercises'i doğrudan oluşturur ve kullanıcı değişimine tepki verir.
        viewModelScope.launch {
            userStateFlow.flatMapLatest { firebaseUser ->
                val userId = firebaseUser?.uid
                Log.d(TAG, "User state changed for exercise loading: $userId")
                loadAndCombineExercises(userId) // Bu Flow<List<ExerciseGroup>> döndürür
            }.collect { combinedGroupedList ->
                _groupedExercises.value = combinedGroupedList
                Log.d(TAG, "Updated grouped exercises state via flatMapLatest.")
            }
        }

    } // init sonu

    // <<< YENİ: Premium durumunu yükleyen fonksiyon >>>
    private fun loadUserPremiumStatus(userId: String?) {
        if (userId == null) {
            _isPremium.value = false
            return
        }
        viewModelScope.launch {
            try {
                val prefs = taskRepository.getUserPreferences(userId)
                _isPremium.value = prefs?.isPremium ?: false
                Log.d(TAG, "Premium status loaded for $userId: ${isPremium.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading premium status for $userId", e)
                _isPremium.value = false
            }
        }
    }

    // <<< YENİ: Geçiş reklamını yükleyen fonksiyon >>>
    fun loadInterstitialAd(context: Context) {
        // Zaten yüklü bir reklam varsa veya yükleniyorsa tekrar yükleme (opsiyonel)
        if (_interstitialAd.value != null){
            Log.d(TAG, "Interstitial Ad already loaded or is loading.")
            return
        }

        Log.d(TAG, "Attempting to load Interstitial Ad with ID: $INTERSTITIAL_AD_UNIT_ID") // <<< GERÇEK ID KULLANILIYOR OLMALI
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, // <<< TEST ID'si KULLAN
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial Ad FAILED to load. Code: ${adError.code}, Message: ${adError.message}, Domain: ${adError.domain}, Cause: ${adError.cause}")
                    _interstitialAd.value = null // Hata durumunda null yap
                }

                override fun onAdLoaded(loadedAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad loaded successfully.")
                    _interstitialAd.value = loadedAd // Yüklenen reklamı state'e ata
                }
            })
    }

    // <<< YENİ: Reklam gösterildikten sonra state'i temizleyen fonksiyon >>>
    fun interstitialAdShown() {
        _interstitialAd.value = null
        Log.d(TAG, "Interstitial Ad state cleared.")
        // İsteğe bağlı: Bir sonraki reklamı hemen yüklemeye başlayabiliriz
        // loadInterstitialAd(applicationContext) // Application context lazım olurdu
    }

    // Hem varsayılan hem de özel egzersizleri alıp birleştiren Flow döndüren fonksiyon
    private fun loadAndCombineExercises(userId: String?): Flow<List<ExerciseGroup>> {
        // Varsayılan egzersizleri bir kere yükle (Flow değil, direkt liste)
        val defaultExercisesFlow: Flow<List<DefaultTaskDto>> = flow {
            emit(taskRepository.loadDefaultTasks())
        }

        // Özel egzersizleri dinleyen Flow (kullanıcı varsa)
        val customExercisesFlow: Flow<List<UserExercise>> = userId?.let {
            taskRepository.getCustomExercises(it)
        } ?: flowOf(emptyList()) // Kullanıcı yoksa boş liste Flow'u

        // İki Flow'u birleştir
        return combine(defaultExercisesFlow, customExercisesFlow) { defaultTasks, customTasks ->
            Log.d("ExercisesVM", "Combining: ${defaultTasks.size} default, ${customTasks.size} custom")
            // Varsayılanları DisplayExercise'e map et
            val defaultDisplay = defaultTasks
                .filter { it.type == "body" } // Sadece body olanları al
                .map { dto ->
                    DisplayExercise(
                        id = "default_${dto.title}", // Basit bir ID oluşturalım
                        title = dto.title,
                        description = dto.description,
                        category = dto.category,
                        isCustom = false
                    )
                }
            // Özelleri DisplayExercise'e map et
            val customDisplay = customTasks.map { userEx ->
                DisplayExercise(
                    id = userEx.id, // Firestore ID'si
                    title = userEx.title,
                    description = userEx.description,
                    category = userEx.category,
                    isCustom = true
                )
            }

            // İki listeyi birleştir, kategoriye göre grupla, ExerciseGroup listesi oluştur
            (defaultDisplay + customDisplay)
                .groupBy { it.category ?: "Diğer" } // Kategorisi olmayanları "Diğer" grubuna ata
                .map { (category, exercises) ->
                    ExerciseGroup(
                        category = category,
                        // Egzersizleri başlığa göre sırala (isteğe bağlı)
                        exercises = exercises.sortedBy { it.title }
                    )
                }
                // Grupları kategori adına göre sırala (isteğe bağlı)
                .sortedBy { it.category }
        }
    }

    /*
    private fun loadAndGroupExercises() {
        viewModelScope.launch {
            val allDefaultTasks = taskRepository.loadDefaultTasks()
            val bodyExercises = allDefaultTasks.filter { it.type == "body" && !it.category.isNullOrBlank() }

            // Kategoriye göre grupla ve ExerciseGroup listesi oluştur
            val grouped = bodyExercises
                .groupBy { it.category!! } // Null olmayan kategoriye göre grupla
                .map { (category, exercises) -> // Her grup için ExerciseGroup oluştur
                    ExerciseGroup(category = category, exercises = exercises)
                }
                .sortedBy { it.category } // Kategoriye göre sırala (isteğe bağlı)

            _groupedExercises.value = grouped // StateFlow'u güncelle
        }
    }

     */

    // Başlığa göre egzersizi bulan fonksiyon
    /*
    fun getExerciseByTitle(title: String): DefaultTaskDto? {
        return groupedExercises.value // StateFlow'un güncel değerini al
            .flatMap { it.exercises } // Tüm gruplardaki egzersizleri tek bir listeye düzleştir
            .find { it.title == title } // Başlığa göre eşleşeni bul
    }

     */
    // ID'ye göre DisplayExercise bulan fonksiyon
    fun getDisplayExerciseById(id: String): DisplayExercise? {
        // _groupedExercises yerine groupedExercises (StateFlow) kullanmak daha doğru
        // .value ile güncel listeyi al
        return groupedExercises.value
            .flatMap { exerciseGroup -> exerciseGroup.exercises } // Tüm egzersizleri tek listeye çevir
            .find { displayExercise -> displayExercise.id == id } // ID'ye göre bul
    }
}