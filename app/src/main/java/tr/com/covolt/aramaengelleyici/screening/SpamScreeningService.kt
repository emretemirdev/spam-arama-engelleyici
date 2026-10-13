package tr.com.covolt.aramaengelleyici.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.telecom.TelecomManager
import tr.com.covolt.aramaengelleyici.core.Decision
import tr.com.covolt.aramaengelleyici.core.PhoneNumbers
import tr.com.covolt.aramaengelleyici.core.RuleEngine
import tr.com.covolt.aramaengelleyici.data.BlockAction
import tr.com.covolt.aramaengelleyici.data.BlockedCall
import tr.com.covolt.aramaengelleyici.data.HistoryStore
import tr.com.covolt.aramaengelleyici.data.SettingsStore
import java.util.Calendar

/**
 * Android, rehberde olmayan numaralardan gelen her aramada bu servisi çağırır.
 * Karar tamamen cihazda verilir; numara hiçbir sunucuya gönderilmez.
 */
class SpamScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, allow())
            return
        }

        val response = runCatching { screen(callDetails) }.getOrElse { allow() }
        respondToCall(callDetails, response)
    }

    private fun screen(details: Call.Details): CallResponse {
        val settings = SettingsStore(this)
        val rawNumber = details.handle?.schemeSpecificPart
        val presentation = details.handlePresentation
        // Okunamayan numaralar (boş, "anonymous", SIP adresi) da gizli numara sayılır.
        val parsed = PhoneNumbers.parse(rawNumber)
        val isHidden = presentation == TelecomManager.PRESENTATION_RESTRICTED ||
            presentation == TelecomManager.PRESENTATION_UNKNOWN ||
            parsed == null

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val decision = RuleEngine.decide(rawNumber, isHidden, settings.toConfig(), now, minuteOfDay)
        if (decision !is Decision.Block) return allow()

        val shownNumber = if (isHidden || parsed == null) HIDDEN_LABEL else parsed.normalized
        HistoryStore(this).add(BlockedCall(shownNumber, decision.reason, now))

        return when (settings.action) {
            BlockAction.SILENCE -> CallResponse.Builder()
                .setSilenceCall(true)
                .build()

            BlockAction.REJECT -> CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                // Arama, telefonun kendi arama kaydında görünmeye devam eder.
                .setSkipCallLog(false)
                .build()
        }
    }

    private fun allow(): CallResponse = CallResponse.Builder().build()

    companion object {
        const val HIDDEN_LABEL = "Gizli numara"
    }
}
