package com.acagribahar.muscleandmindapp.biling

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.android.billingclient.api.* // Billing kütüphanesi importları
import com.android.billingclient.api.ProductDetails


// Google Play Billing işlemlerini yönetmek için yardımcı sınıf
class BillingClientWrapper(
    private val context: Context // Application context'i almak iyi olur
) {

    companion object {
        private const val TAG = "BillingClientWrapper"
        // Play Console'da tanımladığınız Ürün ID'si (sonra kullanılacak)
        const val PREMIUM_PRODUCT_ID = "premium_monthly" // Veya sizin ID'niz
    }
    // <<< ViewModel'ı satın alma onayı hakkında bilgilendirmek için callback >>>
    var onPurchaseAcknowledged: ((Purchase) -> Unit)? = null
    // <<< ViewModel'ı satın alma hatası hakkında bilgilendirmek için callback (opsiyonel) >>>
    var onPurchaseError: ((BillingResult) -> Unit)? = null

    // Satın alma güncellemelerini dinleyen listener
    // Bu listener, satın alma akışı bittiğinde veya bekleyen satın almalar olduğunda tetiklenir
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d(TAG, "onPurchasesUpdated: Response Code: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Log.d(TAG, "Purchases not null and OK. Count: ${purchases.size}")
            // Satın almaları işle (sonraki adımlarda implemente edilecek)
            for (purchase in purchases) {
                //Her bir satın almayı işle.
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User cancelled the purchase flow.")
            onPurchaseError?.invoke(billingResult) // ViewModel'ı bilgilendir

        } else {
            // Diğer hatalar
            Log.e(TAG, "Purchase error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
            onPurchaseError?.invoke(billingResult) // ViewModel'ı bilgilendir

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

    // <<< EKLENECEK STATEFLOW TANIMLARI >>>
    // Sorgulanan ürün detaylarını tutacak private MutableStateFlow
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    // Dışarıya sunulacak public, sadece okunabilir StateFlow
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()
    // <<< --- --- >>>

    // Sınıf örneği oluşturulduğunda bağlantıyı başlat
    init {
        Log.d(TAG, "Initializing BillingClientWrapper and starting connection...")
        startConnection()
    }

    // Google service Bağlantı kurma işlemi (GÜNCELLENDİ: queryProductDetails çağrısı eklendi)
    fun startConnection() {
        Log.d(TAG, "startConnection called. Current billingClient state: ${billingClient.connectionState}")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished: Response Code: ${billingResult.responseCode}, Debug Message: ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient setup finished successfully. isReady: ${billingClient.isReady}") // <<< EKLE
                    _billingClientReady.value = true
                    queryProductDetails() // Bağlantı sonrası sorgula
                } else {
                    Log.e(TAG, "BillingClient setup FAILED. isReady: ${billingClient.isReady}") // <<< EKLE
                    _billingClientReady.value = false
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.e(TAG, "!!!!!!!! BILLING SERVICE DISCONNECTED !!!!!!!!")
                _billingClientReady.value = false
                // startConnection() // Otomatik tekrar bağlanmayı deneyebilir
            }
        })
    }

    // --- Gelecek Adımlar İçin Placeholder Fonksiyonlar ---

    // Ürün detaylarını sorgulayacak fonksiyon (GÜNCELLENDİ: İçi dolduruldu)
    fun queryProductDetails() {
        Log.d(TAG, "queryProductDetails called. billingClient.isReady: ${billingClient.isReady}, connectionState: ${billingClient.connectionState}")
        if (!billingClient.isReady) {
            Log.e(TAG, "queryProductDetails: BillingClient not ready. Aborting query.")
            return
        }

        Log.d(TAG, "Querying product details for ID: $PREMIUM_PRODUCT_ID")

        // Sorgulanacak ürün listesini oluştur (Sadece bir ürünümüz var)
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS) // Abonelik olduğu için SUBS
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        // Asenkron olarak ürün detaylarını sorgula
        billingClient.queryProductDetailsAsync(params,
            ProductDetailsResponseListener { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (productDetailsList.isNullOrEmpty()) {
                        Log.w(TAG, "Product details list is null or empty. Product ID might be incorrect or not configured in Play Console.")
                        _productDetails.value = null
                    } else {
                        // Bizim ürünümüzü listeden bul (genelde tek elemanlı olur)
                        val foundProductDetails = productDetailsList.find { it.productId == PREMIUM_PRODUCT_ID }
                        if (foundProductDetails != null) {
                            _productDetails.value = foundProductDetails
                            Log.d(TAG, "Product details loaded successfully: ${foundProductDetails.name} - ${foundProductDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice}")
                        } else {
                            Log.w(TAG, "Product with ID $PREMIUM_PRODUCT_ID not found in the returned list.")
                            _productDetails.value = null
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
                    _productDetails.value = null
                }
            })
    }

    // <<< SATIN ALMA AKIŞINI BAŞLATAN FONKSİYONUN DOĞRU İMZASI VE İÇERİĞİ >>>
    fun launchPurchaseFlow(activity: Activity, params: BillingFlowParams) {
        Log.d(TAG, "launchPurchaseFlow called. billingClient.isReady: ${billingClient.isReady}, connectionState: ${billingClient.connectionState}")
        if (!billingClient.isReady) {
            Log.e(TAG, "launchPurchaseFlow: BillingClient is not ready.")
            return
        }
        // <<< Satın alma akışını başlat (ViewModel'dan gelen params kullanılır) >>>
        val billingResult = billingClient.launchBillingFlow(activity, params)

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing flow launched successfully.")
        } else {
            Log.e(TAG, "Billing flow launch failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
            // TODO: Kullanıcıya hata mesajı gösterilebilir
        }
    } // <<< launchPurchaseFlow Sonu >>>

    // Satın almaları işleyecek fonksiyon
    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "handlePurchase called for purchase: ${purchase.orderId} (not implemented yet)")
        // Sadece bizim ürünümüzle ilgili ve durumu PURCHASED olanları işle
        if (!purchase.products.contains(PREMIUM_PRODUCT_ID)) {
            Log.w(TAG, "Purchase product ID (${purchase.products.firstOrNull()}) does not match expected $PREMIUM_PRODUCT_ID. Skipping.")
            return
        }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                // Satın alma onaylanmamış, onaylama işlemini başlat
                Log.d(TAG, "Purchase for ${purchase.orderId} is PURCHASED and NOT acknowledged. Acknowledging...")
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackBillingResult ->
                    if (ackBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully for order: ${purchase.orderId}")
                        // <<< ViewModel'ı başarılı onaylama hakkında bilgilendir >>>
                        onPurchaseAcknowledged?.invoke(purchase)
                    } else {
                        Log.e(TAG, "Error acknowledging purchase for ${purchase.orderId}: ${ackBillingResult.debugMessage} (Code: ${ackBillingResult.responseCode})")
                        onPurchaseError?.invoke(ackBillingResult) // ViewModel'ı bilgilendir
                    }
                }
            } else {
                // Satın alma zaten onaylanmış (örneğin yinelenen abonelik veya geri yüklenen satın alma)
                Log.d(TAG, "Purchase for ${purchase.orderId} is ALREADY acknowledged.")
                // Bu durumda da kullanıcı ürüne sahip olduğu için ViewModel'ı bilgilendirebiliriz.
                onPurchaseAcknowledged?.invoke(purchase)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase for ${purchase.orderId} is PENDING. Handle pending state if necessary.")
            // TODO: Kullanıcıya "Satın alma bekleniyor..." gibi bir mesaj gösterilebilir.
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            Log.d(TAG, "Purchase for ${purchase.orderId} is in UNSPECIFIED_STATE.")
        }

        // acknowledgePurchase(purchase.purchaseToken)
    }

    // Satın almayı onaylayacak fonksiyon şu an gerek yok handlePurchase içerisinde yapılıyor o yüzden private olarak güncellendi.
    private suspend fun acknowledgePurchase(purchaseToken: String) {
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