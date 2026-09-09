package com.holyware.coinsdash

import com.holyware.coinsdash.data.BotStatus
import com.holyware.coinsdash.data.ConnectionSettings
import com.holyware.coinsdash.data.DashboardSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    private val settings = ConnectionSettings("https://dash.example.com", "token")

    @Test
    fun botStatusRemainsCheckingDuringRetries() {
        val healthy = DashboardSnapshot(bot = BotStatus(alive = true))

        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(settings = settings, loading = true)))
        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(snapshot = healthy, settings = settings, consecutiveFailures = 1)))
        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(snapshot = healthy, settings = settings, consecutiveFailures = 2)))
        assertEquals(BotPresentation.OUTAGE, botPresentation(DashboardUiState(snapshot = healthy, settings = settings, consecutiveFailures = 3)))
    }

    @Test
    fun successfulResponseClearsCheckingState() {
        val state = DashboardUiState(
            snapshot = DashboardSnapshot(bot = BotStatus(alive = true)),
            settings = settings,
            consecutiveFailures = 0,
        )
        assertEquals(BotPresentation.HEALTHY, botPresentation(state))
    }

    @Test
    fun confirmedBotFailureIsShownImmediately() {
        val state = DashboardUiState(
            snapshot = DashboardSnapshot(bot = BotStatus(alive = false)),
            settings = settings,
        )
        assertEquals(BotPresentation.OUTAGE, botPresentation(state))
    }

    @Test
    fun missingSettingsIsNotReportedAsOutage() {
        assertEquals(BotPresentation.NEEDS_SETTINGS, botPresentation(DashboardUiState()))
    }
}
