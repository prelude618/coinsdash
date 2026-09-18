package com.holyware.coinsdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holyware.coinsdash.data.CoinStatus
import com.holyware.coinsdash.data.DashboardSnapshot
import com.holyware.coinsdash.data.Delisting
import com.holyware.coinsdash.data.Trade
import com.holyware.coinsdash.ui.theme.CoinSDashTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CoinSDashTheme { CoinSDashApp(dashboardViewModel) } }
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel.onForeground()
    }
}

private enum class Screen(val label: String, val symbol: String) {
    Overview("현황", "●"), Coins("코인", "◆"), History("거래", "↕"), Settings("설정", "⚙")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinSDashApp(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    when (state.auth.status) {
        AuthStatus.CHECKING -> {
            AuthenticationLoadingScreen()
            return
        }
        AuthStatus.SIGNED_OUT -> {
            LoginScreen(state, viewModel)
            return
        }
        AuthStatus.AUTHENTICATED -> Unit
    }
    var selected by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Coinsdance", fontWeight = FontWeight.Bold); Text("CoinSDance 실시간 관제", style = MaterialTheme.typography.labelSmall) } },
                actions = {
                    if (state.connection.refreshing) CircularProgressIndicator(Modifier.padding(14.dp).height(22.dp), strokeWidth = 2.dp)
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
                Screen.Settings -> SettingsScreen(state, viewModel)
            }
        }
    }
}

@Composable
private fun AuthenticationLoadingScreen() {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Google 로그인 확인 중")
        }
    }
}

