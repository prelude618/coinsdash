package com.holyware.coinsdash

import com.holyware.coinsdash.data.BotStatus
import com.holyware.coinsdash.data.DashboardSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    private val email = "prelude618@gmail.com"

    @Test
    fun botStatusRemainsCheckingDuringRetries() {
        val healthy = DashboardSnapshot(bot = BotStatus(alive = true))

        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(signedInEmail = email, loading = true)))
        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(snapshot = healthy, signedInEmail = email, consecutiveFailures = 1)))
        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(snapshot = healthy, signedInEmail = email, consecutiveFailures = 2)))
        assertEquals(BotPresentation.OUTAGE, botPresentation(DashboardUiState(snapshot = healthy, signedInEmail = email, consecutiveFailures = 3)))
    }

    @Test
    fun successfulResponseClearsCheckingState() {
        val state = DashboardUiState(
            snapshot = DashboardSnapshot(bot = BotStatus(alive = true)),
            signedInEmail = email,
            consecutiveFailures = 0,
        )
        assertEquals(BotPresentation.HEALTHY, botPresentation(state))
    }

    @Test
    fun confirmedBotFailureIsShownImmediately() {
        val state = DashboardUiState(
            snapshot = DashboardSnapshot(bot = BotStatus(alive = false)),
            signedInEmail = email,
        )
        assertEquals(BotPresentation.OUTAGE, botPresentation(state))
    }

    @Test
    fun signedOutIsNotReportedAsOutage() {
        assertEquals(BotPresentation.NEEDS_LOGIN, botPresentation(DashboardUiState()))
    }

    @Test
    fun krwPrefixIsHiddenFromTradeMarketName() {
        assertEquals("B3", marketDisplayName("KRW-B3"))
        assertEquals("BTC-USDT", marketDisplayName("BTC-USDT"))
    }
}
