package tr.com.covolt.aramaengelleyici.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h1600dp")
class ScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private val actions = FakeActions()

    private fun showApp(state: UiState) {
        compose.setContent { AppTheme { AppRoot(state = state, actions = actions) } }
    }

    /** Tembel listede öğeyi görünür hale getirip tıklar. */
    private fun clickInList(listTag: String, text: String) {
        compose.onNodeWithTag(listTag).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }

    private fun scrollToTag(listTag: String, tag: String) =
        compose.onNodeWithTag(listTag).performScrollToNode(hasTestTag(tag))

    @Test
    fun protectionOffShowsEnableButton() {
        showApp(SampleData.state(roleHeld = false))
        compose.onNodeWithText("Koruma kapalı").assertIsDisplayed()
        compose.onNodeWithTag("enable_button").performClick()
        assertEquals(listOf("enable"), actions.calls)
    }

    @Test
    fun protectionOnShowsCounters() {
        showApp(SampleData.state(roleHeld = true))
        compose.onNodeWithText("Koruma açık").assertIsDisplayed()
        compose.onNodeWithText("17").assertIsDisplayed()
        compose.onNodeWithText("143").assertIsDisplayed()
        compose.onNodeWithText("Paylaş").performClick()
        assertTrue("share" in actions.calls)
    }

    @Test
    fun freeRuleToggles() {
        showApp(SampleData.state())
        scrollToTag("home_list", "rule_B0850")
        compose.onNodeWithTag("rule_B0850").assertIsOn().performClick()
        assertTrue("rule:B0850:false" in actions.calls)
    }

    @Test
    fun lockedProRuleOpensPaywall() {
        actions.setRuleResult = false
        showApp(SampleData.state(isPro = false))
        scrollToTag("home_list", "rule_UNKNOWN")
        compose.onNodeWithTag("rule_UNKNOWN").assertIsOff().performClick()
        assertTrue("rule:UNKNOWN:true" in actions.calls)
        compose.onNodeWithText("Hiçbir istenmeyen arama kalmasın").assertIsDisplayed()
        compose.onNodeWithText("Yıllık · ₺149,99 / yıl").performClick()
        assertTrue("purchase:yillik" in actions.calls)
    }

    @Test
    fun cargoModeButtons() {
        showApp(SampleData.state())
        compose.onNodeWithText("2 saat durdur").performClick()
        assertTrue("pause:2" in actions.calls)
    }

    @Test
    fun pausedCargoCanBeEnded() {
        showApp(SampleData.state(paused = true))
        compose.onNodeWithText("Kargo modu açık").assertIsDisplayed()
        compose.onNodeWithText("Şimdi bitir").performClick()
        assertTrue("resume" in actions.calls)
    }

    @Test
    fun silenceActionSelectable() {
        showApp(SampleData.state())
        clickInList("home_list", "Sessize al")
        assertTrue("action:SILENCE" in actions.calls)
    }

    @Test
    fun listsAddNumberAndShowError() {
        showApp(SampleData.state())
        clickInList("home_list", "Kara liste, beyaz liste ve önekler")
        compose.onNodeWithText("0500 123 45 67").assertIsDisplayed() // kara listedeki numara (biçimli)
        actions.addToListResult = "Geçerli bir numara yaz."
        compose.onNodeWithTag("input_BLACK").performTextInput("abc")
        compose.onNodeWithTag("add_BLACK").performClick()
        assertTrue("add:BLACK:abc" in actions.calls)
        compose.onNodeWithText("Geçerli bir numara yaz.").assertIsDisplayed()
    }

    @Test
    fun listsRemove() {
        showApp(SampleData.state())
        clickInList("home_list", "Kara liste, beyaz liste ve önekler")
        compose.onNodeWithTag("remove_BLACK_05001234567").performClick()
        assertTrue("remove:BLACK:05001234567" in actions.calls)
    }

    @Test
    fun historyAllowCallsWhitelist() {
        showApp(SampleData.state())
        clickInList("home_list", "Engellenen aramalar")
        compose.onNodeWithText("0850 000 00 01").assertIsDisplayed()
        compose.onNodeWithTag("allow_08500000001").performClick()
        assertTrue("add:WHITE:08500000001" in actions.calls)
    }

    @Test
    fun historyClearAsksForConfirmation() {
        showApp(SampleData.state())
        clickInList("home_list", "Engellenen aramalar")
        compose.onNodeWithText("Temizle").performClick()
        compose.onNodeWithText("Geçmiş silinsin mi?").assertIsDisplayed()
        compose.onNodeWithText("Sil").performClick()
        assertTrue("clearHistory" in actions.calls)
    }

    @Test
    fun settingsStepperAndSwitches() {
        showApp(SampleData.state())
        clickInList("home_list", "Ayarlar ve yardım")
        scrollToTag("settings_list", "night_start_plus")
        compose.onNodeWithTag("night_start_value").assertIsDisplayed()
        compose.onNodeWithTag("night_start_plus").performClick()
        assertTrue("night:${23 * 60 + 30}-${8 * 60}" in actions.calls)
        scrollToTag("settings_list", "switch_weekly")
        compose.onNodeWithTag("switch_weekly").performClick()
        assertTrue("weekly:false" in actions.calls)
        compose.onNodeWithTag("switch_notify_block").performClick()
        assertTrue("notify:true" in actions.calls)
    }

    @Test
    fun faqExpands() {
        showApp(SampleData.state())
        clickInList("home_list", "Ayarlar ve yardım")
        clickInList("settings_list", "Verilerim nereye gidiyor?")
        compose.onNodeWithText("Hiçbir yere", substring = true).assertIsDisplayed()
    }

    @Test
    fun proScreenRestore() {
        compose.setContent {
            AppTheme { ProScreen(SampleData.state(), actions, SnackbarHostState(), onBack = {}) }
        }
        compose.onNodeWithText("Satın alımları geri yükle").performScrollTo().performClick()
        assertTrue("restore" in actions.calls)
    }

    @Test
    fun proActiveHidesOffers() {
        compose.setContent {
            AppTheme { ProScreen(SampleData.state(isPro = true), actions, SnackbarHostState(), onBack = {}) }
        }
        compose.onNodeWithText("Pro üyeliğin aktif").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithTextCount("₺29,99 / ay"))
    }

    @Test
    fun backReturnsHome() {
        showApp(SampleData.state())
        clickInList("home_list", "Ayarlar ve yardım")
        compose.onNodeWithText("‹ Geri").performClick()
        compose.onNodeWithText("Koruma açık").assertIsDisplayed()
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(text: String): Int =
    onAllNodes(androidx.compose.ui.test.hasText(text, substring = true)).fetchSemanticsNodes().size