@Composable
private fun LoginScreen(state: DashboardUiState, viewModel: DashboardViewModel) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Coinsdance", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("CoinSDance 실시간 관제", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(
                enabled = state.auth.status != AuthStatus.CHECKING,
                onClick = { viewModel.signIn(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Google로 로그인")
            }
            if (!state.auth.error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(state.auth.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OverviewScreen(state: DashboardUiState) {
    val snapshot = state.snapshot
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(4.dp)) }
        item { BotCard(state) }
        val money = snapshot?.money
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyCard("총투자금액", money?.investment ?: 0.0, Modifier.weight(1f), true)
                MoneyCard("총보유자산", money?.totalAssets ?: 0.0, Modifier.weight(1f), true)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyCard("보유현금", money?.cash ?: 0.0, Modifier.weight(1f))
                MoneyCard("총매수원가", money?.purchaseCost ?: 0.0, Modifier.weight(1f))
            }
        }
        item { MoneyCard("총코인평가액", money?.coinValue ?: 0.0, Modifier.fillMaxWidth()) }
        item {
            val buyCounts = buyTrackingDisplay(snapshot)
            val sellCounts = sellTrackingDisplay(snapshot)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountCard("매수 저점 추적", buyCounts.first, Color(0xFF2E7D32), Modifier.weight(1f), buyCounts.second)
                CountCard("매도 고점 추적", sellCounts.first, Color(0xFFC62828), Modifier.weight(1f), sellCounts.second)
            }
        }
        item {
            val active = snapshot?.registered?.count { it.buyActive } ?: 0
            val registered = snapshot?.registered?.size ?: 0
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountCard("등록 ${registered}개 · 신규 매수 대상", active, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MinimumBuyCard(snapshot?.minimumBuy ?: 0.0, Modifier.weight(1f))
            }
        }
        item { Text("최근 등록해제", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (snapshot?.delistings.isNullOrEmpty()) item { EmptyCard("기록된 등록해제 종목이 없습니다.") }
        else items(snapshot!!.delistings.take(5), key = { it.market + it.occurredAt }) { DelistingRow(it) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

internal fun buyTrackingDisplay(snapshot: DashboardSnapshot?): Pair<Int, Int> =
    (snapshot?.buyHooked ?: 0) to (snapshot?.buyTracking ?: 0)

internal fun sellTrackingDisplay(snapshot: DashboardSnapshot?): Pair<Int, Int> =
    (snapshot?.sellHooked ?: 0) to (snapshot?.sellTracking ?: 0)

internal enum class BotPresentation { HEALTHY, CHECKING, NEEDS_LOGIN, OUTAGE }

internal fun botPresentation(state: DashboardUiState): BotPresentation {
    if (state.auth.status != AuthStatus.AUTHENTICATED) return BotPresentation.NEEDS_LOGIN
    return when (state.connection.status) {
        ConnectionStatus.LOADING -> BotPresentation.CHECKING
        ConnectionStatus.ERROR -> BotPresentation.OUTAGE
        ConnectionStatus.CONNECTED -> when (state.snapshot?.bot?.alive) {
            true -> BotPresentation.HEALTHY
            false -> BotPresentation.OUTAGE
            null -> BotPresentation.CHECKING
        }
    }
}

@Composable
private fun BotCard(state: DashboardUiState) {
    val bot = state.snapshot?.bot
    val presentation = botPresentation(state)
    val color = when (presentation) {
        BotPresentation.HEALTHY -> Color(0xFF16803A)
        BotPresentation.CHECKING, BotPresentation.NEEDS_LOGIN -> MaterialTheme.colorScheme.primary
        BotPresentation.OUTAGE -> MaterialTheme.colorScheme.error
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .11f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(color, RoundedCornerShape(50)).padding(5.dp))
                Text(
                    when (presentation) {
                        BotPresentation.HEALTHY -> "  봇 정상 실행 중"
                        BotPresentation.CHECKING -> "  봇 상태 확인 중"
                        BotPresentation.NEEDS_LOGIN -> "  Google 로그인 필요"
                        BotPresentation.OUTAGE -> "  봇 장애 또는 연결 끊김"
                    },
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
            val error = if (presentation == BotPresentation.OUTAGE) state.connection.error ?: bot?.error else bot?.error
            if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (bot != null) Text("마지막 신호: ${localTime(bot.lastHeartbeat)}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MoneyCard(label: String, value: Double, modifier: Modifier, emphasized: Boolean = false) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(won(value), style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CountCard(label: String, value: Int, color: Color, modifier: Modifier, hooked: Int? = null) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f))) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${value}개${hooked?.let { " ($it)" }.orEmpty()}", style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MinimumBuyCard(value: Double, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .10f))) {
        Column(Modifier.padding(14.dp)) {
            Text("최소 1회 매수금액", style = MaterialTheme.typography.labelMedium)
            Text(if (value > 0) won(value) else "-", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

internal enum class CoinBooleanFilter { ALL, YES, NO }
internal enum class CoinChangeFilter { ALL, GAIN, LOSS }
internal enum class CoinSortField { SYMBOL, HELD, BUY_ACTIVE, PURCHASE_COST, CURRENT_VALUE, CHANGE_PERCENT }

internal fun filterAndSortCoins(
    coins: List<CoinStatus>,
    query: String = "",
    held: CoinBooleanFilter = CoinBooleanFilter.ALL,
    buyActive: CoinBooleanFilter = CoinBooleanFilter.ALL,
    change: CoinChangeFilter = CoinChangeFilter.ALL,
    minimumPurchaseCost: Double = 0.0,
    minimumCurrentValue: Double = 0.0,
    sortField: CoinSortField = CoinSortField.SYMBOL,
    descending: Boolean = false,
): List<CoinStatus> {
    val filtered = coins.filter { coin ->
        val symbol = marketDisplayName(coin.market)
        symbol.contains(query.trim(), ignoreCase = true) &&
            (held == CoinBooleanFilter.ALL || coin.held == (held == CoinBooleanFilter.YES)) &&
            (buyActive == CoinBooleanFilter.ALL || coin.buyActive == (buyActive == CoinBooleanFilter.YES)) &&
            (change == CoinChangeFilter.ALL || (change == CoinChangeFilter.GAIN && coin.changePercent >= 0) || (change == CoinChangeFilter.LOSS && coin.changePercent < 0)) &&
            coin.purchaseCost >= minimumPurchaseCost && coin.currentValue >= minimumCurrentValue
    }
    val comparator = when (sortField) {
        CoinSortField.SYMBOL -> compareBy<CoinStatus> { marketDisplayName(it.market).lowercase(Locale.US) }
        CoinSortField.HELD -> compareBy<CoinStatus> { it.held }
        CoinSortField.BUY_ACTIVE -> compareBy<CoinStatus> { it.buyActive }
        CoinSortField.PURCHASE_COST -> compareBy<CoinStatus> { it.purchaseCost }
        CoinSortField.CURRENT_VALUE -> compareBy<CoinStatus> { it.currentValue }
        CoinSortField.CHANGE_PERCENT -> compareBy<CoinStatus> { it.changePercent }
    }.thenBy { it.market }
    return filtered.sortedWith(if (descending) comparator.reversed() else comparator)
}

@Composable
private fun CoinsScreen(snapshot: DashboardSnapshot?) {
    var query by remember { mutableStateOf("") }
    var heldFilter by remember { mutableStateOf(CoinBooleanFilter.ALL) }
    var buyFilter by remember { mutableStateOf(CoinBooleanFilter.ALL) }
    var changeFilter by remember { mutableStateOf(CoinChangeFilter.ALL) }
    var minimumCost by remember { mutableStateOf("") }
    var minimumValue by remember { mutableStateOf("") }
    var sortField by remember { mutableStateOf(CoinSortField.SYMBOL) }
    var descending by remember { mutableStateOf(false) }
    val coins = filterAndSortCoins(
        coins = snapshot?.registered.orEmpty(),
        query = query,
        held = heldFilter,
        buyActive = buyFilter,
        change = changeFilter,
        minimumPurchaseCost = minimumCost.toDoubleOrNull() ?: 0.0,
        minimumCurrentValue = minimumValue.toDoubleOrNull() ?: 0.0,
        sortField = sortField,
        descending = descending,
    )
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("등록 코인", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { Text("모든 금액과 등락률은 업비트 실잔고·공식 평단·현재가 기준입니다.", style = MaterialTheme.typography.bodySmall) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("코인 검색") },
                placeholder = { Text("예: AAVE") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text("보유 여부", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CoinBooleanFilter.entries) { option ->
                    FilterChip(
                        selected = heldFilter == option,
                        onClick = { heldFilter = option },
                        label = { Text(when (option) { CoinBooleanFilter.ALL -> "전체"; CoinBooleanFilter.YES -> "보유"; CoinBooleanFilter.NO -> "미보유" }) },
                    )
                }
            }
        }
        item {
            Text("매수 대상 여부", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CoinBooleanFilter.entries) { option ->
                    FilterChip(
                        selected = buyFilter == option,
                        onClick = { buyFilter = option },
                        label = { Text(when (option) { CoinBooleanFilter.ALL -> "전체"; CoinBooleanFilter.YES -> "매수 대상"; CoinBooleanFilter.NO -> "비대상" }) },
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minimumCost,
                    onValueChange = { minimumCost = it.filter(Char::isDigit) },
                    label = { Text("최소 매수원가") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = minimumValue,
                    onValueChange = { minimumValue = it.filter(Char::isDigit) },
                    label = { Text("최소 평가액") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text("등락률", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CoinChangeFilter.entries) { option ->
                    FilterChip(
                        selected = changeFilter == option,
                        onClick = { changeFilter = option },
                        label = { Text(when (option) { CoinChangeFilter.ALL -> "전체"; CoinChangeFilter.GAIN -> "보합·상승"; CoinChangeFilter.LOSS -> "하락" }) },
                    )
                }
            }
        }
        item {
            Text("정렬 · 선택한 항목을 다시 누르면 방향 전환", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CoinSortField.entries) { option ->
                    val selected = sortField == option
                    val label = when (option) {
                        CoinSortField.SYMBOL -> "코인"
                        CoinSortField.HELD -> "보유"
                        CoinSortField.BUY_ACTIVE -> "매수대상"
                        CoinSortField.PURCHASE_COST -> "매수원가"
                        CoinSortField.CURRENT_VALUE -> "평가액"
                        CoinSortField.CHANGE_PERCENT -> "등락률"
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) descending = !descending else {
                                sortField = option
                                descending = option != CoinSortField.SYMBOL
                            }
                        },
                        label = { Text(label + if (selected) if (descending) " ↓" else " ↑" else "") },
                    )
                }
            }
            Text("${coins.size}개 표시", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (coins.isEmpty()) item { EmptyCard("등록 코인 데이터가 없습니다.") }
        items(coins, key = { it.market }) { coin ->
            Card(colors = CardDefaults.cardColors(containerColor = if (coin.buyActive) Color(0xFF1B5E20).copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(marketDisplayName(coin.market), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (coin.held) "보유" else "미보유", color = if (coin.held) Color(0xFF16803A) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(" · ")
                        Text(if (coin.buyActive) "매수 대상" else "비대상", color = if (coin.buyActive) Color(0xFF16803A) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoinMetric("총매수원가", won(coin.purchaseCost), Modifier.weight(1f))
                        CoinMetric("현재평가액", won(coin.currentValue), Modifier.weight(1f))
                        CoinMetric(
                            "등락률",
                            if (coin.held) String.format(Locale.US, "%+.2f%%", coin.changePercent) else "-",
                            Modifier.weight(1f),
                            when { coin.changePercent > 0 -> Color(0xFFC62828); coin.changePercent < 0 -> Color(0xFF1565C0); else -> MaterialTheme.colorScheme.onSurfaceVariant },
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CoinMetric(label: String, value: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryScreen(snapshot: DashboardSnapshot?) {
    var delistingMode by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!delistingMode) Button(onClick = { delistingMode = false }, modifier = Modifier.weight(1f)) { Text("거래내역") }
            else OutlinedButton(onClick = { delistingMode = false }, modifier = Modifier.weight(1f)) { Text("거래내역") }
            if (delistingMode) Button(onClick = { delistingMode = true }, modifier = Modifier.weight(1f)) { Text("등록해제") }
            else OutlinedButton(onClick = { delistingMode = true }, modifier = Modifier.weight(1f)) { Text("등록해제") }
        }
        if (delistingMode) DelistingList(snapshot?.delistings.orEmpty()) else TradeList(snapshot?.trades.orEmpty())
    }
}

@Composable
private fun TradeList(trades: List<Trade>) {
    if (trades.isEmpty()) { EmptyCard("거래내역이 없습니다."); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(trades, key = { it.uuid }) { trade ->
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(marketDisplayName(trade.market), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(if (trade.side == "buy") "매수" else "매도", color = if (trade.side == "buy") Color(0xFFC62828) else Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                    }
                    Text("${won(trade.funds)} · ${number(trade.volume)}개 @ ${won(trade.price)}")
                    Text("수수료 ${won(trade.fee)} · ${localTime(trade.executedAt)}", style = MaterialTheme.typography.labelSmall)
                    if (trade.side == "sell") {
						val profit = trade.netProfit
						Text(
							if (profit == null) "실수익 집계 전" else "실수익 ${if (profit >= 0) "+" else ""}${won(profit)}",
							color = when { profit == null -> MaterialTheme.colorScheme.onSurfaceVariant; profit >= 0 -> Color(0xFFC62828); else -> Color(0xFF1565C0) },
							fontWeight = FontWeight.Bold,
						)
					}
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

internal fun marketDisplayName(market: String): String = market.removePrefix("KRW-")

@Composable
private fun DelistingList(items: List<Delisting>) {
    if (items.isEmpty()) { EmptyCard("개발 완료 이후 등록해제 기록이 여기에 누적됩니다."); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.market + it.occurredAt }) { DelistingRow(it) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun DelistingRow(item: Delisting) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(item.market, fontWeight = FontWeight.Bold); Text(reasonLabel(item.reason), color = MaterialTheme.colorScheme.error) }
            Text(localTime(item.occurredAt), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SettingsScreen(state: DashboardUiState, viewModel: DashboardViewModel) {
    var showKeys by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Google 계정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { Text(state.auth.email.orEmpty(), fontWeight = FontWeight.Bold) }
        item { OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) { Text("로그아웃") } }
        item { HorizontalDivider() }
        item { Text("업비트 API 키", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Text("키는 앱에 저장하지 않고 HTTPS로 서버에 한 번 전달합니다. 서버는 새 키를 검증한 후 교체해야 합니다.", style = MaterialTheme.typography.bodySmall) }
        item { OutlinedButton(onClick = { showKeys = true }, modifier = Modifier.fillMaxWidth()) { Text("API 키 갱신") } }
    }
    if (showKeys) KeyDialog(viewModel) { showKeys = false }
}

@Composable
private fun KeyDialog(viewModel: DashboardViewModel, dismiss: () -> Unit) {
    var access by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!saving) dismiss() },
        title = { Text("업비트 API 키 갱신") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(access, { access = it }, label = { Text("Access Key") }, singleLine = true)
                OutlinedTextField(secret, { secret = it }, label = { Text("Secret Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(enabled = !saving && access.isNotBlank() && secret.isNotBlank(), onClick = {
                saving = true
                scope.launch {
                    viewModel.updateKeys(access, secret).onSuccess { access = ""; secret = ""; dismiss() }.onFailure { error = it.message }
                    saving = false
                }
            }) { Text(if (saving) "검증 중…" else "검증 후 교체") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text("취소") } },
    )
}

@Composable
private fun EmptyCard(message: String) { Card { Text(message, Modifier.fillMaxWidth().padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }

private val wonFormat = NumberFormat.getNumberInstance(Locale.KOREA).apply { maximumFractionDigits = 0 }
private val numberFormat = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 8 }
private val localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
private fun won(value: Double) = "₩${wonFormat.format(value)}"
private fun number(value: Double) = numberFormat.format(value)
private fun localTime(value: String?): String {
    if (value.isNullOrBlank()) return "확인 안 됨"
    return runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).format(localFormatter) }.getOrDefault(value)
}
private fun reasonLabel(reason: String) = when (reason) {
    "UPBIT_CAUTION" -> "업비트 유의 지정"
    "COINBASE_DELISTED" -> "Coinbase 상장 폐지"
    else -> reason
}
