package com.holyware.coinsdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holyware.coinsdash.data.BotStatus
import com.holyware.coinsdash.data.CoinStatus
import com.holyware.coinsdash.data.ConnectionSettings
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CoinSDashTheme { CoinSDashApp() } }
    }
}

private enum class Screen(val label: String, val symbol: String) {
    Overview("현황", "●"), Coins("코인", "◆"), History("거래", "↕"), Settings("설정", "⚙")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinSDashApp(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("CoinSDash", fontWeight = FontWeight.Bold); Text("CoinSDance 실시간 관제", style = MaterialTheme.typography.labelSmall) } },
                actions = {
                    if (state.loading) CircularProgressIndicator(Modifier.padding(14.dp).height(22.dp), strokeWidth = 2.dp)
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
                Screen.Overview -> OverviewScreen(state.snapshot, state.connectionError)
                Screen.Coins -> CoinsScreen(state.snapshot)
                Screen.History -> HistoryScreen(state.snapshot)
                Screen.Settings -> SettingsScreen(state.settings, viewModel)
            }
            if (state.settings.baseUrl.isBlank()) {
                Surface(Modifier.align(Alignment.BottomCenter).padding(16.dp), color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Text("설정에서 CoinSDance 서버 주소와 인증 토큰을 입력하세요.", Modifier.padding(14.dp))
                }
            }
        }
    }
}

@Composable
private fun OverviewScreen(snapshot: DashboardSnapshot?, connectionError: String?) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(4.dp)) }
        item { BotCard(snapshot?.bot, connectionError) }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountCard("매수 저점 추적", snapshot?.buyTracking ?: 0, Color(0xFF2E7D32), Modifier.weight(1f))
                CountCard("매도 고점 추적", snapshot?.sellTracking ?: 0, Color(0xFFC62828), Modifier.weight(1f))
            }
        }
        item {
            val active = snapshot?.registered?.count { it.buyActive } ?: 0
            val registered = snapshot?.registered?.size ?: 0
            CountCard("등록 ${registered}개 · 신규 매수 대상", active, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth())
        }
        item { Text("최근 등록해제", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (snapshot?.delistings.isNullOrEmpty()) item { EmptyCard("기록된 등록해제 종목이 없습니다.") }
        else items(snapshot!!.delistings.take(5), key = { it.market + it.occurredAt }) { DelistingRow(it) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun BotCard(bot: BotStatus?, connectionError: String?) {
    val alive = bot?.alive == true && connectionError == null
    val color = if (alive) Color(0xFF16803A) else MaterialTheme.colorScheme.error
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .11f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(color, RoundedCornerShape(50)).padding(5.dp))
                Text(if (alive) "  봇 정상 실행 중" else "  봇 장애 또는 연결 끊김", fontWeight = FontWeight.Bold, color = color)
            }
            val error = connectionError ?: bot?.error
            if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Text("마지막 신호: ${localTime(bot?.lastHeartbeat)}", style = MaterialTheme.typography.labelMedium)
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
private fun CountCard(label: String, value: Int, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f))) {
        Column(Modifier.padding(14.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Text("${value}개", style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun CoinsScreen(snapshot: DashboardSnapshot?) {
    val coins = snapshot?.registered.orEmpty().sortedWith(compareByDescending<CoinStatus> { it.buyActive }.thenBy { it.market })
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("등록 코인", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { Text("초록색은 현재 신규 매수 대상입니다. 순위 밖 보유 종목도 매도 관리는 계속됩니다.", style = MaterialTheme.typography.bodySmall) }
        if (coins.isEmpty()) item { EmptyCard("등록 코인 데이터가 없습니다.") }
        items(coins, key = { it.market }) { coin ->
            Card(colors = CardDefaults.cardColors(containerColor = if (coin.buyActive) Color(0xFF1B5E20).copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(coin.market.removePrefix("KRW-"), fontWeight = FontWeight.Bold); Text(coin.market, style = MaterialTheme.typography.labelSmall) }
                    if (coin.held) Text("보유  ", style = MaterialTheme.typography.labelMedium)
                    Text(if (coin.buyActive) "매수 대상" else "매도 관리", color = if (coin.buyActive) Color(0xFF16803A) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
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
                        Text(trade.market, Modifier.weight(1f), fontWeight = FontWeight.Bold)
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
private fun SettingsScreen(settings: ConnectionSettings, viewModel: DashboardViewModel) {
    var baseUrl by remember(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var token by remember(settings.dashboardToken) { mutableStateOf(settings.dashboardToken) }
    var showKeys by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("서버 연결", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("HTTPS 서버 주소") }, placeholder = { Text("https://dash.example.com") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)) }
        item { OutlinedTextField(token, { token = it }, label = { Text("대시보드 인증 토큰") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation()) }
        item { Button(onClick = { viewModel.saveSettings(ConnectionSettings(baseUrl, token)) }, modifier = Modifier.fillMaxWidth()) { Text("연결 설정 저장") } }
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
