package tr.com.covolt.aramaengelleyici.ui

import tr.com.covolt.aramaengelleyici.billing.BillingRepository
import tr.com.covolt.aramaengelleyici.core.RuleConfig
import tr.com.covolt.aramaengelleyici.data.BlockAction
import tr.com.covolt.aramaengelleyici.data.BlockedCall

data class UiState(
    val config: RuleConfig = RuleConfig(),
    val action: BlockAction = BlockAction.REJECT,
    val roleAvailable: Boolean = true,
    val roleHeld: Boolean = false,
    val weekCount: Int = 0,
    val totalCount: Long = 0,
    val history: List<BlockedCall> = emptyList(),
    val offers: List<BillingRepository.Offer> = emptyList(),
    val message: String? = null,
    val nowMillis: Long = System.currentTimeMillis(),
) {
    val isPro: Boolean get() = config.isPro
    val isPaused: Boolean get() = config.pausedUntilMillis > nowMillis
}

enum class Screen { HOME, HISTORY, LISTS, PRO }

enum class RuleToggle(val title: String, val description: String, val pro: Boolean) {
    B0850("0850 numaralarını engelle", "Çağrı merkezi, kampanya ve reklam aramaları", pro = false),
    HIDDEN("Gizli numaraları engelle", "Numarasını gizleyerek arayanlar", pro = false),
    B444("444 numaralarını engelle", "444 ile başlayan çağrı merkezleri", pro = true),
    INTERNATIONAL("Yurt dışı numaraları engelle", "+90 dışındaki tüm numaralar; dolandırıcılara karşı", pro = true),
    NIGHT("Gece modu", "Gece rehber dışı kimse aramasın", pro = true),
    UNKNOWN(
        "Rehberde olmayan herkesi engelle",
        "Bahis ve dolandırıcıların cep numaralarına karşı en güçlü koruma",
        pro = true,
    ),
}

enum class ListKind { BLACK, WHITE, PREFIX }

/** Arayüzün tetiklediği işlemler. MainActivity uygular. */
interface AppActions {
    fun enableProtection()

    /** Pro gerektiren bir kural Pro olmadan açılmak istenirse false döner. */
    fun setRule(rule: RuleToggle, enabled: Boolean): Boolean
    fun setAction(action: BlockAction)
    fun pauseForCargo(hours: Int)
    fun resumeNow()

    /** Hata varsa kullanıcıya gösterilecek mesajı, yoksa null döner. */
    fun addToList(kind: ListKind, raw: String): String?
    fun removeFromList(kind: ListKind, value: String)
    fun clearHistory()
    fun purchase(offer: BillingRepository.Offer)
    fun restorePurchases()
    fun openUrl(url: String)
    fun messageShown()
}
