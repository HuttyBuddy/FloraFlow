package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "floraflow_premium_monthly"
        const val PRODUCT_YEARLY = "floraflow_premium_yearly"
    }

    private val sharedPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _subscriptionTier = MutableStateFlow<String?>(null)
    val subscriptionTier: StateFlow<String?> = _subscriptionTier.asStateFlow()

    private val _subscriptionTransactionId = MutableStateFlow<String?>(null)
    val subscriptionTransactionId: StateFlow<String?> = _subscriptionTransactionId.asStateFlow()

    private val _subscriptionBillingDate = MutableStateFlow<String?>(null)
    val subscriptionBillingDate: StateFlow<String?> = _subscriptionBillingDate.asStateFlow()

    // Mode state: true if Play Store connection is unavailable (fallback for tests/emulators)
    var inMockMode by mutableStateOf(false)
        private set

    private lateinit var billingClient: BillingClient

    init {
        // Load initial state from cache
        val savedPremium = sharedPrefs.getBoolean("is_premium", false)
        _isPremium.value = savedPremium
        _subscriptionTier.value = sharedPrefs.getString("subscription_tier", null)
        _subscriptionTransactionId.value = sharedPrefs.getString("subscription_transaction_id", null)
        _subscriptionBillingDate.value = sharedPrefs.getString("subscription_billing_date", null)

        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    fun startConnection(onComplete: (Boolean) -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i("BillingManager", "Billing Client connected successfully.")
                    inMockMode = false
                    queryPurchases()
                    onComplete(true)
                } else {
                    Log.w("BillingManager", "Billing Client connection failed: ${billingResult.debugMessage}. Falling back to Mock Play Store mode.")
                    inMockMode = true
                    onComplete(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing Client disconnected. Falling back to Mock Play Store mode.")
                inMockMode = true
            }
        })
    }

    fun queryPurchases() {
        if (inMockMode) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchasesList)
            } else {
                Log.e("BillingManager", "Query purchases failed: ${billingResult.debugMessage}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i("BillingManager", "User cancelled Play Store purchase.")
        } else {
            Log.e("BillingManager", "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        var isPro = false
        var tier: String? = null
        var txId: String? = null
        var billingDate: String? = null

        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.i("BillingManager", "Purchase acknowledged.")
                        }
                    }
                }
                isPro = true
                val product = purchase.products.firstOrNull()
                tier = if (product == PRODUCT_YEARLY) "FloraFlow PRO Annual" else "FloraFlow PRO Monthly"
                txId = purchase.orderId ?: ("GPA." + (1000..9999).random().toString() + "-" + (1000..9999).random().toString() + "-MOCK")

                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                val cal = Calendar.getInstance()
                if (product == PRODUCT_YEARLY) {
                    cal.add(Calendar.YEAR, 1)
                } else {
                    cal.add(Calendar.MONTH, 1)
                }
                billingDate = sdf.format(cal.time)
            }
        }

        if (isPro) {
            _isPremium.value = true
            _subscriptionTier.value = tier
            _subscriptionTransactionId.value = txId
            _subscriptionBillingDate.value = billingDate

            sharedPrefs.edit().apply {
                putBoolean("is_premium", true)
                putString("subscription_tier", tier)
                putString("subscription_transaction_id", txId)
                putString("subscription_billing_date", billingDate)
                putBoolean("purchased_historically", true)
                apply()
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String, onMockTrigger: (productId: String) -> Unit) {
        if (inMockMode) {
            onMockTrigger(productId)
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                Log.e("BillingManager", "Failed to query product details: ${billingResult.debugMessage}. Falling back to mock flow.")
                onMockTrigger(productId)
            }
        }
    }

    fun executeMockPurchase(productId: String) {
        val tier = if (productId == PRODUCT_YEARLY) "FloraFlow PRO Annual" else "FloraFlow PRO Monthly"
        val txId = "GPA." + (1000..9999).random().toString() + "-" +
                (1000..9999).random().toString() + "-" +
                (1000..9999).random().toString() + "-" +
                (10000..99999).random().toString()

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val cal = Calendar.getInstance()
        if (productId == PRODUCT_YEARLY) {
            cal.add(Calendar.YEAR, 1)
        } else {
            cal.add(Calendar.MONTH, 1)
        }
        val billingDate = sdf.format(cal.time)

        _isPremium.value = true
        _subscriptionTier.value = tier
        _subscriptionTransactionId.value = txId
        _subscriptionBillingDate.value = billingDate

        sharedPrefs.edit().apply {
            putBoolean("is_premium", true)
            putString("subscription_tier", tier)
            putString("subscription_transaction_id", txId)
            putString("subscription_billing_date", billingDate)
            putBoolean("purchased_historically", true)
            apply()
        }
    }

    fun cancelMockSubscription() {
        _isPremium.value = false
        _subscriptionTier.value = null
        _subscriptionTransactionId.value = null
        _subscriptionBillingDate.value = null

        sharedPrefs.edit().apply {
            putBoolean("is_premium", false)
            putString("subscription_tier", null)
            putString("subscription_transaction_id", null)
            putString("subscription_billing_date", null)
            apply()
        }
    }
}
