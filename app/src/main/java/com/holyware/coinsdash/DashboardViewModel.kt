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

internal enum class AuthStatus { CHECKING, USERNAME_REQUIRED, CREDENTIALS_REQUIRED, APPROVAL_REQUIRED, AUTHENTICATED, SIGNED_OUT }

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
    private var approvalJob: Job? = null

    init {
        initializeAuthentication()
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                if (mutableState.value.auth.status == AuthStatus.APPROVAL_REQUIRED) {
                    checkApproval()
                } else {
                    refresh()
                }
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
                        status = profile.authStatus(),
                        email = profile.email.ifBlank { email },
                        username = profile.username.ifBlank { null },
                        error = if (profile.disabled) "관리자에 의해 사용이 중지된 계정입니다." else null,
                    ),
                )
                if (profile.authStatus() == AuthStatus.AUTHENTICATED) refresh()
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

    private fun checkApproval() {
        if (approvalJob?.isActive == true) return
        val current = mutableState.value
        if (current.auth.status != AuthStatus.APPROVAL_REQUIRED) return
        approvalJob = viewModelScope.launch {
            runCatching {
                val token = authManager.idToken()
                withContext(Dispatchers.IO) { repository.fetchProfile(token) }
            }.onSuccess { profile ->
                if (profile.disabled) {
                    handleAuthenticationFailure("관리자에 의해 사용이 중지된 계정입니다.")
                } else if (profile.authStatus() == AuthStatus.AUTHENTICATED) {
                    mutableState.value = DashboardUiState(
                        auth = current.auth.copy(status = AuthStatus.AUTHENTICATED),
                    )
                    refresh()
                }
            }.onFailure { error ->
                if (error is AuthenticationRequiredException) handleAuthenticationFailure(error.message)
            }
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
        if (current.auth.status == AuthStatus.APPROVAL_REQUIRED) {
            checkApproval()
            return
        }
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
        approvalJob?.cancel()
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
        approvalJob?.cancel()
        mutableState.value = DashboardUiState(auth = AuthUiState(status = AuthStatus.CHECKING))
        viewModelScope.launch {
            runCatching { authManager.signOut() }
            mutableState.value = DashboardUiState(auth = AuthUiState(status = AuthStatus.SIGNED_OUT))
        }
    }

    suspend fun updateKeys(accessKey: String, secretKey: String): Result<Unit> = runCatching {
        val token = authManager.idToken()
        val profile = withContext(Dispatchers.IO) { repository.updateUpbitKeys(token, accessKey, secretKey) }
        mutableState.value = DashboardUiState(
            auth = mutableState.value.auth.copy(status = profile.authStatus()),
        )
        if (profile.authStatus() == AuthStatus.AUTHENTICATED) refresh()
    }.onFailure { error ->
        if (error is AuthenticationRequiredException) handleAuthenticationFailure(error.message)
    }

    suspend fun setUsername(username: String): Result<Unit> = runCatching {
        val token = authManager.idToken()
        val profile = withContext(Dispatchers.IO) { repository.setUsername(token, username) }
        mutableState.value = DashboardUiState(
            auth = AuthUiState(
                status = profile.authStatus(),
                email = profile.email.ifBlank { authManager.currentEmail },
                username = profile.username,
            ),
        )
        if (profile.authStatus() == AuthStatus.AUTHENTICATED) refresh()
    }.onFailure { error ->
        if (error is AuthenticationRequiredException) handleAuthenticationFailure(error.message)
    }

    private fun handleAuthenticationFailure(message: String?) {
        refreshJob?.cancel()
        approvalJob?.cancel()
        viewModelScope.launch { runCatching { authManager.signOut() } }
        mutableState.value = DashboardUiState(
            auth = AuthUiState(
                status = AuthStatus.SIGNED_OUT,
                error = message ?: "Google 인증이 만료되었습니다. 다시 로그인하세요.",
            ),
        )
    }
}

internal fun DashboardRepository.UserProfile.authStatus(): AuthStatus = when {
    disabled -> AuthStatus.SIGNED_OUT
    requiresUsername -> AuthStatus.USERNAME_REQUIRED
    requiresCredentials -> AuthStatus.CREDENTIALS_REQUIRED
    !approved -> AuthStatus.APPROVAL_REQUIRED
    else -> AuthStatus.AUTHENTICATED
}
