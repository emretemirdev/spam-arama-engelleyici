package tr.com.covolt.aramaengelleyici

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import tr.com.covolt.aramaengelleyici.data.SettingsStore

/** Gerçek MainActivity'yi (gerçek ayarlar, gerçek geçmiş) açıp uçtan uca dener. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h1600dp")
class MainActivitySmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val settings get() = SettingsStore(compose.activity)

    private fun open(text: String) {
        compose.onNodeWithTag("home_list").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }

    @Test
    fun launchesWithProtectionCard() {
        compose.onNodeWithTag("protection_title").assertIsDisplayed()
        compose.onNodeWithText("Kurallar").assertIsDisplayed()
    }

    @Test
    fun toggling0850Persists() {
        assertTrue(settings.block0850)
        compose.onNodeWithTag("home_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("rule_B0850"))
        compose.onNodeWithTag("rule_B0850").performClick()
        compose.waitForIdle()
        assertTrue(!settings.block0850)
    }

    @Test
    fun blacklistAddPersistsAndFreeLimitApplies() {
        open("Kara liste, beyaz liste ve önekler")
        val numbers = listOf("05321110001", "05321110002", "05321110003", "05321110004", "05321110005")
        numbers.forEach {
            compose.onNodeWithTag("input_BLACK").performTextInput(it)
            compose.onNodeWithTag("add_BLACK").performClick()
            compose.waitForIdle()
        }
        assertEquals(numbers.toSet(), settings.blacklist)

        compose.onNodeWithTag("input_BLACK").performTextInput("05321110006")
        compose.onNodeWithTag("add_BLACK").performClick()
        compose.onNodeWithText("Ücretsiz sürümde en fazla", substring = true).assertIsDisplayed()
        assertEquals(5, settings.blacklist.size)
    }

    @Test
    fun whitelistRemovesFromBlacklist() {
        settings.blacklist = setOf("08501112233")
        open("Kara liste, beyaz liste ve önekler")
        compose.onNodeWithTag("input_WHITE").performTextInput("+90 850 111 22 33")
        compose.onNodeWithTag("add_WHITE").performClick()
        compose.waitForIdle()
        assertEquals(setOf("08501112233"), settings.whitelist)
        assertTrue(settings.blacklist.isEmpty())
    }

    @Test
    fun prefixRequiresPro() {
        open("Kara liste, beyaz liste ve önekler")
        compose.onNodeWithTag("lists_list").performScrollToNode(hasText("Pro ile aç"))
        compose.onNodeWithText("Pro ile aç").performClick()
        compose.onNodeWithText("Hiçbir istenmeyen arama kalmasın").assertIsDisplayed()
    }

    @Test
    fun nightWindowEditable() {
        open("Ayarlar ve yardım")
        compose.onNodeWithTag("settings_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("night_end_minus"))
        compose.onNodeWithTag("night_end_minus").performClick()
        compose.waitForIdle()
        assertEquals(7 * 60 + 30, settings.nightEndMinute)
    }

    @Test
    fun weeklySummaryToggle() {
        open("Ayarlar ve yardım")
        compose.onNodeWithTag("settings_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("switch_weekly"))
        compose.onNodeWithTag("switch_weekly").performClick()
        compose.waitForIdle()
        assertTrue(!settings.weeklySummary)
    }
}
