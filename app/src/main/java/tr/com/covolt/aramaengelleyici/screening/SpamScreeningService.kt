package tr.com.covolt.aramaengelleyici.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import tr.com.covolt.aramaengelleyici.data.BlockAction

/**
 * Android, rehberde olmayan numaralardan gelen her aramada bu servisi çağırır.
 * Karar tamamen cihazda verilir; numara hiçbir sunucuya gönderilmez.
 * Asıl mantık test edilebilsin diye [CallScreeningLogic] içinde.
 */
class SpamScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, allow())
            return
        }

        val response = runCatching {
            val result = CallScreeningLogic.evaluate(
                context = this,
                rawNumber = callDetails.handle?.schemeSpecificPart,
                presentation = callDetails.handlePresentation,
            )
            toResponse(result)
        }.getOrElse { allow() }

        respondToCall(callDetails, response)
    }

    private fun toResponse(result: CallScreeningLogic.Result): CallResponse = when (result) {
        CallScreeningLogic.Result.Allow -> allow()
        is CallScreeningLogic.Result.Block -> when (result.action) {
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
}
