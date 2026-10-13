package tr.com.covolt.aramaengelleyici.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun AppRoot(state: UiState, actions: AppActions) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        actions.messageShown()
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
    val goHome = { screen = Screen.HOME }

    when (screen) {
        Screen.HOME -> HomeScreen(state, actions, snackbar, onNavigate = { screen = it })
        Screen.HISTORY -> HistoryScreen(state, actions, snackbar, onBack = goHome)
        Screen.LISTS -> ListsScreen(
            state, actions, snackbar,
            onBack = goHome,
            onOpenPro = { screen = Screen.PRO },
        )
        Screen.PRO -> ProScreen(state, actions, snackbar, onBack = goHome)
    }
}
