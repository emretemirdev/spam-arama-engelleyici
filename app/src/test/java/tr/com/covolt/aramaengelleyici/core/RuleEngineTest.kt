package tr.com.covolt.aramaengelleyici.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val now = 1_000_000L
    private val noon = 12 * 60

    private fun decide(
        number: String?,
        config: RuleConfig,
        hidden: Boolean = false,
        minute: Int = noon,
        at: Long = now,
    ) = RuleEngine.decide(number, hidden, config, at, minute)

    @Test
    fun freeDefaultsBlock0850AndHidden() {
        val free = RuleConfig()
        assertEquals(Decision.Block(BlockReason.N0850), decide("+908501112233", free))
        assertEquals(Decision.Block(BlockReason.HIDDEN), decide(null, free, hidden = true))
        assertEquals(Decision.Allow, decide("+905321112233", free))
    }

    @Test
    fun proRulesIgnoredWhenNotPro() {
        val config = RuleConfig(
            isPro = false, block444 = true, blockInternational = true,
            blockAllUnknown = true, customPrefixes = setOf("0212"),
        )
        assertEquals(Decision.Allow, decide("4441234", config))
        assertEquals(Decision.Allow, decide("+447911123456", config))
        assertEquals(Decision.Allow, decide("02121112233", config))
        assertEquals(Decision.Allow, decide("05321112233", config))
    }

    @Test
    fun proRules() {
        val pro = RuleConfig(isPro = true, block444 = true, blockInternational = true)
        assertEquals(Decision.Block(BlockReason.N444), decide("4441234", pro))
        assertEquals(Decision.Block(BlockReason.INTERNATIONAL), decide("+447911123456", pro))
        assertEquals(Decision.Allow, decide("05321112233", pro))

        val unknown = RuleConfig(isPro = true, blockAllUnknown = true)
        assertEquals(Decision.Block(BlockReason.UNKNOWN), decide("05321112233", unknown))

        val prefix = RuleConfig(isPro = true, customPrefixes = setOf("0212"))
        assertEquals(Decision.Block(BlockReason.CUSTOM_PREFIX), decide("+902121112233", prefix))
    }

    @Test
    fun whitelistBeatsRules() {
        val config = RuleConfig(whitelist = setOf("08501112233"))
        assertEquals(Decision.Allow, decide("+908501112233", config))
    }

    @Test
    fun blacklistWorksForFreeUsers() {
        val config = RuleConfig(blacklist = setOf("05321112233"))
        assertEquals(Decision.Block(BlockReason.BLACKLIST), decide("+905321112233", config))
    }

    @Test
    fun hiddenAllowedWhenRuleOff() {
        assertEquals(Decision.Allow, decide(null, RuleConfig(blockHidden = false), hidden = true))
    }

    @Test
    fun cargoPausePausesAutomaticRulesButNotLists() {
        val config = RuleConfig(
            isPro = true,
            blockAllUnknown = true,
            pausedUntilMillis = now + 60_000,
            blacklist = setOf("05329998877"),
        )
        assertEquals(Decision.Allow, decide("+908501112233", config))
        assertEquals(Decision.Allow, decide("05321112233", config))
        assertEquals(Decision.Block(BlockReason.BLACKLIST), decide("05329998877", config))
        // Süre dolunca kurallar geri gelir.
        assertEquals(Decision.Block(BlockReason.N0850), decide("08501112233", config, at = now + 60_001))
    }

    @Test
    fun nightMode() {
        val config = RuleConfig(isPro = true, nightModeEnabled = true)
        assertEquals(Decision.Block(BlockReason.NIGHT), decide("05321112233", config, minute = 23 * 60 + 30))
        assertEquals(Decision.Block(BlockReason.NIGHT), decide("05321112233", config, minute = 3 * 60))
        assertEquals(Decision.Allow, decide("05321112233", config, minute = 9 * 60))
    }

    @Test
    fun windowMath() {
        assertTrue(RuleEngine.isInWindow(23 * 60, 23 * 60, 8 * 60))
        assertFalse(RuleEngine.isInWindow(8 * 60, 23 * 60, 8 * 60))
        assertTrue(RuleEngine.isInWindow(10 * 60, 9 * 60, 17 * 60))
        assertFalse(RuleEngine.isInWindow(17 * 60, 9 * 60, 17 * 60))
        assertFalse(RuleEngine.isInWindow(5, 5, 5))
    }
}
