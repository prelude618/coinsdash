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
        val authenticated = AuthUiState(AuthStatus.AUTHENTICATED, email)

        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(auth = authenticated)))
        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(snapshot = healthy, auth = authenticated, connection = ConnectionUiState(status = ConnectionStatus.LOADING, consecutiveFailures = 1))))
        assertEquals(BotPresentation.CHECKING, botPresentation(DashboardUiState(snapshot = healthy, auth = authenticated, connection = ConnectionUiState(status = ConnectionStatus.LOADING, consecutiveFailures = 2))))
        assertEquals(BotPresentation.OUTAGE, botPresentation(DashboardUiState(snapshot = healthy, auth = authenticated, connection = ConnectionUiState(status = ConnectionStatus.ERROR, consecutiveFailures = 3))))
    }

    @Test
    fun successfulResponseClearsCheckingState() {
        val state = DashboardUiState(
            snapshot = DashboardSnapshot(bot = BotStatus(alive = true)),
            auth = AuthUiState(AuthStatus.AUTHENTICATED, email),
            connection = ConnectionUiState(ConnectionStatus.CONNECTED),
        )
        assertEquals(BotPresentation.HEALTHY, botPresentation(state))
    }

    @Test
    fun confirmedBotFailureIsShownImmediately() {
        val state = DashboardUiState(
            snapshot = DashboardSnapshot(bot = BotStatus(alive = false)),
            auth = AuthUiState(AuthStatus.AUTHENTICATED, email),
            connection = ConnectionUiState(ConnectionStatus.CONNECTED),
        )
        assertEquals(BotPresentation.OUTAGE, botPresentation(state))
    }

    @Test
    fun signedOutIsNotReportedAsOutage() {
        val signedOut = DashboardUiState(auth = AuthUiState(AuthStatus.SIGNED_OUT))
        assertEquals(BotPresentation.NEEDS_LOGIN, botPresentation(signedOut))
    }

    @Test
    fun defaultConnectionStateIsLoadingAndIndependentFromAuthentication() {
        val state = DashboardUiState(auth = AuthUiState(AuthStatus.AUTHENTICATED, email))
        assertEquals(ConnectionStatus.LOADING, state.connection.status)
        assertEquals(BotPresentation.CHECKING, botPresentation(state))
    }

    @Test
    fun staleBotFailureIsIgnoredWhileConnectionIsLoading() {
        val state = DashboardUiState(
            snapshot = DashboardSnapshot(bot = BotStatus(alive = false)),
            auth = AuthUiState(AuthStatus.AUTHENTICATED, email),
            connection = ConnectionUiState(ConnectionStatus.LOADING),
        )
        assertEquals(BotPresentation.CHECKING, botPresentation(state))
    }

    @Test
    fun connectionErrorRequiresThreeConsecutiveFailures() {
        val failure = IllegalStateException("network down")
        val first = connectionAfterFailure(ConnectionUiState(), failure)
        val second = connectionAfterFailure(first, failure)
        val third = connectionAfterFailure(second, failure)

        assertEquals(ConnectionStatus.LOADING, first.status)
        assertEquals(null, first.error)
        assertEquals(ConnectionStatus.LOADING, second.status)
        assertEquals(null, second.error)
        assertEquals(ConnectionStatus.ERROR, third.status)
        assertEquals("network down", third.error)
        assertEquals(3, third.consecutiveFailures)
    }

    @Test
    fun krwPrefixIsHiddenFromTradeMarketName() {
        assertEquals("B3", marketDisplayName("KRW-B3"))
        assertEquals("BTC-USDT", marketDisplayName("BTC-USDT"))
    }
}
