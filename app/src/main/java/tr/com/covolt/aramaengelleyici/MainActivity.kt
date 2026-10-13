package tr.com.covolt.aramaengelleyici

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tr.com.covolt.aramaengelleyici.billing.BillingRepository
import tr.com.covolt.aramaengelleyici.core.PhoneNumbers
import tr.com.covolt.aramaengelleyici.core.RuleEngine
import tr.com.covolt.aramaengelleyici.data.BlockAction
import tr.com.covolt.aramaengelleyici.data.HistoryStore
import tr.com.covolt.aramaengelleyici.data.SettingsStore
import tr.com.covolt.aramaengelleyici.screening.ScreeningRole
import tr.com.covolt.aramaengelleyici.ui.AppActions
import tr.com.covolt.aramaengelleyici.ui.AppRoot
import tr.com.covolt.aramaengelleyici.ui.AppTheme
import tr.com.covolt.aramaengelleyici.ui.ListKind
import tr.com.covolt.aramaengelleyici.ui.RuleToggle
import tr.com.covolt.aramaengelleyici.ui.UiState

class MainActivity : ComponentActivity(), AppActions {

    private lateinit var settings: SettingsStore
    private lateinit var history: HistoryStore
    private lateinit var billing: BillingRepository

    private var uiState by mutableStateOf(UiState())

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refresh()
            showMessage(
                if (uiState.roleHeld) {
                    "Koruma açıldı. İstenmeyen aramalar artık engelleniyor."
                } else {
                    // Android iki retten sonra pencereyi artık göstermiyor; ayar yolunu söyleyelim.
                    "Koruma açılmadı. Ayarlar > Uygulamalar > Varsayılan uygulamalar > " +
                        "Arayan kimliği ve spam uygulaması bölümünden Arama Engelle'yi seçebilirsin."
                }
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        settings = SettingsStore(this)
        history = HistoryStore(this)
        billing = BillingRepository(this, settings).apply {
            onProChanged = { runOnUiThread { refresh() } }
            onOffersChanged = { offers -> runOnUiThread { uiState = uiState.copy(offers = offers) } }
            onMessage = { message -> runOnUiThread { showMessage(message) } }
        }

        refresh()
        setContent {
            AppTheme {
                AppRoot(state = uiState, actions = this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        billing.connect()
    }

    override fun onDestroy() {
        billing.endConnection()
        super.onDestroy()
    }

    /** Ayarları ve geçmişi diskten yeniden okuyup ekranı günceller. */
    private fun refresh() {
        val now = System.currentTimeMillis()
        uiState = uiState.copy(
            config = settings.toConfig(),
            action = settings.action,
            roleAvailable = ScreeningRole.isAvailable(this),
            roleHeld = ScreeningRole.isHeld(this),
            weekCount = history.countSince(now - WEEK_MILLIS),
            totalCount = history.totalCount(),
            history = history.all(),
            nowMillis = now,
        )
    }

    private fun showMessage(message: String) {
        uiState = uiState.copy(message = message)
    }

    // --- AppActions ---

    override fun enableProtection() {
        val intent = ScreeningRole.requestIntent(this)
        if (intent == null || !ScreeningRole.isAvailable(this)) {
            showMessage("Bu telefon arama filtrelemeyi desteklemiyor.")
            return
        }
        roleLauncher.launch(intent)
    }

    override fun setRule(rule: RuleToggle, enabled: Boolean): Boolean {
        if (enabled && rule.pro && !settings.isPro) return false
        when (rule) {
            RuleToggle.B0850 -> settings.block0850 = enabled
            RuleToggle.HIDDEN -> settings.blockHidden = enabled
            RuleToggle.B444 -> settings.block444 = enabled
            RuleToggle.INTERNATIONAL -> settings.blockInternational = enabled
            RuleToggle.NIGHT -> settings.nightModeEnabled = enabled
            RuleToggle.UNKNOWN -> settings.blockAllUnknown = enabled
        }
        refresh()
        return true
    }

    override fun setAction(action: BlockAction) {
        settings.action = action
        refresh()
    }

    override fun pauseForCargo(hours: Int) {
        settings.pausedUntilMillis = System.currentTimeMillis() + hours * HOUR_MILLIS
        refresh()
    }

    override fun resumeNow() {
        settings.pausedUntilMillis = 0L
        refresh()
    }

    override fun addToList(kind: ListKind, raw: String): String? {
        val value = when (kind) {
            ListKind.PREFIX -> PhoneNumbers.normalizePrefix(raw)
            else -> PhoneNumbers.normalizeForList(raw)
        } ?: return "Geçerli bir numara yaz."

        when (kind) {
            ListKind.BLACK -> {
                val current = settings.blacklist
                if (value in current) return "Bu numara zaten listede."
                if (!settings.isPro && current.size >= RuleEngine.FREE_BLACKLIST_LIMIT) {
                    return "Ücretsiz sürümde en fazla ${RuleEngine.FREE_BLACKLIST_LIMIT} numara eklenebilir. Pro ile sınırsız."
                }
                settings.blacklist = current + value
                settings.whitelist = settings.whitelist - value
            }
            ListKind.WHITE -> {
                val current = settings.whitelist
                if (value in current) return "Bu numara zaten listede."
                settings.whitelist = current + value
                settings.blacklist = settings.blacklist - value
                showMessage("${PhoneNumbers.pretty(value)} artık engellenmeyecek.")
            }
            ListKind.PREFIX -> {
                if (!settings.isPro) return "Önek engelleme Pro özelliğidir."
                val digits = value.count { it.isDigit() }
                // Yurt dışı ülke kodları (+1, +7) kısa olabilir; yurt içi öneklerde en az 3 rakam iste.
                if (!value.startsWith("+") && digits < 3) return "Önek en az 3 rakam olmalı."
                val current = settings.customPrefixes
                if (value in current) return "Bu önek zaten listede."
                settings.customPrefixes = current + value
            }
        }
        refresh()
        return null
    }

    override fun removeFromList(kind: ListKind, value: String) {
        when (kind) {
            ListKind.BLACK -> settings.blacklist = settings.blacklist - value
            ListKind.WHITE -> settings.whitelist = settings.whitelist - value
            ListKind.PREFIX -> settings.customPrefixes = settings.customPrefixes - value
        }
        refresh()
    }

    override fun clearHistory() {
        history.clear()
        refresh()
    }

    override fun purchase(offer: BillingRepository.Offer) {
        billing.launchPurchase(this, offer)
    }

    override fun restorePurchases() {
        billing.refreshPurchases(userInitiated = true)
    }

    override fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            showMessage("Bağlantı açılamadı.")
        }
    }

    override fun messageShown() {
        uiState = uiState.copy(message = null)
    }

    private companion object {
        const val HOUR_MILLIS = 60L * 60L * 1000L
        const val WEEK_MILLIS = 7L * 24L * HOUR_MILLIS
    }
}
