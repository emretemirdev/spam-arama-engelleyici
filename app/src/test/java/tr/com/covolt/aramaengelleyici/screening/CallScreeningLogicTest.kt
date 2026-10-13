package tr.com.covolt.aramaengelleyici.screening

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.telecom.TelecomManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import tr.com.covolt.aramaengelleyici.clearAppPrefs
import tr.com.covolt.aramaengelleyici.core.BlockReason
import tr.com.covolt.aramaengelleyici.data.BlockAction
import tr.com.covolt.aramaengelleyici.data.HistoryStore
import tr.com.covolt.aramaengelleyici.data.SettingsStore
import tr.com.covolt.aramaengelleyici.screening.CallScreeningLogic.Result

@RunWith(AndroidJUnit4::class)
class CallScreeningLogicTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val allowed = TelecomManager.PRESENTATION_ALLOWED

    @Before
    fun setUp() = context.clearAppPrefs()

    private fun evaluate(number: String?, presentation: Int = allowed, minute: Int = 12 * 60) =
        CallScreeningLogic.evaluate(context, number, presentation, nowMillis = 1_000L, minuteOfDay = minute)

    @Test
    fun blocks0850ByDefaultAndRecordsHistory() {
        val result = evaluate("+908501112233")
        assertEquals(Result.Block(BlockReason.N0850, BlockAction.REJECT, "08501112233"), result)

        val history = HistoryStore(context).all()
        assertEquals(1, history.size)
        assertEquals("08501112233", history[0].number)
        assertEquals(BlockReason.N0850, history[0].reason)
    }

    @Test
    fun allowsOrdinaryMobileByDefault() {
        assertEquals(Result.Allow, evaluate("+905321112233"))
        assertTrue(HistoryStore(context).all().isEmpty())
    }

    @Test
    fun restrictedPresentationIsHidden() {
        val result = evaluate(null, TelecomManager.PRESENTATION_RESTRICTED)
        assertEquals(
            Result.Block(BlockReason.HIDDEN, BlockAction.REJECT, CallScreeningLogic.HIDDEN_LABEL),
            result,
        )
    }

    @Test
    fun unreadableNumberIsTreatedAsHidden() {
        val result = evaluate("anonymous")
        assertTrue(result is Result.Block && result.reason == BlockReason.HIDDEN)
    }

    @Test
    fun silenceActionIsReturned() {
        SettingsStore(context).action = BlockAction.SILENCE
        val result = evaluate("08501112233") as Result.Block
        assertEquals(BlockAction.SILENCE, result.action)
    }

    @Test
    fun proRulesRequirePro() {
        val settings = SettingsStore(context)
        settings.blockAllUnknown = true
        assertEquals(Result.Allow, evaluate("05321112233"))

        settings.isPro = true
        assertEquals(
            Result.Block(BlockReason.UNKNOWN, BlockAction.REJECT, "05321112233"),
            evaluate("05321112233"),
        )
    }

    @Test
    fun whitelistWins() {
        SettingsStore(context).whitelist = setOf("08501112233")
        assertEquals(Result.Allow, evaluate("+90 850 111 22 33"))
    }

    @Test
    fun cargoPauseLetsCallsThrough() {
        SettingsStore(context).pausedUntilMillis = 5_000L
        assertEquals(Result.Allow, evaluate("08501112233"))
    }

    @Test
    fun postsNotificationOnlyWhenEnabled() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(NotificationManager::class.java)

        evaluate("08501112233")
        assertEquals(0, shadowOf(manager).allNotifications.size)

        SettingsStore(context).notifyOnBlock = true
        evaluate("08501112233")
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }
}
