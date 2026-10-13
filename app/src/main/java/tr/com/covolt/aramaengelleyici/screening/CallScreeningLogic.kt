package tr.com.covolt.aramaengelleyici.screening

import android.content.Context
import android.telecom.TelecomManager
import tr.com.covolt.aramaengelleyici.core.BlockReason
import tr.com.covolt.aramaengelleyici.core.Decision
import tr.com.covolt.aramaengelleyici.core.PhoneNumbers
import tr.com.covolt.aramaengelleyici.core.RuleEngine
import tr.com.covolt.aramaengelleyici.data.BlockAction
import tr.com.covolt.aramaengelleyici.data.BlockedCall
import tr.com.covolt.aramaengelleyici.data.HistoryStore
import tr.com.covolt.aramaengelleyici.data.SettingsStore
import tr.com.covolt.aramaengelleyici.notifications.Notifier
import java.util.Calendar

/**
 * Arama filtresi servisinin asıl işi: ayarları oku, karar ver, geçmişe yaz, gerekirse bildir.
 * Android'in Call.Details sınıfından bağımsız olduğu için doğrudan test edilebilir.
 */
object CallScreeningLogic {

    const val HIDDEN_LABEL = "Gizli numara"

    sealed interface Result {
        data object Allow : Result
        data class Block(val reason: BlockReason, val action: BlockAction, val shownNumber: String) : Result
    }

    fun evaluate(
        context: Context,
        rawNumber: String?,
        presentation: Int,
        nowMillis: Long = System.currentTimeMillis(),
        minuteOfDay: Int = currentMinuteOfDay(),
    ): Result {
        val settings = SettingsStore(context)

        // Okunamayan numaralar (boş, "anonymous", SIP adresi) da gizli numara sayılır.
        val parsed = PhoneNumbers.parse(rawNumber)
        val isHidden = presentation == TelecomManager.PRESENTATION_RESTRICTED ||
            presentation == TelecomManager.PRESENTATION_UNKNOWN ||
            parsed == null

        val decision = RuleEngine.decide(rawNumber, isHidden, settings.toConfig(), nowMillis, minuteOfDay)
        if (decision !is Decision.Block) return Result.Allow

        val shownNumber = if (isHidden || parsed == null) HIDDEN_LABEL else parsed.normalized
        HistoryStore(context).add(BlockedCall(shownNumber, decision.reason, nowMillis))

        if (settings.notifyOnBlock) {
            runCatching { Notifier.showBlocked(context, shownNumber, decision.reason) }
        }
        return Result.Block(decision.reason, settings.action, shownNumber)
    }

    fun currentMinuteOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }
}
