package com.acagribahar.muscleandmindapp.biling

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.android.billingclient.api.* // Billing kütüphanesi importları

// Google Play Billing işlemlerini yönetmek için yardımcı sınıf
class BillingClientWrapper(
    private val context: Context // Application context'i almak iyi olur
) {

    companion object {
        private const val TAG = "BillingClientWrapper"
        // Play Console'da tanımladığınız Ürün ID'si (sonra kullanılacak)
        const val PREMIUM_PRODUCT_ID = "premium_monthly" // Veya sizin ID'niz
    }

    // Satın alma güncellemelerini dinleyen listener
    // Bu listener, satın alma akışı bittiğinde veya bekleyen satın almalar olduğunda tetiklenir
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Log.d(TAG, "Purchase(s) successful, processing...")
            // Satın almaları işle (sonraki adımlarda implemente edilecek)
            for (purchase in purchases) {
                // handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User cancelled the purchase flow.")
            // Kullanıcı iptal etti, özel bir işlem gerekmez
        } else {
            // Diğer hatalar
            Log.e(TAG, "Purchase error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
        }
    }

    // BillingClient: Google Play ile ana iletişim arayüzü
    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener) // Satın alma listener'ını ayarla
        .enablePendingPurchases() // Bekleyen satın almaları etkinleştir (zorunlu)
        .build()

    // BillingClient'ın Google Play servisine bağlı olup olmadığını tutan state
    private val _billingClientReady = MutableStateFlow(false)
    val billingClientReady: StateFlow<Boolean> = _billingClientReady.asStateFlow()

    // Sınıf örneği oluşturulduğunda bağlantıyı başlat
    init {
        Log.d(TAG, "Initializing BillingClientWrapper and starting connection...")
        startConnection()
    }

    // Google Play servisine bağlantı kurma işlemi
    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            // Kurulum tamamlandığında çağrılır
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient setup finished successfully.")
                    _billingClientReady.value = true // Bağlantı hazır state'ini güncelle
                    // Bağlantı başarılı, ürünleri ve mevcut satın almaları sorgulayabiliriz
                    // queryProductDetails() // Sonraki adım
                    // queryActiveSubscriptions() // Sonraki adım
                } else {
                    Log.e(TAG, "BillingClient setup failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
                    _billingClientReady.value = false
                }
            }

            // Servis bağlantısı kesildiğinde çağrılır
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "BillingClient service disconnected. Retrying connection...")
                _billingClientReady.value = false
                // Bağlantı koptuğunda tekrar bağlanmayı deneyebiliriz (opsiyonel)
                // startConnection()
            }
        })
    }

    // --- Gelecek Adımlar İçin Placeholder Fonksiyonlar ---

    // Ürün detaylarını sorgulayacak fonksiyon
    fun queryProductDetails() {
        Log.d(TAG, "queryProductDetails called (not implemented yet)")
        // TODO: queryProductDetailsAsync çağrısı yapılacak
    }

    // Satın alma akışını başlatacak fonksiyon
    fun launchPurchaseFlow(activity: Activity, params: BillingFlowParams) {
        Log.d(TAG, "launchPurchaseFlow called (not implemented yet)")
        // TODO: billingClient.launchBillingFlow çağrısı yapılacak
    }

    // Satın almaları işleyecek fonksiyon
    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "handlePurchase called for purchase: ${purchase.orderId} (not implemented yet)")
        // TODO: Satın alma doğrulama ve onaylama işlemleri
        // acknowledgePurchase(purchase.purchaseToken)
    }

    // Satın almayı onaylayacak fonksiyon
    suspend fun acknowledgePurchase(purchaseToken: String) {
        Log.d(TAG, "acknowledgePurchase called for token: $purchaseToken (not implemented yet)")
        // TODO: billingClient.acknowledgePurchase çağrısı yapılacak
    }

    // Aktif abonelikleri/satın almaları sorgulayacak fonksiyon
    suspend fun queryActiveSubscriptions(): List<Purchase> {
        Log.d(TAG, "queryActiveSubscriptions called (not implemented yet)")
        // TODO: billingClient.queryPurchasesAsync çağrısı yapılacak
        return emptyList()
    }

    // Activity/ViewModel yok olurken bağlantıyı kesmek için fonksiyon
    fun endConnection() {
        if (billingClient.isReady) {
            Log.d(TAG, "Ending BillingClient connection.")
            billingClient.endConnection()
            _billingClientReady.value = false
        }
    }
}