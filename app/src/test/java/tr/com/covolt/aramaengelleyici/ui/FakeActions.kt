package tr.com.covolt.aramaengelleyici.ui

import tr.com.covolt.aramaengelleyici.billing.BillingRepository
import tr.com.covolt.aramaengelleyici.core.BlockReason
import tr.com.covolt.aramaengelleyici.core.RuleConfig
import tr.com.covolt.aramaengelleyici.data.BlockAction
import tr.com.covolt.aramaengelleyici.data.BlockedCall

/** Arayüz testleri için: çağrılan işlemleri kaydeder, istenen sonuçları döndürür. */
class FakeActions : AppActions {
    val calls = mutableListOf<String>()
    var setRuleResult = true
    var addToListResult: String? = null

    override fun enableProtection() { calls += "enable" }
    override fun setRule(rule: RuleToggle, enabled: Boolean): Boolean {
        calls += "rule:${rule.name}:$enabled"
        return setRuleResult
    }
    override fun setAction(action: BlockAction) { calls += "action:${action.name}" }
    override fun pauseForCargo(hours: Int) { calls += "pause:$hours" }
    override fun resumeNow() { calls += "resume" }
    override fun addToList(kind: ListKind, raw: String): String? {
        calls += "add:${kind.name}:$raw"
        return addToListResult
    }
    override fun removeFromList(kind: ListKind, value: String) { calls += "remove:${kind.name}:$value" }
    override fun clearHistory() { calls += "clearHistory" }
    override fun purchase(offer: BillingRepository.Offer) { calls += "purchase:${offer.id}" }
    override fun restorePurchases() { calls += "restore" }
    override fun setNotifyOnBlock(enabled: Boolean) { calls += "notify:$enabled" }
    override fun setWeeklySummary(enabled: Boolean) { calls += "weekly:$enabled" }
    override fun openNotificationSettings() { calls += "notificationSettings" }
    override fun setNightWindow(startMinute: Int, endMinute: Int) { calls += "night:$startMinute-$endMinute" }
    override fun shareApp() { calls += "share" }
    override fun rateApp() { calls += "rate" }
    override fun openUrl(url: String) { calls += "url:$url" }
    override fun messageShown() { calls += "messageShown" }
}

/** Ekran görüntüleri ve testler için örnek veriler. */
object SampleData {
    private const val NOW = 1_760_000_000_000L
    private const val MIN = 60_000L

    val history = listOf(
        BlockedCall("08500000001", BlockReason.N0850, NOW - 12 * MIN),
        BlockedCall("05000000002", BlockReason.UNKNOWN, NOW - 95 * MIN),
        BlockedCall("Gizli numara", BlockReason.HIDDEN, NOW - 5 * 60 * MIN),
        BlockedCall("+447700900123", BlockReason.INTERNATIONAL, NOW - 26 * 60 * MIN),
        BlockedCall("4440000", BlockReason.N444, NOW - 30 * 60 * MIN),
        BlockedCall("08500000003", BlockReason.N0850, NOW - 50 * 60 * MIN),
    )

    val offers = listOf(
        BillingRepository.Offer(
            id = "yillik", title = "Yıllık", price = "₺149,99 / yıl",
            note = "7 gün ücretsiz, sonra otomatik yenilenir, istediğin zaman iptal edebilirsin",
            details = null, offerToken = null,
        ),
        BillingRepository.Offer(
            id = "aylik", title = "Aylık", price = "₺29,99 / ay",
            note = "otomatik yenilenir, istediğin zaman iptal edebilirsin",
            details = null, offerToken = null,
        ),
        BillingRepository.Offer(
            id = "pro_omur_boyu", title = "Ömür boyu", price = "₺299,99",
            note = "Tek seferlik ödeme, abonelik yok",
            details = null, offerToken = null,
        ),
    )

    fun state(
        roleHeld: Boolean = true,
        isPro: Boolean = false,
        paused: Boolean = false,
    ) = UiState(
        config = RuleConfig(
            isPro = isPro,
            block444 = isPro,
            blockInternational = isPro,
            blockAllUnknown = isPro,
            pausedUntilMillis = if (paused) NOW + 2 * 60 * MIN else 0L,
            blacklist = setOf("05001234567", "02120000000"),
            whitelist = setOf("08500001111"),
            customPrefixes = if (isPro) setOf("0212444") else emptySet(),
        ),
        roleHeld = roleHeld,
        weekCount = 17,
        totalCount = 143,
        history = history,
        offers = offers,
        versionName = "1.0.0",
        nowMillis = NOW,
    )
}
