package com.holyware.coinsdash.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class DashboardRepository(context: Context) {
    private val preferences = context.getSharedPreferences("coinsdash", Context.MODE_PRIVATE)

    fun loadSettings() = ConnectionSettings(
        baseUrl = preferences.getString("base_url", "") ?: "",
        dashboardToken = preferences.getString("dashboard_token", "") ?: "",
    )

    fun saveSettings(settings: ConnectionSettings) {
        preferences.edit()
            .putString("base_url", settings.baseUrl.trim().trimEnd('/'))
            .putString("dashboard_token", settings.dashboardToken.trim())
            .apply()
    }

    fun fetchDashboard(settings: ConnectionSettings): DashboardSnapshot {
        require(settings.baseUrl.startsWith("https://")) { "서버 주소는 HTTPS여야 합니다." }
        val json = request(settings, "GET", "/api/v1/dashboard")
        val bot = json.getJSONObject("bot")
        val money = json.getJSONObject("money")
        return DashboardSnapshot(
            generatedAt = json.optString("generated_at"),
            bot = BotStatus(bot.optBoolean("alive"), bot.optString("last_heartbeat"), bot.nullableString("error")),
            money = MoneySummary(
                money.optDouble("investment"), money.optDouble("cash"), money.optDouble("purchase_cost"),
                money.optDouble("total_assets"), money.optDouble("coin_value"),
            ),
            buyTracking = json.optInt("buy_tracking"),
            sellTracking = json.optInt("sell_tracking"),
            registered = json.optJSONArray("registered").objects().map {
                CoinStatus(it.getString("market"), it.optBoolean("buy_active"), it.optBoolean("held"))
            },
            delistings = json.optJSONArray("delistings").objects().map {
                Delisting(it.getString("market"), it.getString("reason"), it.getString("occurred_at"))
            }.sortedByDescending { parseInstant(it.occurredAt) },
            trades = json.optJSONArray("trades").objects().map {
                Trade(
                    it.getString("uuid"), it.getString("market"), it.getString("side"),
                    it.optDouble("price"), it.optDouble("volume"), it.optDouble("funds"),
                    it.optDouble("fee"), it.getString("executed_at"),
                    if (it.has("net_profit") && !it.isNull("net_profit")) it.getDouble("net_profit") else null,
                )
            }.sortedByDescending { parseInstant(it.executedAt) },
        )
    }

    fun updateUpbitKeys(settings: ConnectionSettings, accessKey: String, secretKey: String) {
        require(accessKey.isNotBlank() && secretKey.isNotBlank()) { "Access Key와 Secret Key를 모두 입력하세요." }
        request(
            settings,
            "PUT",
            "/api/v1/credentials",
            JSONObject().put("access_key", accessKey.trim()).put("secret_key", secretKey.trim()).toString(),
        )
    }

    private fun request(settings: ConnectionSettings, method: String, path: String, body: String? = null): JSONObject {
        require(settings.dashboardToken.isNotBlank()) { "대시보드 인증 토큰을 입력하세요." }
        val connection = URL(settings.baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Authorization", "Bearer ${settings.dashboardToken}")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("서버 오류 $code: ${text.take(300)}")
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
private fun JSONObject.nullableString(name: String): String? = if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
private fun parseInstant(value: String): Instant = runCatching { Instant.parse(value) }.getOrDefault(Instant.EPOCH)
