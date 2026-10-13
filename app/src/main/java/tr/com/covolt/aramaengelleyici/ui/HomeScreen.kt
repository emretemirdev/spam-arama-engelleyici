package tr.com.covolt.aramaengelleyici.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tr.com.covolt.aramaengelleyici.core.RuleConfig
import tr.com.covolt.aramaengelleyici.data.BlockAction

@Composable
fun HomeScreen(
    state: UiState,
    actions: AppActions,
    snackbar: SnackbarHostState,
    onNavigate: (Screen) -> Unit,
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_list"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item {
                Text(
                    "Arama Engelle",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                ProtectionCard(state, actions)
            }

            if (state.roleHeld) {
                item {
                    Spacer(Modifier.height(12.dp))
                    CargoCard(state, actions)
                }
            }

            item { SectionTitle("Kurallar") }
            item {
                AppCard {
                    RuleToggle.entries.forEachIndexed { index, rule ->
                        if (index > 0) HorizontalDivider()
                        RuleRow(
                            rule = rule,
                            // Pro bitmişse kayıtlı ayar açık olsa bile kapalı görünür (motor da yok sayar).
                            checked = rule.isOn(state.config) && !(rule.pro && !state.isPro),
                            locked = rule.pro && !state.isPro,
                            extra = if (rule == RuleToggle.NIGHT) {
                                "${formatMinuteOfDay(state.config.nightStartMinute)}–" +
                                    formatMinuteOfDay(state.config.nightEndMinute)
                            } else null,
                            onChange = { enabled ->
                                if (!actions.setRule(rule, enabled)) onNavigate(Screen.PRO)
                            },
                        )
                    }
                }
            }

            item { SectionTitle("Engellenen arama ne olsun?") }
            item {
                AppCard {
                    ActionOption(
                        title = "Reddet",
                        description = "Arama hemen kapatılır, telefon hiç çalmaz.",
                        selected = state.action == BlockAction.REJECT,
                        onSelect = { actions.setAction(BlockAction.REJECT) },
                    )
                    ActionOption(
                        title = "Sessize al",
                        description = "Arama sessizce gelir, zil çalmaz; istersen açabilirsin.",
                        selected = state.action == BlockAction.SILENCE,
                        onSelect = { actions.setAction(BlockAction.SILENCE) },
                    )
                }
            }

            item { SectionTitle("Yönet") }
            item {
                AppCard {
                    NavRow(
                        title = "Engellenen aramalar",
                        subtitle = if (state.history.isEmpty()) "Henüz yok" else "${state.history.size} kayıt",
                        onClick = { onNavigate(Screen.HISTORY) },
                    )
                    HorizontalDivider()
                    NavRow(
                        title = "Kara liste, beyaz liste ve önekler",
                        subtitle = "Belirli numaraları engelle ya da hep izin ver",
                        onClick = { onNavigate(Screen.LISTS) },
                    )
                    HorizontalDivider()
                    NavRow(
                        title = if (state.isPro) "Pro üyelik ✓" else "Pro'ya geç",
                        subtitle = if (state.isPro) "Tüm kurallar açık" else "444, yurt dışı, gece modu ve rehber dışı koruma",
                        onClick = { onNavigate(Screen.PRO) },
                    )
                    HorizontalDivider()
                    NavRow(
                        title = "Ayarlar ve yardım",
                        subtitle = "Bildirimler, gece saatleri, sık sorulanlar",
                        onClick = { onNavigate(Screen.SETTINGS) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Rehberindeki kişiler asla engellenmez. Numaralar, listeler ve geçmiş yalnızca " +
                        "bu telefonda tutulur; hiçbir veri sunucuya gönderilmez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProtectionCard(state: UiState, actions: AppActions) {
    val on = state.roleHeld
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (on) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                if (on) "Koruma açık" else "Koruma kapalı",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("protection_title"),
            )
            Spacer(Modifier.height(6.dp))
            when {
                on -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Stat(value = state.weekCount.toString(), label = "bu hafta engellendi")
                        Stat(value = state.totalCount.toString(), label = "toplam")
                    }
                    if (state.totalCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = actions::shareApp) { Text("Paylaş") }
                    }
                }
                !state.roleAvailable -> Text(
                    "Bu telefon arama filtrelemeyi desteklemiyor. Android 10 veya üzeri gerekir; " +
                        "bazı üretici yazılımları bu özelliği kapatmış olabilir.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> {
                    Text(
                        "Açmak için Arama Engelle'yi \"Arayan kimliği ve spam\" uygulaması olarak seç. " +
                            "Rehberine erişmiyoruz, aramaların hiçbir yere gönderilmiyor.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = actions::enableProtection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enable_button"),
                    ) {
                        Text("Korumayı aç")
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CargoCard(state: UiState, actions: AppActions) {
    AppCard {
        if (state.isPaused) {
            Text("Kargo modu açık", style = MaterialTheme.typography.titleMedium)
            Text(
                "Otomatik engelleme durduruldu. Bitiş saati: ${formatTime(state.config.pausedUntilMillis)}. " +
                    "Kara listen çalışmaya devam ediyor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = actions::resumeNow) { Text("Şimdi bitir") }
        } else {
            Text("Kargo veya kurye mi bekliyorsun?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Bazı kargo ve market aramaları 0850'den gelir. Engellemeyi kısa süre durdur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { actions.pauseForCargo(2) }) { Text("2 saat durdur") }
                OutlinedButton(onClick = { actions.pauseForCargo(6) }) { Text("6 saat") }
            }
        }
    }
}

@Composable
private fun RuleRow(
    rule: RuleToggle,
    checked: Boolean,
    locked: Boolean,
    extra: String?,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rule.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                if (locked) {
                    Spacer(Modifier.width(8.dp))
                    ProBadge()
                }
            }
            Text(
                if (extra != null) "${rule.description} ($extra)" else rule.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag("rule_${rule.name}"))
    }
}

@Composable
private fun ActionOption(title: String, description: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun RuleToggle.isOn(config: RuleConfig): Boolean = when (this) {
    RuleToggle.B0850 -> config.block0850
    RuleToggle.HIDDEN -> config.blockHidden
    RuleToggle.B444 -> config.block444
    RuleToggle.INTERNATIONAL -> config.blockInternational
    RuleToggle.NIGHT -> config.nightModeEnabled
    RuleToggle.UNKNOWN -> config.blockAllUnknown
}
