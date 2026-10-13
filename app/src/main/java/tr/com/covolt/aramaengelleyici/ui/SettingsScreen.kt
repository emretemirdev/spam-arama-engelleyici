package tr.com.covolt.aramaengelleyici.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val STEP_MINUTES = 30
private const val DAY_MINUTES = 24 * 60

private data class Faq(val question: String, val answer: String)

private val faqs = listOf(
    Faq(
        "Rehberimdeki biri engellenir mi?",
        "Hayır. Android, rehberindeki numaralardan gelen aramaları uygulamaya hiç göstermez; " +
            "bu yüzden tanıdıkların hiçbir kurala takılmaz.",
    ),
    Faq(
        "Kargo veya banka aramasını kaçırır mıyım?",
        "Bazı kargo ve bankalar 0850'den arar. Beklediğin bir arama varsa ana ekrandaki " +
            "kargo modunu aç ya da numarayı beyaz listeye ekle. Engellenen her arama geçmişte " +
            "görünür; tek dokunuşla izin verebilirsin.",
    ),
    Faq(
        "Engelleme çalışmıyor, ne yapmalıyım?",
        "Ana ekranda \"Koruma açık\" yazdığından emin ol. Kapalıysa telefonun Ayarlar > " +
            "Uygulamalar > Varsayılan uygulamalar > Arayan kimliği ve spam bölümünden " +
            "Arama Engelle'yi seç. Samsung ve Xiaomi gibi bazı telefonların kendi spam " +
            "korumaları da vardır; orada Arama Engelle seçili olmalı.",
    ),
    Faq(
        "Bahis aramaları cep numarasından geliyor, ne yapmalıyım?",
        "Bu aramalar sürekli numara değiştirdiği için tek tek engellemek yetmez. Pro'daki " +
            "\"Rehberde olmayan herkesi engelle\" kuralı tanımadığın tüm numaraları durdurur; " +
            "beklediğin aramalar için beyaz listeyi kullan.",
    ),
    Faq(
        "Verilerim nereye gidiyor?",
        "Hiçbir yere. Numaralar, listeler ve geçmiş yalnızca bu telefonda tutulur. Rehberini, " +
            "arama kaydını ve SMS'lerini okumuyoruz; uygulamada reklam veya takip aracı yok.",
    ),
    Faq(
        "Aboneliği nasıl iptal ederim?",
        "Google Play > Profil > Ödemeler ve abonelikler > Abonelikler bölümünden. İptal " +
            "edersen Pro dönem sonuna kadar açık kalır.",
    ),
)

@Composable
fun SettingsScreen(
    state: UiState,
    actions: AppActions,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { BackTopBar(title = "Ayarlar ve yardım", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .testTag("settings_list")
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item { SectionTitle("Bildirimler") }
            item {
                AppCard {
                    SwitchRow(
                        title = "Engellenen aramayı bildir",
                        description = "Her engellemede sessiz bir bildirim göster",
                        checked = state.notifyOnBlock,
                        onChange = actions::setNotifyOnBlock,
                        tag = "switch_notify_block",
                    )
                    HorizontalDivider()
                    SwitchRow(
                        title = "Haftalık özet",
                        description = "Haftada bir kaç aramayı engellediğimizi söyleyelim",
                        checked = state.weeklySummary,
                        onChange = actions::setWeeklySummary,
                        tag = "switch_weekly",
                    )
                    if (!state.notificationsAllowed && (state.notifyOnBlock || state.weeklySummary)) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Bildirim izni kapalı, bu yüzden bildirim gösteremiyoruz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = actions::openNotificationSettings) {
                            Text("Bildirim ayarlarını aç")
                        }
                    }
                }
            }

            item { SectionTitle("Gece modu saatleri") }
            item {
                AppCard {
                    TimeStepper(
                        label = "Başlangıç",
                        minute = state.config.nightStartMinute,
                        tag = "night_start",
                        onChange = { actions.setNightWindow(it, state.config.nightEndMinute) },
                    )
                    HorizontalDivider()
                    TimeStepper(
                        label = "Bitiş",
                        minute = state.config.nightEndMinute,
                        tag = "night_end",
                        onChange = { actions.setNightWindow(state.config.nightStartMinute, it) },
                    )
                    if (!state.isPro) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Gece modunu açmak Pro gerektirir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            ProBadge()
                        }
                    }
                }
            }

            item { SectionTitle("Sık sorulanlar") }
            item {
                AppCard {
                    faqs.forEachIndexed { index, faq ->
                        if (index > 0) HorizontalDivider()
                        FaqRow(faq)
                    }
                }
            }

            item { SectionTitle("Uygulama") }
            item {
                AppCard {
                    NavRow(title = "Uygulamayı değerlendir", subtitle = "Google Play'de yorum yaz", onClick = actions::rateApp)
                    HorizontalDivider()
                    NavRow(title = "Arkadaşına öner", subtitle = "Onlar da 0850 aramalarından kurtulsun", onClick = actions::shareApp)
                    HorizontalDivider()
                    NavRow(
                        title = "Ticari aramaları İYS'den reddet",
                        subtitle = "iys.org.tr",
                        onClick = { actions.openUrl(Links.IYS) },
                    )
                    if (Links.PRIVACY_POLICY.isNotBlank()) {
                        HorizontalDivider()
                        NavRow(title = "Gizlilik politikası", onClick = { actions.openUrl(Links.PRIVACY_POLICY) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Arama Engelle ${state.versionName}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun TimeStepper(label: String, minute: Int, tag: String, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { onChange(shiftMinute(minute, -STEP_MINUTES)) },
            modifier = Modifier.testTag("${tag}_minus"),
        ) { Text("−") }
        Text(
            formatMinuteOfDay(minute),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .testTag("${tag}_value"),
        )
        OutlinedButton(
            onClick = { onChange(shiftMinute(minute, STEP_MINUTES)) },
            modifier = Modifier.testTag("${tag}_plus"),
        ) { Text("+") }
    }
}

/** Günün dakikasını ileri/geri kaydırır; gece yarısını sarar (23:30 + 30 dk = 00:00). */
fun shiftMinute(minute: Int, delta: Int): Int = ((minute + delta) % DAY_MINUTES + DAY_MINUTES) % DAY_MINUTES

@Composable
private fun FaqRow(faq: Faq) {
    var expanded by rememberSaveable(faq.question) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(faq.question, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleMedium)
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Text(
                faq.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
