package com.holyware.coinsdash.ui.preview

import com.holyware.coinsdash.AuthStatus
import com.holyware.coinsdash.AuthUiState
import com.holyware.coinsdash.ConnectionStatus
import com.holyware.coinsdash.ConnectionUiState
import com.holyware.coinsdash.DashboardUiState
import com.holyware.coinsdash.data.BotStatus
import com.holyware.coinsdash.data.CoinStatus
import com.holyware.coinsdash.data.DashboardSnapshot
import com.holyware.coinsdash.data.Delisting
import com.holyware.coinsdash.data.MoneySummary
import com.holyware.coinsdash.data.Trade

internal object PreviewData {
    val coins = listOf(
        CoinStatus("KRW-AAVE", true, true, 312_450.0, 356_820.0, 14.20),
        CoinStatus("KRW-BTC", false, true, 1_240_000.0, 1_181_000.0, -4.76),
        CoinStatus("KRW-ETH", true, true, 248_000.0, 252_500.0, 1.81),
        CoinStatus("KRW-ORCA", true, false, 0.0, 0.0, 0.0),
        CoinStatus("KRW-PUMP", false, true, 187_500.0, 92_300.0, -50.77),
    )
    val snapshot = DashboardSnapshot(
        generatedAt = "2026-09-18T20:44:33Z",
        bot = BotStatus(true, "2026-09-18T20:44:33Z"),
        money = MoneySummary(32_100_000.0, 5_400_000.0, 26_700_000.0, 31_800_000.0, 26_400_000.0),
        buyTracking = 12,
        sellTracking = 8,
        buyHooked = 3,
        sellHooked = 2,
        minimumBuy = 5_645.0,
        registered = coins,
        delistings = listOf(Delisting("KRW-INJ", "UPBIT_CAUTION", "2026-09-01T12:00:00Z")),
        trades = listOf(
            Trade("1", "KRW-AAVE", "buy", 320_000.0, 0.02, 6_400.0, 3.2, "2026-09-18T19:00:00Z"),
            Trade("2", "KRW-BTC", "sell", 92_000_000.0, 0.001, 92_000.0, 46.0, "2026-09-18T18:00:00Z", 1_320.0),
        ),
    )
    val state = DashboardUiState(
        snapshot = snapshot,
        auth = AuthUiState(AuthStatus.AUTHENTICATED, "preview@coinsdance.app", "preview-user"),
        connection = ConnectionUiState(ConnectionStatus.CONNECTED),
    )
}
