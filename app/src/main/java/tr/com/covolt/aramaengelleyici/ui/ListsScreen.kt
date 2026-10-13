package tr.com.covolt.aramaengelleyici.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tr.com.covolt.aramaengelleyici.core.PhoneNumbers
import tr.com.covolt.aramaengelleyici.core.RuleEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListsScreen(
    state: UiState,
    actions: AppActions,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
) {
    Scaffold(
        topBar = { BackTopBar(title = "Listeler", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        // Kenardan kenara ekranda klavye alanları örtmesin diye imePadding gerekiyor.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            item {
                ListSection(
                    title = "Kara liste",
                    description = if (state.isPro) {
                        "Bu numaralar her zaman engellenir."
                    } else {
                        "Bu numaralar her zaman engellenir. Ücretsiz sürümde en fazla " +
                            "${RuleEngine.FREE_BLACKLIST_LIMIT} numara."
                    },
                    placeholder = "0532 123 45 67",
                    items = state.config.blacklist,
                    kind = ListKind.BLACK,
                    actions = actions,
                )
            }
            item {
                ListSection(
                    title = "Beyaz liste",
                    description = "Rehberinde olmasa bile bu numaralar hiçbir kurala takılmaz. " +
                        "Kargo firması veya bankanın 0850 numarasını buraya ekleyebilirsin.",
                    placeholder = "0850 123 45 67",
                    items = state.config.whitelist,
                    kind = ListKind.WHITE,
                    actions = actions,
                )
            }
            item {
                if (state.isPro) {
                    ListSection(
                        title = "Engellenen önekler",
                        description = "Bu rakamlarla başlayan tüm numaralar engellenir. Örnek: 0212 444, +44",
                        placeholder = "0212 444",
                        items = state.config.customPrefixes,
                        kind = ListKind.PREFIX,
                        actions = actions,
                    )
                } else {
                    SectionTitle("Engellenen önekler")
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Belirli rakamlarla başlayan tüm numaraları engelle (ör. 0212 444, +44).",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            ProBadge()
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = onOpenPro) { Text("Pro ile aç") }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ListSection(
    title: String,
    description: String,
    placeholder: String,
    items: Set<String>,
    kind: ListKind,
    actions: AppActions,
) {
    var input by rememberSaveable(kind) { mutableStateOf("") }
    var error by rememberSaveable(kind) { mutableStateOf<String?>(null) }

    SectionTitle(title)
    AppCard {
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    error = null
                },
                placeholder = { Text(placeholder) },
                singleLine = true,
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    error = actions.addToList(kind, input)
                    if (error == null) input = ""
                },
                enabled = input.isNotBlank(),
            ) { Text("Ekle") }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        items.sorted().forEach { value ->
            HorizontalDivider(Modifier.padding(top = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    PhoneNumbers.pretty(value),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { actions.removeFromList(kind, value) }) { Text("Kaldır") }
            }
        }
    }
}
