package com.holyware.coinsdash

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holyware.coinsdash.data.AuthenticationRequiredException
import com.holyware.coinsdash.data.DashboardRepository
import com.holyware.coinsdash.data.DashboardSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class AuthStatus { CHECKING, USERNAME_REQUIRED, AUTHENTICATED, SIGNED_OUT }

internal data class AuthUiState(
    val status: AuthStatus = AuthStatus.CHECKING,
    val email: String? = null,
    val username: String? = null,
    val error: String? = null,
)

internal enum class ConnectionStatus { LOADING, CONNECTED, ERROR }

internal data class ConnectionUiState(
    val status: ConnectionStatus = ConnectionStatus.LOADING,
    val refreshing: Boolean = false,
    val error: String? = null,
    val lastSuccessfulRefresh: Long? = null,
    val consecutiveFailures: Int = 0,
)

internal data class DashboardUiState(
    val snapshot: DashboardSnapshot? = null,
    val auth: AuthUiState = AuthUiState(),
    val connection: ConnectionUiState = ConnectionUiState(),
)

internal const val CONNECTION_FAILURE_LIMIT = 3

internal fun connectionAfterFailure(current: ConnectionUiState, error: Throwable): ConnectionUiState {
    val failures = current.consecutiveFailures + 1
    return current.copy(
        status = if (failures >= CONNECTION_FAILURE_LIMIT) ConnectionStatus.ERROR else ConnectionStatus.LOADING,
        refreshing = false,
        error = if (failures >= CONNECTION_FAILURE_LIMIT) error.message ?: "연결 실패" else null,
        consecutiveFailures = failures,
    )
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DashboardRepository()
    private val authManager = GoogleAuthManager(application)
    private val mutableState = MutableStateFlow(DashboardUiState())
    internal val state: StateFlow<DashboardUiState> = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        initializeAuthentication()
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                refresh()
            }
        }
    }

    private fun initializeAuthentication() {
        val email = authManager.currentEmail
        if (email == null) {
            mutableState.value = DashboardUiState(auth = AuthUiState(status = AuthStatus.SIGNED_OUT))
            return
        }
        loadProfile(email)
    }

    private fun loadProfile(email: String) {
        mutableState.value = DashboardUiState(auth = AuthUiState(status = AuthStatus.CHECKING, email = email))
        viewModelScope.launch {
            runCatching {
                val token = authManager.idToken()
                withContext(Dispatchers.IO) { repository.fetchProfile(token) }
            }.onSuccess { profile ->
                mutableState.value = DashboardUiState(
                    auth = AuthUiState(
                        status = if (profile.requiresUsername) AuthStatus.USERNAME_REQUIRED else AuthStatus.AUTHENTICATED,
                        email = profile.email.ifBlank { email },
                        username = profile.username.ifBlank { null },
                    ),
                )
                if (!profile.requiresUsername) refresh()
            }.onFailure { error ->
                if (error is AuthenticationRequiredException) {
                    handleAuthenticationFailure(error.message)
                } else {
                    mutableState.value = DashboardUiState(
                        auth = AuthUiState(
                            status = AuthStatus.CHECKING,
                            email = email,
                            error = error.message ?: "사용자 정보를 확인하지 못했습니다.",
                        ),
                    )
                }
            }
        }
    }

    fun retryProfile() {
        authManager.currentEmail?.let(::loadProfile) ?: run {
            mutableState.value = DashboardUiState(auth = AuthUiState(status = AuthStatus.SIGNED_OUT))
        }
    }

    fun refresh() {
        val current = mutableState.value
        if (current.auth.status != AuthStatus.AUTHENTICATED || current.connection.refreshing) return

        mutableState.value = current.copy(
            connection = current.connection.copy(
                status = if (current.connection.status == ConnectionStatus.CONNECTED) {
                    ConnectionStatus.CONNECTED
                } else {
                    ConnectionStatus.LOADING
                },
                refreshing = true,
                error = null,
            ),
        )
        refreshJob = viewModelScope.launch {
            runCatching {
                val token = authManager.idToken()
                withContext(Dispatchers.IO) { repository.fetchDashboard(token) }
            }.onSuccess { snapshot ->
                if (mutableState.value.auth.status != AuthStatus.AUTHENTICATED) return@onSuccess
                mutableState.value = mutableState.value.copy(
                    snapshot = snapshot,
                    connection = ConnectionUiState(
                        status = ConnectionStatus.CONNECTED,
                        lastSuccessfulRefresh = System.currentTimeMillis(),
                    ),
                )
            }.onFailure { error ->
                // Activity resume, logout, and a new authentication attempt all
                // cancel an obsolete request. Cancellation is control flow, not
                // a network failure, and must never increment the error count.
                if (error is CancellationException) return@onFailure
                if (error is AuthenticationRequiredException) {
                    handleAuthenticationFailure(error.message)
                    return@onFailure
                }
                mutableState.value = mutableState.value.copy(
                    connection = connectionAfterFailure(mutableState.value.connection, error),
                )
            }
        }
    }

    fun onForeground() {
        val current = mutableState.value
        if (current.auth.status != AuthStatus.AUTHENTICATED) return
        refreshJob?.cancel()
        mutableState.value = current.copy(
            connection = ConnectionUiState(status = ConnectionStatus.LOADING),
        )
        refresh()
    }

    fun signIn(context: Context) {
        if (mutableState.value.auth.status == AuthStatus.CHECKING) return
        refreshJob?.cancel()
        mutableState.value = DashboardUiState(auth = AuthUiState(status = AuthStatus.CHECKING))
        viewModelScope.launch {
            runCatching { GoogleAuthManager(context).signIn() }
                .onSuccess { email ->
                    loadProfile(email)
                }
                .onFailure { error ->
                    mutableState.value = DashboardUiState(
                        auth = AuthUiState(
                            status = AuthStatus.SIGNED_OUT,
                            error = error.message ?: "Google 로그인 실패",
                        ),
                    )
                }
        }
    }

    fun signOut() {
        refreshJob?.cancel()
        mutableState.value = DashboardUiState(auth = AuthUiState(status = AuthStatus.CHECKING))
        viewModelScope.launch {
            runCatching { authManager.signOut() }
            mutableState.value = DashboardUiState(auth = AuthUiState(status = AuthStatus.SIGNED_OUT))
        }
    }

    suspend fun updateKeys(accessKey: String, secretKey: String): Result<Unit> = runCatching {
        val token = authManager.idToken()
        withContext(Dispatchers.IO) { repository.updateUpbitKeys(token, accessKey, secretKey) }
        refresh()
    }.onFailure { error ->
        if (error is AuthenticationRequiredException) handleAuthenticationFailure(error.message)
    }

    suspend fun setUsername(username: String): Result<Unit> = runCatching {
        val token = authManager.idToken()
        val profile = withContext(Dispatchers.IO) { repository.setUsername(token, username) }
        mutableState.value = DashboardUiState(
            auth = AuthUiState(
                status = AuthStatus.AUTHENTICATED,
                email = profile.email.ifBlank { authManager.currentEmail },
                username = profile.username,
            ),
        )
        refresh()
    }.onFailure { error ->
        if (error is AuthenticationRequiredException) handleAuthenticationFailure(error.message)
    }

    private fun handleAuthenticationFailure(message: String?) {
        refreshJob?.cancel()
        viewModelScope.launch { runCatching { authManager.signOut() } }
        mutableState.value = DashboardUiState(
            auth = AuthUiState(
                status = AuthStatus.SIGNED_OUT,
                error = message ?: "Google 인증이 만료되었습니다. 다시 로그인하세요.",
            ),
        )
    }
}
