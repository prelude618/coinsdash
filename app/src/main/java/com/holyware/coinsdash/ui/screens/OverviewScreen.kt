package com.holyware.coinsdash.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.holyware.coinsdash.AuthStatus
import com.holyware.coinsdash.ConnectionStatus
import com.holyware.coinsdash.DashboardUiState
import com.holyware.coinsdash.data.DashboardSnapshot
import com.holyware.coinsdash.ui.components.DelistingRow
import com.holyware.coinsdash.ui.components.EmptyCard
import com.holyware.coinsdash.ui.components.localTime
import com.holyware.coinsdash.ui.components.won
import com.holyware.coinsdash.ui.preview.PreviewData
import com.holyware.coinsdash.ui.theme.CoinSDashTheme

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
internal fun OverviewScreen(state: DashboardUiState) {
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

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun OverviewScreenPreview() {
    CoinSDashTheme(dynamicColor = false) { OverviewScreen(PreviewData.state) }
}
