package com.holyware.coinsdash.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.holyware.coinsdash.AuthStatus
import com.holyware.coinsdash.DashboardViewModel
import com.holyware.coinsdash.ui.screens.AuthenticationLoadingScreen
import com.holyware.coinsdash.ui.screens.CoinsScreen
import com.holyware.coinsdash.ui.screens.HistoryScreen
import com.holyware.coinsdash.ui.screens.LoginScreen
import com.holyware.coinsdash.ui.screens.OverviewScreen
import com.holyware.coinsdash.ui.screens.SettingsScreen

private enum class Screen(val label: String, val symbol: String) {
    Overview("현황", "●"), Coins("코인", "◆"), History("거래", "↕"), Settings("설정", "⚙")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinSDashApp(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsState()
    when (state.auth.status) {
        AuthStatus.CHECKING -> {
            AuthenticationLoadingScreen()
            return
        }
        AuthStatus.SIGNED_OUT -> {
            LoginScreen(state, viewModel::signIn)
            return
        }
        AuthStatus.AUTHENTICATED -> Unit
    }
    var selected by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Coinsdance", fontWeight = FontWeight.Bold)
                        Text("CoinSDance 실시간 관제", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    if (state.connection.refreshing) {
                        CircularProgressIndicator(Modifier.padding(14.dp).height(22.dp), strokeWidth = 2.dp)
                    }
                    TextButton(onClick = viewModel::refresh) { Text("새로고침") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Text(screen.symbol) },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (Screen.entries[selected]) {
                Screen.Overview -> OverviewScreen(state)
                Screen.Coins -> CoinsScreen(state.snapshot)
                Screen.History -> HistoryScreen(state.snapshot)
                Screen.Settings -> SettingsScreen(state, viewModel::signOut, viewModel::updateKeys)
            }
        }
    }
}
