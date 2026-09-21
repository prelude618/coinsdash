package com.holyware.coinsdash.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class DashboardRepository {
    data class UserProfile(
        val email: String,
        val username: String,
        val requiresUsername: Boolean,
        val requiresCredentials: Boolean,
        val disabled: Boolean,
    )

    fun fetchProfile(idToken: String): UserProfile = request(idToken, "GET", "/api/v1/profile").toUserProfile()

    fun setUsername(idToken: String, username: String): UserProfile {
        require(username.isNotBlank()) { "아이디를 입력하세요." }
        return request(
            idToken,
            "PUT",
            "/api/v1/profile/username",
            JSONObject().put("username", username.trim()).toString(),
        ).toUserProfile()
    }

    fun fetchDashboard(idToken: String): DashboardSnapshot {
        val json = request(idToken, "GET", "/api/v1/dashboard")
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
            buyHooked = json.optInt("buy_hooked"),
            sellHooked = json.optInt("sell_hooked"),
            minimumBuy = json.optDouble("minimum_buy"),
            registered = json.optJSONArray("registered").objects().map {
                CoinStatus(
                    market = it.getString("market"),
                    buyActive = it.optBoolean("buy_active"),
                    held = it.optBoolean("held"),
                    purchaseCost = it.optDouble("purchase_cost"),
                    currentValue = it.optDouble("current_value"),
                    changePercent = it.optDouble("change_percent"),
                )
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

    fun updateUpbitKeys(idToken: String, accessKey: String, secretKey: String) {
        require(accessKey.isNotBlank() && secretKey.isNotBlank()) { "Access Key와 Secret Key를 모두 입력하세요." }
        request(
            idToken,
            "PUT",
            "/api/v1/credentials",
            JSONObject().put("access_key", accessKey.trim()).put("secret_key", secretKey.trim()).toString(),
        )
    }

    private fun request(idToken: String, method: String, path: String, body: String? = null): JSONObject {
        require(idToken.isNotBlank()) { "Google 로그인이 필요합니다." }
        val connection = URL(SERVER_URL + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Authorization", "Bearer $idToken")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val errorCode = runCatching { JSONObject(text).optString("error") }.getOrDefault("")
            if (code == 401 || code == 403) {
                throw AuthenticationRequiredException("Google 인증을 다시 확인해야 합니다. 로그인해 주세요.")
            }
            if (code == 409 && errorCode == "username_taken") throw UsernameTakenException()
            if (code == 409 && errorCode == "username_already_set") throw UsernameAlreadySetException()
            if (code == 400 && errorCode == "invalid_username") throw UsernameInvalidException()
            if (code !in 200..299) error("서버 오류 $code: ${text.take(300)}")
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val SERVER_URL = "https://3.39.151.27.nip.io"
    }
}

private fun JSONObject.toUserProfile() = DashboardRepository.UserProfile(
    email = optString("email"),
    username = optString("username"),
    requiresUsername = optBoolean("requires_username", optString("username").isBlank()),
    requiresCredentials = optBoolean("requires_credentials"),
    disabled = optBoolean("disabled"),
)

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
private fun JSONObject.nullableString(name: String): String? = if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
private fun parseInstant(value: String): Instant = runCatching { Instant.parse(value) }.getOrDefault(Instant.EPOCH)
