package tr.com.covolt.aramaengelleyici.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import tr.com.covolt.aramaengelleyici.data.SettingsStore

/**
 * Play Console'da tanımlanması gereken ürünler:
 *  - Abonelik [SUB_PRO], içinde iki temel plan: [PLAN_MONTHLY] (aylık) ve [PLAN_YEARLY] (yıllık).
 *    Yıllık plana isteğe bağlı 7 günlük ücretsiz deneme teklifi eklenebilir.
 *  - Tek seferlik ürün [INAPP_LIFETIME] (ömür boyu Pro).
 *
 * Satın alma bilgisi yalnızca Google Play'de ve cihazda tutulur; kendi sunucumuz yok.
 */
class BillingRepository(
    context: Context,
    private val settings: SettingsStore,
) : PurchasesUpdatedListener {

    /** Ekranda gösterilecek bir satın alma seçeneği. */
    data class Offer(
        val id: String,
        val title: String,
        val price: String,
        val note: String,
        /** Testlerde ve önizlemelerde null olabilir; gerçek satın alma için zorunlu. */
        val details: ProductDetails?,
        val offerToken: String?,
    )

    var onProChanged: ((Boolean) -> Unit)? = null
    var onOffersChanged: ((List<Offer>) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private var subscriptionOffers: List<Offer> = emptyList()
    private var lifetimeOffers: List<Offer> = emptyList()

    /** Kullanıcı "geri yükle"ye basmış ama bağlantı henüz kurulmamışsa true. */
    private var pendingUserRestore = false

    fun connect() {
        if (client.isReady) {
            refreshPurchases()
            if (subscriptionOffers.isEmpty() && lifetimeOffers.isEmpty()) loadOffers()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val userInitiated = pendingUserRestore
                pendingUserRestore = false
                if (result.responseCode == BillingResponseCode.OK) {
                    refreshPurchases(userInitiated)
                    loadOffers()
                } else if (userInitiated) {
                    onMessage?.invoke("Google Play'e bağlanılamadı. İnternet bağlantını kontrol edip tekrar dene.")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Bir sonraki connect() çağrısında yeniden bağlanılır (ekran her açıldığında).
            }
        })
    }

    fun endConnection() {
        if (client.isReady) client.endConnection()
    }

    /** Mevcut satın alımları Play'den okuyup Pro durumunu günceller ("Satın alımları geri yükle"). */
    fun refreshPurchases(userInitiated: Boolean = false) {
        if (!client.isReady) {
            if (userInitiated) pendingUserRestore = true
            connect()
            return
        }
        val types = listOf(ProductType.SUBS, ProductType.INAPP)
        var remaining = types.size
        var anyActive = false
        var anyFailed = false

        types.forEach { type ->
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            client.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingResponseCode.OK) {
                    purchases.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            anyActive = true
                            acknowledgeIfNeeded(purchase)
                        }
                    }
                } else {
                    anyFailed = true
                }
                remaining--
                if (remaining == 0) {
                    // Sorgu başarısızsa kayıtlı durumu bozma (ör. internet yokken Pro kapanmasın).
                    if (!anyFailed || anyActive) setPro(anyActive)
                    if (userInitiated) {
                        onMessage?.invoke(
                            if (anyActive) "Pro üyeliğin geri yüklendi."
                            else "Bu Google hesabında aktif bir Pro satın alımı bulunamadı."
                        )
                    }
                }
            }
        }
    }

    fun launchPurchase(activity: Activity, offer: Offer) {
        val details = offer.details
        if (details == null) {
            onMessage?.invoke("Satın alma şu anda başlatılamıyor. Lütfen tekrar dene.")
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { offer.offerToken?.let { setOfferToken(it) } }
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingResponseCode.OK) {
            onMessage?.invoke("Satın alma başlatılamadı. Lütfen tekrar dene.")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                list.forEach { if (it.purchaseState == Purchase.PurchaseState.PURCHASED) acknowledgeIfNeeded(it) }
                when {
                    list.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } -> {
                        setPro(true)
                        onMessage?.invoke("Teşekkürler! Pro özellikler açıldı.")
                    }
                    list.any { it.purchaseState == Purchase.PurchaseState.PENDING } ->
                        onMessage?.invoke("Ödemen onay bekliyor. Onaylanınca Pro otomatik açılır.")
                }
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> refreshPurchases(userInitiated = true)
            BillingResponseCode.USER_CANCELED -> Unit
            else -> onMessage?.invoke("Satın alma tamamlanamadı (kod ${result.responseCode}).")
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { }
    }

    private fun loadOffers() {
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUB_PRO)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(subsParams) { result, productResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                subscriptionOffers = productResult.productDetailsList.flatMap { subscriptionOffersOf(it) }
                publishOffers()
            }
        }

        val inappParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(INAPP_LIFETIME)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(inappParams) { result, productResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                lifetimeOffers = productResult.productDetailsList.mapNotNull { details ->
                    val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                        ?: return@mapNotNull null
                    Offer(
                        id = INAPP_LIFETIME,
                        title = "Ömür boyu",
                        price = price,
                        note = "Tek seferlik ödeme, abonelik yok",
                        details = details,
                        offerToken = null,
                    )
                }
                publishOffers()
            }
        }
    }

    /** Her temel plan için bir seçenek üretir; plan için ücretsiz deneme teklifi varsa onu tercih eder. */
    private fun subscriptionOffersOf(details: ProductDetails): List<Offer> {
        val all = details.subscriptionOfferDetails.orEmpty()
        return all.groupBy { it.basePlanId }.mapNotNull { (basePlanId, offers) ->
            val withTrial = offers.firstOrNull { o ->
                o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
            }
            val chosen = withTrial ?: offers.firstOrNull { it.offerId == null } ?: offers.first()
            val phases = chosen.pricingPhases.pricingPhaseList
            val recurring = phases.lastOrNull() ?: return@mapNotNull null
            val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
            val isYearly = basePlanId == PLAN_YEARLY
            Offer(
                id = basePlanId,
                title = if (isYearly) "Yıllık" else "Aylık",
                price = recurring.formattedPrice + if (isYearly) " / yıl" else " / ay",
                note = buildString {
                    if (trialPhase != null) append(trialText(trialPhase.billingPeriod)).append(", sonra ")
                    append("otomatik yenilenir, istediğin zaman iptal edebilirsin")
                },
                details = details,
                offerToken = chosen.offerToken,
            )
        }
    }

    /** ISO-8601 süresini ("P7D", "P1W") okunur metne çevirir. */
    private fun trialText(period: String): String {
        val amount = period.filter { it.isDigit() }.toIntOrNull() ?: return "Ücretsiz deneme"
        val unit = when (period.lastOrNull()) {
            'D' -> "gün"
            'W' -> "hafta"
            'M' -> "ay"
            'Y' -> "yıl"
            else -> return "Ücretsiz deneme"
        }
        return "$amount $unit ücretsiz"
    }

    private fun publishOffers() {
        // Sıra: yıllık (önerilen), aylık, ömür boyu.
        val subs = subscriptionOffers.sortedBy { if (it.id == PLAN_YEARLY) 0 else 1 }
        onOffersChanged?.invoke(subs + lifetimeOffers)
    }

    private fun setPro(value: Boolean) {
        if (settings.isPro != value) settings.isPro = value
        onProChanged?.invoke(value)
    }

    companion object {
        const val SUB_PRO = "pro"
        const val PLAN_MONTHLY = "aylik"
        const val PLAN_YEARLY = "yillik"
        const val INAPP_LIFETIME = "pro_omur_boyu"
        const val MANAGE_SUBSCRIPTIONS_URL =
            "https://play.google.com/store/account/subscriptions?sku=$SUB_PRO&package=tr.com.covolt.aramaengelleyici"
    }
}
