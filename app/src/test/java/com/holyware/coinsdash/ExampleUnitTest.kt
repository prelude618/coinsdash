package com.holyware.coinsdash

import com.holyware.coinsdash.data.BotStatus
import com.holyware.coinsdash.data.CoinStatus
import com.holyware.coinsdash.data.DashboardSnapshot
import com.holyware.coinsdash.ui.components.marketDisplayName
import com.holyware.coinsdash.ui.screens.BooleanColumnFilter
import com.holyware.coinsdash.ui.screens.BotPresentation
import com.holyware.coinsdash.ui.screens.CoinListUiState
import com.holyware.coinsdash.ui.screens.CoinSort
import com.holyware.coinsdash.ui.screens.CoinSortField
import com.holyware.coinsdash.ui.screens.SortDirection
import com.holyware.coinsdash.ui.screens.botPresentation
import com.holyware.coinsdash.ui.screens.buyTrackingDisplay
import com.holyware.coinsdash.ui.screens.coinSearchSuggestions
import com.holyware.coinsdash.ui.screens.filterAndSortCoins
import com.holyware.coinsdash.ui.screens.sellTrackingDisplay
import com.holyware.coinsdash.ui.screens.toggleCoinSort
import com.holyware.coinsdash.ui.screens.validUsernameInput
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    private val email = "prelude618@gmail.com"

    @Test
    fun usernameValidationMatchesSignupRules() {
        assertEquals(true, validUsernameInput("prelude_618"))
        assertEquals(true, validUsernameInput("코인댄스"))
        assertEquals(false, validUsernameInput("ab"))
        assertEquals(false, validUsernameInput("contains space"))
        assertEquals(false, validUsernameInput("bad!"))
    }

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

    @Test
    fun trackingCardsShowLiveTargetSearchOutsideAndAverageComparisonInside() {
        val snapshot = DashboardSnapshot(
            buyTracking = 31,
            sellTracking = 47,
            buyHooked = 2,
            sellHooked = 5,
        )

        assertEquals(2 to 31, buyTrackingDisplay(snapshot))
        assertEquals(5 to 47, sellTrackingDisplay(snapshot))
    }

    @Test
    fun coinTableFiltersHoldingAndBuyTargetIndependently() {
        val coins = listOf(
            CoinStatus("KRW-AAVE", buyActive = true, held = true, purchaseCost = 300_000.0, currentValue = 240_000.0, changePercent = -20.0),
            CoinStatus("KRW-BTC", buyActive = false, held = true, purchaseCost = 200_000.0, currentValue = 220_000.0, changePercent = 10.0),
            CoinStatus("KRW-ETH", buyActive = true, held = false),
        )

        val filtered = filterAndSortCoins(
            coins = coins,
            held = BooleanColumnFilter.YES,
            buyActive = BooleanColumnFilter.YES,
        )

        assertEquals(listOf("KRW-AAVE"), filtered.map { it.market })
    }

    @Test
    fun coinListSessionDefaultsToHeldCoinsByPurchaseCostDescending() {
        val state = CoinListUiState()

        assertEquals(BooleanColumnFilter.YES, state.heldFilter)
        assertEquals(BooleanColumnFilter.ALL, state.buyActiveFilter)
        assertEquals(
            listOf(CoinSort(CoinSortField.PURCHASE_COST, SortDirection.DESCENDING)),
            state.sorts,
        )
    }

    @Test
    fun numericColumnsSupportOrderedMultiColumnSorting() {
        val coins = listOf(
            CoinStatus("KRW-A", false, true, 100.0, 80.0, -20.0),
            CoinStatus("KRW-B", true, false, 200.0, 250.0, 25.0),
            CoinStatus("KRW-C", true, true, 200.0, 300.0, 10.0),
        )

        val sorted = filterAndSortCoins(
            coins,
            sorts = listOf(
                CoinSort(CoinSortField.PURCHASE_COST, SortDirection.DESCENDING),
                CoinSort(CoinSortField.CURRENT_VALUE, SortDirection.ASCENDING),
            ),
        )

        assertEquals(listOf("KRW-B", "KRW-C", "KRW-A"), sorted.map { it.market })
    }

    @Test
    fun coinsAreAlphabeticalWhenNoNumericSortIsSelected() {
        val coins = listOf(
            CoinStatus("KRW-XRP", buyActive = false, held = false),
            CoinStatus("KRW-AAVE", buyActive = false, held = false),
            CoinStatus("KRW-BTC", buyActive = false, held = false),
        )

        val sorted = filterAndSortCoins(coins, sorts = emptyList())

        assertEquals(listOf("KRW-AAVE", "KRW-BTC", "KRW-XRP"), sorted.map { it.market })
    }

    @Test
    fun coinSearchMatchesOnlyFromFirstCharacterCaseInsensitively() {
        val coins = listOf(
            CoinStatus("KRW-BTC", buyActive = false, held = false, purchaseCost = 123_456.0),
            CoinStatus("KRW-WBTC", buyActive = false, held = false),
            CoinStatus("KRW-ETH", buyActive = false, held = false, purchaseCost = 123_456.0),
        )

        val result = filterAndSortCoins(coins, searchQuery = "bt")

        assertEquals(listOf("KRW-BTC"), result.map { it.market })
    }

    @Test
    fun coinSearchSuggestionsArePrefixMatchedAndBounded() {
        val coins = (0..20).map {
            CoinStatus("KRW-A${it.toString().padStart(2, '0')}", buyActive = false, held = false)
        } + CoinStatus("KRW-XA", buyActive = false, held = false)

        val result = coinSearchSuggestions(coins, "a")

        assertEquals(8, result.size)
        assertEquals((0..7).map { "KRW-A${it.toString().padStart(2, '0')}" }, result.map { it.market })
    }

    @Test
    fun sortHeaderCyclesDescendingAscendingAndOffWithoutRemovingOtherSorts() {
        val first = toggleCoinSort(emptyList(), CoinSortField.PURCHASE_COST)
        val withSecond = toggleCoinSort(first, CoinSortField.CURRENT_VALUE)
        val ascendingFirst = toggleCoinSort(withSecond, CoinSortField.PURCHASE_COST)
        val removedFirst = toggleCoinSort(ascendingFirst, CoinSortField.PURCHASE_COST)

        assertEquals(listOf(CoinSortField.PURCHASE_COST, CoinSortField.CURRENT_VALUE), withSecond.map { it.field })
        assertEquals(SortDirection.ASCENDING, ascendingFirst.first().direction)
        assertEquals(listOf(CoinSortField.CURRENT_VALUE), removedFirst.map { it.field })
    }
}
