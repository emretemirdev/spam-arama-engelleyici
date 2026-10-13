package tr.com.covolt.aramaengelleyici.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Ekranların PNG görüntülerini app/build/screenshots altına yazar.
 * 411x822 dp (2:1) ve xxhdpi: Google Play'in ekran görüntüsü oran sınırına uyar.
 * CI bu dosyaları "screenshots" dalına yükler.
 */
internal fun ComposeShooter.save(name: String) {
    rule.waitForIdle()
    val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
    val dir = File("build/screenshots").apply { mkdirs() }
    FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

internal class ComposeShooter(
    val rule: androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>,
) {
    fun shoot(name: String, content: @Composable () -> Unit) {
        rule.setContent {
            AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        save(name)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h822dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shooter by lazy { ComposeShooter(compose) }
    private val actions = FakeActions()
    private val snackbar = SnackbarHostState()

    @Test
    fun home() = shooter.shoot("01_ana_ekran") {
        HomeScreen(SampleData.state(), actions, snackbar, onNavigate = {})
    }

    @Test
    fun homeProtectionOff() = shooter.shoot("02_koruma_kapali") {
        HomeScreen(SampleData.state(roleHeld = false), actions, snackbar, onNavigate = {})
    }

    @Test
    fun history() = shooter.shoot("03_engellenenler") {
        HistoryScreen(SampleData.state(), actions, snackbar, onBack = {})
    }

    @Test
    fun lists() = shooter.shoot("04_listeler") {
        ListsScreen(SampleData.state(), actions, snackbar, onBack = {}, onOpenPro = {})
    }

    @Test
    fun pro() = shooter.shoot("05_pro") {
        ProScreen(SampleData.state(), actions, snackbar, onBack = {})
    }

    @Test
    fun settings() = shooter.shoot("06_ayarlar") {
        SettingsScreen(SampleData.state(), actions, snackbar, onBack = {})
    }

    @Test
    fun homePro() = shooter.shoot("07_ana_ekran_pro") {
        HomeScreen(SampleData.state(isPro = true), actions, snackbar, onNavigate = {})
    }

    @Test
    fun homeCargoPaused() = shooter.shoot("08_kargo_modu") {
        HomeScreen(SampleData.state(paused = true), actions, snackbar, onNavigate = {})
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h822dp-xxhdpi-night")
class DarkScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shooter by lazy { ComposeShooter(compose) }
    private val actions = FakeActions()
    private val snackbar = SnackbarHostState()

    @Test
    fun home() = shooter.shoot("09_ana_ekran_koyu") {
        HomeScreen(SampleData.state(), actions, snackbar, onNavigate = {})
    }

    @Test
    fun pro() = shooter.shoot("10_pro_koyu") {
        ProScreen(SampleData.state(), actions, snackbar, onBack = {})
    }
}
