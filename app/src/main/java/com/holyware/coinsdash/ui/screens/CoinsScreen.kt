package com.holyware.coinsdash.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.holyware.coinsdash.data.CoinStatus
import com.holyware.coinsdash.data.DashboardSnapshot
import com.holyware.coinsdash.ui.components.marketDisplayName
import com.holyware.coinsdash.ui.components.won
import com.holyware.coinsdash.ui.preview.PreviewData
import com.holyware.coinsdash.ui.theme.CoinSDashTheme
import java.util.Locale

internal enum class BooleanColumnFilter { ALL, YES, NO }
internal enum class CoinSortField { PURCHASE_COST, CURRENT_VALUE, CHANGE_PERCENT }
internal enum class SortDirection { ASCENDING, DESCENDING }
internal data class CoinSort(val field: CoinSortField, val direction: SortDirection)

internal fun toggleCoinSort(sorts: List<CoinSort>, field: CoinSortField): List<CoinSort> {
    val index = sorts.indexOfFirst { it.field == field }
    if (index < 0) return sorts + CoinSort(field, SortDirection.DESCENDING)
    val selected = sorts[index]
    return when (selected.direction) {
        SortDirection.DESCENDING -> sorts.toMutableList().also { it[index] = selected.copy(direction = SortDirection.ASCENDING) }
        SortDirection.ASCENDING -> sorts.filterNot { it.field == field }
    }
}

internal fun filterAndSortCoins(
    coins: List<CoinStatus>,
    held: BooleanColumnFilter = BooleanColumnFilter.ALL,
    buyActive: BooleanColumnFilter = BooleanColumnFilter.ALL,
    sorts: List<CoinSort> = emptyList(),
): List<CoinStatus> {
    val filtered = coins.filter { coin ->
        (held == BooleanColumnFilter.ALL || coin.held == (held == BooleanColumnFilter.YES)) &&
            (buyActive == BooleanColumnFilter.ALL || coin.buyActive == (buyActive == BooleanColumnFilter.YES))
    }
    if (sorts.isEmpty()) return filtered
    return filtered.sortedWith { left, right ->
        for (sort in sorts) {
            val result = when (sort.field) {
                CoinSortField.PURCHASE_COST -> left.purchaseCost.compareTo(right.purchaseCost)
                CoinSortField.CURRENT_VALUE -> left.currentValue.compareTo(right.currentValue)
                CoinSortField.CHANGE_PERCENT -> left.changePercent.compareTo(right.changePercent)
            }
            if (result != 0) return@sortedWith if (sort.direction == SortDirection.ASCENDING) result else -result
        }
        left.market.compareTo(right.market)
    }
}

private val coinWidth = 70.dp
private val heldWidth = 82.dp
private val buyActiveWidth = 88.dp
private val moneyWidth = 112.dp
private val changeWidth = 82.dp
private val scrollingTableWidth = moneyWidth + moneyWidth + changeWidth + heldWidth + buyActiveWidth

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CoinsScreen(snapshot: DashboardSnapshot?) {
    var heldFilter by remember { mutableStateOf(BooleanColumnFilter.ALL) }
    var buyActiveFilter by remember { mutableStateOf(BooleanColumnFilter.ALL) }
    var sorts by remember { mutableStateOf(emptyList<CoinSort>()) }
    val coins = filterAndSortCoins(snapshot?.registered.orEmpty(), heldFilter, buyActiveFilter, sorts)
    val horizontalScroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("등록 코인", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${coins.size}개", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            stickyHeader {
                CoinTableHeader(
                    heldFilter = heldFilter,
                    buyActiveFilter = buyActiveFilter,
                    sorts = sorts,
                    horizontalScroll = horizontalScroll,
                    onHeldFilter = { heldFilter = it },
                    onBuyActiveFilter = { buyActiveFilter = it },
                    onSort = { sorts = toggleCoinSort(sorts, it) },
                )
            }
            itemsIndexed(coins, key = { _, coin -> coin.market }) { index, coin ->
                CoinTableRow(coin, index % 2 == 1, horizontalScroll)
            }
        }
    }
}

