package com.holyware.coinsdash.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.holyware.coinsdash.data.DashboardSnapshot
import com.holyware.coinsdash.data.Delisting
import com.holyware.coinsdash.data.Trade
import com.holyware.coinsdash.ui.components.DelistingRow
import com.holyware.coinsdash.ui.components.EmptyCard
import com.holyware.coinsdash.ui.components.localTime
import com.holyware.coinsdash.ui.components.marketDisplayName
import com.holyware.coinsdash.ui.components.number
import com.holyware.coinsdash.ui.components.won
import com.holyware.coinsdash.ui.preview.PreviewData
import com.holyware.coinsdash.ui.theme.CoinSDashTheme

@Composable
internal fun HistoryScreen(snapshot: DashboardSnapshot?) {
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
    if (trades.isEmpty()) {
        EmptyCard("거래내역이 없습니다.")
        return
    }
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

@Composable
private fun DelistingList(items: List<Delisting>) {
    if (items.isEmpty()) {
        EmptyCard("개발 완료 이후 등록해제 기록이 여기에 누적됩니다.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.market + it.occurredAt }) { DelistingRow(it) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun HistoryScreenPreview() {
    CoinSDashTheme(dynamicColor = false) { HistoryScreen(PreviewData.snapshot) }
}
