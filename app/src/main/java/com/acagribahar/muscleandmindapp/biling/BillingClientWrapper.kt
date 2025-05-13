package com.acagribahar.muscleandmindapp.biling


import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.* // Billing kütüphanesi importları
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingClientWrapper(
    private val context: Context
) {

    companion object {
        private const val TAG = "BillingClientWrapper"
        const val PREMIUM_PRODUCT_ID = "premium_monthly" // Play Console'daki ID'nizle eşleşmeli
    }

    // ViewModel'ı bilgilendirmek için callback'ler
    var onPurchaseAcknowledged: ((Purchase) -> Unit)? = null
    var onPurchaseError: ((BillingResult) -> Unit)? = null

    // Wrapper için kendi CoroutineScope'u
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Satın alma güncellemelerini dinleyen listener
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d(TAG, "onPurchasesUpdated: Response Code: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Log.d(TAG, "Purchases not null and OK. Count: ${purchases.size}")
            for (purchase in purchases) {
                // handlePurchase kendi içinde coroutine başlatacak veya suspend olacak
                // Bu listener Main thread'de çağrıldığı için, handlePurchase içindeki
                // IO operasyonları (acknowledge) ayrı bir coroutine'e kaydırılmalı.
                // handlePurchase fonksiyonunu güncelledik.
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User cancelled the purchase flow.")
            onPurchaseError?.invoke(billingResult)
        } else {
            Log.e(TAG, "Purchase error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
            onPurchaseError?.invoke(billingResult)
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    private val _billingClientReady = MutableStateFlow(false)
    val billingClientReady: StateFlow<Boolean> = _billingClientReady.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    init {
        Log.d(TAG, "Initializing BillingClientWrapper and starting connection...")
        startConnection()
    }

    fun startConnection() {
        Log.d(TAG, "startConnection called. Current billingClient state: ${billingClient.connectionState}")
        if (billingClient.isReady) {
            Log.d(TAG, "BillingClient is already connected. Querying products and active subscriptions.")
            _billingClientReady.value = true // Zaten bağlıysa state'i güncelle
            queryProductDetails()
            coroutineScope.launch { queryActiveSubscriptions() }
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished: Response Code: ${billingResult.responseCode}, Debug Message: ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient setup finished successfully. isReady: ${billingClient.isReady}")
                    _billingClientReady.value = true
                    queryProductDetails()
                    // Aktif abonelikleri sorgulamayı başlat
                    coroutineScope.launch {
                        queryActiveSubscriptions()
                    }
                } else {
                    Log.e(TAG, "BillingClient setup FAILED. isReady: ${billingClient.isReady}")
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

    fun queryProductDetails() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryProductDetails: BillingClient not ready. Aborting query.")
            return
        }
        Log.d(TAG, "Querying product details for ID: $PREMIUM_PRODUCT_ID")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val foundProductDetails = productDetailsList?.find { it.productId == PREMIUM_PRODUCT_ID }
                if (foundProductDetails != null) {
                    _productDetails.value = foundProductDetails
                    Log.d(TAG, "Product details loaded: ${foundProductDetails.name} - ${foundProductDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice}")
                } else {
                    Log.w(TAG, "Product with ID $PREMIUM_PRODUCT_ID not found or list empty.")
                    _productDetails.value = null
                }
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
                _productDetails.value = null
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, params: BillingFlowParams) {
        if (!billingClient.isReady) {
            Log.e(TAG, "launchPurchaseFlow: BillingClient is not ready.")
            // Kullanıcıya hata mesajı gösterilebilir
            onPurchaseError?.invoke(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED).setDebugMessage("Billing client not ready.").build())
            return
        }
        val billingResult = billingClient.launchBillingFlow(activity, params)
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing flow launched successfully.")
        } else {
            Log.e(TAG, "Billing flow launch failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
            onPurchaseError?.invoke(billingResult)
        }
    }

    // Satın almaları işleyen fonksiyon
    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "Handling purchase: OrderID=${purchase.orderId}, Products=${purchase.products.joinToString()}, State=${purchase.purchaseState}, Acked=${purchase.isAcknowledged}")

        // Sadece bizim ürünümüzle ilgili olanları işle
        if (!purchase.products.contains(PREMIUM_PRODUCT_ID)) {
            Log.w(TAG, "Purchase product ID (${purchase.products.firstOrNull()}) does not match $PREMIUM_PRODUCT_ID. Skipping.")
            return
        }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                Log.d(TAG, "Purchase for ${purchase.orderId} is PURCHASED and NOT acknowledged. Initiating acknowledge.")
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                coroutineScope.launch { // Kendi scope'umuzda onaylama yapalım
                    val ackResult = billingClient.acknowledgePurchase(acknowledgePurchaseParams) // KTX suspend fonksiyonu
                    withContext(Dispatchers.Main) { // ViewModel callback'leri UI güncelleyebilir
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Purchase acknowledged successfully (via KTX suspend fun) for order: ${purchase.orderId}")
                            onPurchaseAcknowledged?.invoke(purchase)
                        } else {
                            Log.e(TAG, "Error acknowledging purchase (via KTX suspend fun) for ${purchase.orderId}: ${ackResult.debugMessage} (Code: ${ackResult.responseCode})")
                            onPurchaseError?.invoke(ackResult)
                        }
                    }
                }
            } else {
                Log.d(TAG, "Purchase for ${purchase.orderId} is ALREADY acknowledged.")
                onPurchaseAcknowledged?.invoke(purchase) // Zaten onaylıysa da ViewModel'ı bilgilendir
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase for ${purchase.orderId} is PENDING. User needs to complete payment.")
            // ViewModel'a bilgi verilebilir, UI'da "Bekleniyor" mesajı gösterilebilir.
            // Şimdilik sadece logluyoruz.
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            Log.d(TAG, "Purchase for ${purchase.orderId} is in UNSPECIFIED_STATE.")
        }
    }

    // Aktif abonelikleri sorgula ve gerekirse işle/onayla
    suspend fun queryActiveSubscriptions() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryActiveSubscriptions: BillingClient not ready.")
            return
        }
        Log.d(TAG, "Querying active subscriptions (SUBS type)...")
        val paramsSubs = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS) // Sadece abonelikleri sorgula
            .build()

        val purchasesResultSubs = billingClient.queryPurchasesAsync(paramsSubs) // KTX suspend fonksiyonu
        Log.d(TAG, "Subscriptions query finished with result code: ${purchasesResultSubs.billingResult.responseCode}, Debug: ${purchasesResultSubs.billingResult.debugMessage}")

        if (purchasesResultSubs.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (purchasesResultSubs.purchasesList.isNotEmpty()) {
                Log.d(TAG, "Active subscriptions found: ${purchasesResultSubs.purchasesList.size}")
                purchasesResultSubs.purchasesList.forEach { purchase ->
                    Log.d(TAG, "Found active/pending sub: Products=${purchase.products.joinToString()}, OrderID=${purchase.orderId}, State=${purchase.purchaseState}, Acked=${purchase.isAcknowledged}")
                    // Bulunan her aboneliği işle (onay durumu vs. kontrol edilecek)
                    withContext(Dispatchers.Main) { // handlePurchase UI thread'inden çağrılan listener'ları tetikleyebilir
                        handlePurchase(purchase)
                    }
                }
            } else {
                Log.d(TAG, "No active subscriptions found by queryActiveSubscriptions.")
            }
        } else {
            Log.e(TAG, "Error querying active subscriptions: ${purchasesResultSubs.billingResult.debugMessage}")
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            Log.d(TAG, "Ending BillingClient connection.")
            billingClient.endConnection()
            _billingClientReady.value = false
        }
        // <<< CoroutineScope'u daha basit bir şekilde iptal et >>>
        Log.d(TAG, "Cancelling BillingClientWrapper CoroutineScope...")
        coroutineScope.cancel()
        Log.d(TAG, "BillingClientWrapper CoroutineScope cancelled.")
    }
}