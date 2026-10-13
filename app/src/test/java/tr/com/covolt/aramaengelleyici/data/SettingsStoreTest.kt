package tr.com.covolt.aramaengelleyici.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tr.com.covolt.aramaengelleyici.clearAppPrefs

@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = context.clearAppPrefs()

    @Test
    fun defaults() {
        val s = SettingsStore(context)
        assertFalse(s.isPro)
        assertTrue(s.block0850)
        assertTrue(s.blockHidden)
        assertFalse(s.block444)
        assertFalse(s.blockInternational)
        assertFalse(s.blockAllUnknown)
        assertFalse(s.nightModeEnabled)
        assertEquals(23 * 60, s.nightStartMinute)
        assertEquals(8 * 60, s.nightEndMinute)
        assertEquals(BlockAction.REJECT, s.action)
        assertFalse(s.notifyOnBlock)
        assertTrue(s.weeklySummary)
        assertTrue(s.blacklist.isEmpty())
    }

    @Test
    fun valuesPersistAcrossInstances() {
        SettingsStore(context).apply {
            isPro = true
            block444 = true
            action = BlockAction.SILENCE
            blacklist = setOf("05321112233")
            whitelist = setOf("08501112233")
            customPrefixes = setOf("0212")
            nightStartMinute = 22 * 60
            pausedUntilMillis = 123L
        }
        val again = SettingsStore(context)
        assertTrue(again.isPro)
        assertTrue(again.block444)
        assertEquals(BlockAction.SILENCE, again.action)
        assertEquals(setOf("05321112233"), again.blacklist)
        assertEquals(setOf("08501112233"), again.whitelist)
        assertEquals(setOf("0212"), again.customPrefixes)
        assertEquals(22 * 60, again.nightStartMinute)
        assertEquals(123L, again.pausedUntilMillis)
    }

    @Test
    fun toConfigMirrorsSettings() {
        val s = SettingsStore(context).apply {
            isPro = true
            blockAllUnknown = true
            blacklist = setOf("05320000000")
        }
        val config = s.toConfig()
        assertTrue(config.isPro)
        assertTrue(config.blockAllUnknown)
        assertTrue(config.block0850)
        assertEquals(setOf("05320000000"), config.blacklist)
    }

    @Test
    fun corruptedActionFallsBackToReject() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putString("action", "BOZUK").commit()
        assertEquals(BlockAction.REJECT, SettingsStore(context).action)
    }
}
