package tr.com.covolt.aramaengelleyici.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tr.com.covolt.aramaengelleyici.core.PhoneNumbers
import tr.com.covolt.aramaengelleyici.screening.SpamScreeningService

@Composable
fun HistoryScreen(
    state: UiState,
    actions: AppActions,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            BackTopBar(title = "Engellenen aramalar", onBack = onBack) {
                if (state.history.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) { Text("Temizle") }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.history.isEmpty()) {
            Column(Modifier.padding(padding).padding(24.dp)) {
                Text("Henüz engellenen arama yok.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Engellenen her arama burada görünür. Kayıtlar yalnızca bu telefonda tutulur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                items(state.history) { call ->
                    val isHidden = call.number == SpamScreeningService.HIDDEN_LABEL
                    val whitelisted = call.number in state.config.whitelist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isHidden) call.number else PhoneNumbers.pretty(call.number),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "${call.reason.label} · ${formatDateTime(call.timeMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!isHidden && !whitelisted) {
                            TextButton(onClick = {
                                val error = actions.addToList(ListKind.WHITE, call.number)
                                if (error != null) scope.launch { snackbar.showSnackbar(error) }
                            }) { Text("İzin ver") }
                        } else if (whitelisted) {
                            Text(
                                "İzinli",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Geçmiş silinsin mi?") },
            text = { Text("Engellenen arama kayıtları bu telefondan silinir. Toplam sayaç korunur.") },
            confirmButton = {
                TextButton(onClick = {
                    actions.clearHistory()
                    confirmClear = false
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Vazgeç") } },
        )
    }
}
