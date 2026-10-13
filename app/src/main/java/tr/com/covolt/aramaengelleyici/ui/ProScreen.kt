package tr.com.covolt.aramaengelleyici.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tr.com.covolt.aramaengelleyici.billing.BillingRepository

private val proBenefits = listOf(
    "Rehberde olmayan herkesi engelle: bahisçi ve dolandırıcıların cep numaralarına karşı",
    "444 çağrı merkezlerini engelle",
    "Yurt dışı numaraları engelle",
    "Gece modu: gece rehber dışı kimse aramasın",
    "Kendi öneklerin (ör. 0212 444) ve sınırsız kara liste",
)

@Composable
fun ProScreen(
    state: UiState,
    actions: AppActions,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { BackTopBar(title = "Pro", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                if (state.isPro) "Pro üyeliğin aktif" else "Hiçbir istenmeyen arama kalmasın",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            proBenefits.forEach { benefit ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(benefit, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(20.dp))

            if (state.isPro) {
                Text(
                    "Teşekkürler! Tüm kuralları ana ekrandan açıp kapatabilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (state.offers.isEmpty()) {
                Text(
                    "Fiyatlar yükleniyor… Google Play'e bağlanılamıyorsa internet bağlantını kontrol et.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.offers.forEachIndexed { index, offer ->
                    OfferButton(offer = offer, highlighted = index == 0, onClick = { actions.purchase(offer) })
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = actions::restorePurchases) { Text("Satın alımları geri yükle") }
            TextButton(onClick = { actions.openUrl(BillingRepository.MANAGE_SUBSCRIPTIONS_URL) }) {
                Text("Aboneliği yönet veya iptal et")
            }
            Text(
                "Abonelik, iptal edene kadar her dönemin sonunda gösterilen ücretle otomatik yenilenir " +
                    "ve ödeme Google Play hesabından alınır. İstediğin zaman Google Play > Ödemeler ve " +
                    "abonelikler bölümünden iptal edebilirsin; iptal edersen Pro dönem sonuna kadar açık " +
                    "kalır. Ömür boyu seçeneği tek seferlik ödemedir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OfferButton(offer: BillingRepository.Offer, highlighted: Boolean, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text("${offer.title} · ${offer.price}", fontWeight = FontWeight.Bold)
            Text(offer.note, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (highlighted) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(),
        ) { content() }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
    }
}
