package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.*
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
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

    private val _subscriptionProductId = MutableStateFlow<String?>(null)
    val subscriptionProductId: StateFlow<String?> = _subscriptionProductId.asStateFlow()

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
        _subscriptionProductId.value = sharedPrefs.getString("subscription_product_id", null)

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
                    inMockMode = BuildConfig.DEBUG
                    onComplete(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing Client disconnected. Falling back to Mock Play Store mode.")
                inMockMode = BuildConfig.DEBUG
            }
        })
    }

    private val _lastQueryError = MutableStateFlow<String?>(null)
    val lastQueryError: StateFlow<String?> = _lastQueryError.asStateFlow()

    // Full reconciliation pass (app start, app resume, explicit "Restore Purchases").
    // This is the only path allowed to revoke premium, since it reflects the
    // complete, authoritative state of the user's active subscriptions.
    fun queryPurchases() {
        if (inMockMode) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _lastQueryError.value = null
                processPurchases(purchasesList, allowRevoke = true)
            } else {
                Log.e("BillingManager", "Query purchases failed: ${billingResult.debugMessage}")
                _lastQueryError.value = billingResult.debugMessage
            }
        }
    }

    // Fired after launchBillingFlow completes, or when Play pushes an incremental
    // purchase-state change while the app is open. Never treated as authoritative
    // for revocation — an incomplete delivery here must not strip an existing
    // subscriber's premium. Only queryPurchases() (above) can revoke.
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases, allowRevoke = false)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i("BillingManager", "User cancelled Play Store purchase.")
        } else {
            Log.e("BillingManager", "Purchase failed: ${billingResult.debugMessage}")
            _lastQueryError.value = billingResult.debugMessage
        }
    }

    private fun processPurchases(purchases: List<Purchase>, allowRevoke: Boolean) {
        var isPro = false
        var hasPending = false
        var tier: String? = null
        var txId: String? = null
        var billingDate: String? = null
        var productId: String? = null

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
                productId = product
                tier = if (product == PRODUCT_YEARLY) "FloraFlow PRO Annual" else "FloraFlow PRO Monthly"
                txId = purchase.orderId ?: "—"

                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                val cal = Calendar.getInstance()
                // For a 3-day free trial, billing starts in 3 days
                cal.add(Calendar.DAY_OF_YEAR, 3)
                billingDate = sdf.format(cal.time)
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                hasPending = true
                Log.i("BillingManager", "Purchase is pending. Waiting for completion.")
            }
        }

        if (isPro) {
            _isPremium.value = true
            _subscriptionTier.value = tier
            _subscriptionTransactionId.value = txId
            _subscriptionBillingDate.value = billingDate
            _subscriptionProductId.value = productId

            sharedPrefs.edit().apply {
                putBoolean("is_premium", true)
                putString("subscription_tier", tier)
                putString("subscription_transaction_id", txId)
                putString("subscription_billing_date", billingDate)
                putString("subscription_product_id", productId)
                putBoolean("purchased_historically", true)
                apply()
            }
        } else if (allowRevoke && !hasPending) {
            // Only a full reconciliation pass with zero active or pending
            // subscriptions is allowed to strip premium.
            _isPremium.value = false
            _subscriptionTier.value = null
            _subscriptionTransactionId.value = null
            _subscriptionBillingDate.value = null
            _subscriptionProductId.value = null

            sharedPrefs.edit().apply {
                putBoolean("is_premium", false)
                putString("subscription_tier", null)
                putString("subscription_transaction_id", null)
                putString("subscription_billing_date", null)
                putString("subscription_product_id", null)
                apply()
            }
        }
    }

    // Real, localized price + trial-length info for a subscription product, as
    // configured in Play Console — used so the paywall never promises a trial
    // or price that isn't actually backing the purchase.
    data class OfferInfo(
        val formattedPrice: String,
        val trialDays: Int?
    )

    // Queries live ProductDetails for both SKUs so the paywall can render real
    // prices and only advertise a free trial if one is actually configured.
    // Returns an empty map (never crashes the caller) if Play is unreachable
    // or in mock mode — the UI is expected to fall back to static copy.
    fun queryOfferDetails(productIds: List<String>, onResult: (Map<String, OfferInfo>) -> Unit) {
        if (inMockMode) {
            onResult(emptyMap())
            return
        }
        val productList = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val result = mutableMapOf<String, OfferInfo>()
                for (productDetails in productDetailsList) {
                    val offers = productDetails.subscriptionOfferDetails ?: continue
                    val trialPhase = offers.asSequence()
                        .flatMap { it.pricingPhases.pricingPhaseList.asSequence() }
                        .firstOrNull { it.priceAmountMicros == 0L }
                    val trialDays = trialPhase?.billingPeriod?.let { parseIso8601DurationDays(it) }

                    val paidPhase = offers.asSequence()
                        .flatMap { it.pricingPhases.pricingPhaseList.asSequence() }
                        .firstOrNull { it.priceAmountMicros > 0L }
                    val formattedPrice = paidPhase?.formattedPrice
                        ?: offers.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

                    if (formattedPrice != null) {
                        result[productDetails.productId] = OfferInfo(formattedPrice, trialDays)
                    }
                }
                onResult(result)
            } else {
                Log.e("BillingManager", "Failed to query offer details: ${billingResult.debugMessage}")
                onResult(emptyMap())
            }
        }
    }

    // Parses simple ISO-8601 durations like "P7D", "P2W", "P1M" as used by
    // Play's billingPeriod field. Returns null for forms this doesn't cover.
    private fun parseIso8601DurationDays(period: String): Int? {
        val match = Regex("^P(\\d+)([DWMY])$").find(period) ?: return null
        val amount = match.groupValues[1].toIntOrNull() ?: return null
        return when (match.groupValues[2]) {
            "D" -> amount
            "W" -> amount * 7
            "M" -> amount * 30
            "Y" -> amount * 365
            else -> null
        }
    }

    // hasFreeTrialOffer reports whether the selected product actually has a
    // zero-price introductory offer configured in Play Console, so the UI can
    // avoid promising a trial that doesn't exist.
    fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        onMockTrigger: (productId: String) -> Unit,
        hasFreeTrialOffer: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (BuildConfig.DEBUG && inMockMode) {
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

                // Find offer details that has a free trial pricing phase (price == 0), otherwise fallback to first
                val freeTrialOffer = productDetails.subscriptionOfferDetails?.firstOrNull { offer ->
                    offer.pricingPhases.pricingPhaseList.any { phase ->
                        phase.priceAmountMicros == 0L
                    }
                }
                val offerDetails = freeTrialOffer ?: productDetails.subscriptionOfferDetails?.firstOrNull()
                hasFreeTrialOffer(freeTrialOffer != null)

                val offerToken = offerDetails?.offerToken ?: ""

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                val launchResult = billingClient.launchBillingFlow(activity, billingFlowParams)
                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    onError(launchResult.debugMessage.ifBlank { "Couldn't open Google Play billing." })
                }
            } else {
                Log.e("BillingManager", "Failed to query product details: ${billingResult.debugMessage}.")
                if (BuildConfig.DEBUG) {
                    Log.w("BillingManager", "Falling back to mock flow in debug build.")
                    onMockTrigger(productId)
                } else {
                    onError("Couldn't reach Google Play. Please check your connection and try again.")
                }
            }
        }
    }

    fun executeMockPurchase(productId: String) {
        if (!BuildConfig.DEBUG) return
        val tier = if (productId == PRODUCT_YEARLY) "FloraFlow PRO Annual" else "FloraFlow PRO Monthly"
        val txId = "GPA." + (SecureRandom().nextInt(9000) + 1000).toString() + "-" +
                (SecureRandom().nextInt(9000) + 1000).toString() + "-" +
                (SecureRandom().nextInt(9000) + 1000).toString() + "-" +
                (SecureRandom().nextInt(90000) + 10000).toString()

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val cal = Calendar.getInstance()
        // For a 3-day free trial, billing starts in 3 days
        cal.add(Calendar.DAY_OF_YEAR, 3)
        val billingDate = sdf.format(cal.time)

        _isPremium.value = true
        _subscriptionTier.value = tier
        _subscriptionTransactionId.value = txId
        _subscriptionBillingDate.value = billingDate
        _subscriptionProductId.value = productId

        sharedPrefs.edit().apply {
            putBoolean("is_premium", true)
            putString("subscription_tier", tier)
            putString("subscription_transaction_id", txId)
            putString("subscription_billing_date", billingDate)
            putString("subscription_product_id", productId)
            putBoolean("purchased_historically", true)
            apply()
        }
    }

    fun cancelMockSubscription() {
        if (!BuildConfig.DEBUG) return
        _isPremium.value = false
        _subscriptionTier.value = null
        _subscriptionTransactionId.value = null
        _subscriptionBillingDate.value = null
        _subscriptionProductId.value = null

        sharedPrefs.edit().apply {
            putBoolean("is_premium", false)
            putString("subscription_tier", null)
            putString("subscription_transaction_id", null)
            putString("subscription_billing_date", null)
            putString("subscription_product_id", null)
            apply()
        }
    }

    // Deep-links to Google Play's subscription management page for this
    // specific SKU. Play is the source of truth for cancellation — the app
    // cannot cancel a real subscription locally.
    fun buildManageSubscriptionUri(): android.net.Uri {
        val packageName = context.packageName
        val sku = _subscriptionProductId.value
        return if (sku != null) {
            android.net.Uri.parse("https://play.google.com/store/account/subscriptions?sku=$sku&package=$packageName")
        } else {
            android.net.Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
        }
    }
}

