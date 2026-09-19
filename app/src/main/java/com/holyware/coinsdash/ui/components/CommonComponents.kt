package com.holyware.coinsdash.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.holyware.coinsdash.data.Delisting
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val wonFormat = NumberFormat.getNumberInstance(Locale.KOREA).apply { maximumFractionDigits = 0 }
private val numberFormat = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 8 }
private val localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")

internal fun won(value: Double): String = "₩${wonFormat.format(value)}"
internal fun number(value: Double): String = numberFormat.format(value)
internal fun marketDisplayName(market: String): String = market.removePrefix("KRW-")

internal fun localTime(value: String?): String {
    if (value.isNullOrBlank()) return "확인 안 됨"
    return runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).format(localFormatter) }.getOrDefault(value)
}

private fun reasonLabel(reason: String): String = when (reason) {
    "UPBIT_CAUTION" -> "업비트 유의 지정"
    "COINBASE_DELISTED" -> "Coinbase 상장 폐지"
    else -> reason
}

@Composable
internal fun EmptyCard(message: String) {
    Card { Text(message, Modifier.fillMaxWidth().padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
internal fun DelistingRow(item: Delisting) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(marketDisplayName(item.market), fontWeight = FontWeight.Bold)
                Text(reasonLabel(item.reason), color = MaterialTheme.colorScheme.error)
            }
            Text(localTime(item.occurredAt), style = MaterialTheme.typography.labelSmall)
        }
    }
}
