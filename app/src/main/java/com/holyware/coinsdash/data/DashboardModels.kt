package com.holyware.coinsdash.data

data class MoneySummary(
    val investment: Double = 0.0,
    val cash: Double = 0.0,
    val purchaseCost: Double = 0.0,
    val totalAssets: Double = 0.0,
    val coinValue: Double = 0.0,
)

data class BotStatus(
    val alive: Boolean = false,
    val lastHeartbeat: String = "",
    val error: String? = null,
)

data class CoinStatus(
    val market: String,
    val buyActive: Boolean,
    val held: Boolean,
)

data class Delisting(
    val market: String,
    val reason: String,
    val occurredAt: String,
)

data class Trade(
    val uuid: String,
    val market: String,
    val side: String,
    val price: Double,
    val volume: Double,
    val funds: Double,
    val fee: Double,
    val executedAt: String,
    val netProfit: Double? = null,
)

data class DashboardSnapshot(
    val generatedAt: String = "",
    val bot: BotStatus = BotStatus(),
    val money: MoneySummary = MoneySummary(),
    val buyTracking: Int = 0,
    val sellTracking: Int = 0,
    val registered: List<CoinStatus> = emptyList(),
    val delistings: List<Delisting> = emptyList(),
    val trades: List<Trade> = emptyList(),
)

data class ConnectionSettings(val baseUrl: String = "", val dashboardToken: String = "")