@Composable
private fun CoinTableHeader(
    heldFilter: BooleanColumnFilter,
    buyActiveFilter: BooleanColumnFilter,
    sorts: List<CoinSort>,
    horizontalScroll: ScrollState,
    onHeldFilter: (BooleanColumnFilter) -> Unit,
    onBuyActiveFilter: (BooleanColumnFilter) -> Unit,
    onSort: (CoinSortField) -> Unit,
) {
    Surface(shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderLabel("코인명", coinWidth)
            Box(Modifier.weight(1f).horizontalScroll(horizontalScroll)) {
                Row(Modifier.width(scrollingTableWidth), verticalAlignment = Alignment.CenterVertically) {
                    SortHeader("총매수원가", moneyWidth, CoinSortField.PURCHASE_COST, sorts, onSort)
                    SortHeader("현재평가액", moneyWidth, CoinSortField.CURRENT_VALUE, sorts, onSort)
                    SortHeader("등락률", changeWidth, CoinSortField.CHANGE_PERCENT, sorts, onSort)
                    FilterHeader("보유유무", heldWidth, heldFilter, onHeldFilter)
                    FilterHeader("매수대상", buyActiveWidth, buyActiveFilter, onBuyActiveFilter)
                }
            }
        }
    }
}

@Composable
private fun HeaderLabel(label: String, width: Dp) {
    Text(label, Modifier.width(width).padding(horizontal = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun FilterHeader(
    label: String,
    width: Dp,
    selected: BooleanColumnFilter,
    onSelected: (BooleanColumnFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.width(width)) {
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                "$label\n${when (selected) { BooleanColumnFilter.ALL -> "전체"; BooleanColumnFilter.YES -> "유"; BooleanColumnFilter.NO -> "무" }} ▼",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                BooleanColumnFilter.ALL to "전체",
                BooleanColumnFilter.YES to "유",
                BooleanColumnFilter.NO to "무",
            ).forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onSelected(value); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SortHeader(
    label: String,
    width: Dp,
    field: CoinSortField,
    sorts: List<CoinSort>,
    onSort: (CoinSortField) -> Unit,
) {
    val index = sorts.indexOfFirst { it.field == field }
    val suffix = if (index < 0) " ↕" else " ${index + 1}${if (sorts[index].direction == SortDirection.ASCENDING) "↑" else "↓"}"
    TextButton(onClick = { onSort(field) }, modifier = Modifier.width(width)) {
        Text("$label$suffix", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CoinTableRow(
    coin: CoinStatus,
    alternate: Boolean,
    horizontalScroll: ScrollState,
) {
    val background = if (alternate) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f) else MaterialTheme.colorScheme.surface
    Row(
        Modifier.fillMaxWidth().background(background).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableText(marketDisplayName(coin.market), coinWidth, TextAlign.Start, FontWeight.Bold)
        Box(Modifier.weight(1f).horizontalScroll(horizontalScroll)) {
            Row(Modifier.width(scrollingTableWidth), verticalAlignment = Alignment.CenterVertically) {
                TableText(won(coin.purchaseCost), moneyWidth, TextAlign.End)
                TableText(won(coin.currentValue), moneyWidth, TextAlign.End)
                TableText(
                    if (coin.held) String.format(Locale.US, "%+.2f%%", coin.changePercent) else "-",
                    changeWidth,
                    TextAlign.End,
                    FontWeight.Bold,
                    when { coin.changePercent > 0 -> Color(0xFFC62828); coin.changePercent < 0 -> Color(0xFF1565C0); else -> MaterialTheme.colorScheme.onSurfaceVariant },
                )
                TableText(if (coin.held) "보유" else "미보유", heldWidth, TextAlign.Center, if (coin.held) FontWeight.Bold else FontWeight.Normal)
                TableText(if (coin.buyActive) "유" else "무", buyActiveWidth, TextAlign.Center, if (coin.buyActive) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun TableText(
    text: String,
    width: Dp,
    align: TextAlign,
    weight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 6.dp),
        textAlign = align,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = weight,
        color = color,
        maxLines = 1,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun CoinsScreenPreview() {
    CoinSDashTheme(dynamicColor = false) { CoinsScreen(PreviewData.snapshot) }
}
