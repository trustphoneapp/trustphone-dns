package com.yashwanthsurabhi.shielddns.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BillingUiState(
    val isReady: Boolean = false,
    val proPrice: String? = null,
    val lastError: String? = null,
)

class BillingManager(
    context: Context,
    private val onProStatusChanged: suspend (Boolean) -> Unit,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private var proProduct: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    fun start() {
        if (billingClient.isReady) {
            queryProducts()
            restorePurchases()
            return
        }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        _uiState.value = _uiState.value.copy(isReady = true, lastError = null)
                        queryProducts()
                        restorePurchases()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isReady = false,
                            lastError = result.debugMessage.ifBlank { "Billing unavailable" },
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _uiState.value = _uiState.value.copy(isReady = false)
                }
            },
        )
    }

    fun launchProPurchase(activity: Activity): Boolean {
        val details = proProduct ?: run {
            start()
            return false
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                _uiState.value = _uiState.value.copy(
                    lastError = result.debugMessage.ifBlank { "Purchase failed" },
                )
            }
        }
    }

    private fun queryProducts() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_PRO_LIFETIME)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _uiState.value = _uiState.value.copy(lastError = result.debugMessage)
                return@queryProductDetailsAsync
            }
            val details = productDetailsResult.productDetailsList.firstOrNull()
            proProduct = details
            _uiState.value = _uiState.value.copy(
                proPrice = details?.oneTimePurchaseOfferDetails?.formattedPrice,
                lastError = null,
            )
        }
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val hasPro = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                (purchase.products.contains(PRODUCT_PRO_LIFETIME) ||
                 purchase.products.contains(PRODUCT_PRO_MONTHLY) ||
                 purchase.products.contains(PRODUCT_PRO_YEARLY))
        }

        if (hasPro) {
            purchases
                .filter { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        (purchase.products.contains(PRODUCT_PRO_LIFETIME) ||
                         purchase.products.contains(PRODUCT_PRO_MONTHLY) ||
                         purchase.products.contains(PRODUCT_PRO_YEARLY))
                }
                .forEach { purchase ->
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(params) { result ->
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                grantPro()
                            }
                        }
                    } else {
                        grantPro()
                    }
                }
        } else {
            revokePro()
        }
    }

    private fun grantPro() {
        scope.launch {
            _isPro.value = true
            onProStatusChanged(true)
        }
    }

    private fun revokePro() {
        scope.launch {
            _isPro.value = false
            onProStatusChanged(false)
        }
    }

    companion object {
        const val PRODUCT_PRO_LIFETIME = "shield_dns_pro_lifetime"
        const val PRODUCT_PRO_MONTHLY = "shield_dns_pro_monthly"
        const val PRODUCT_PRO_YEARLY = "shield_dns_pro_yearly"
    }
}
