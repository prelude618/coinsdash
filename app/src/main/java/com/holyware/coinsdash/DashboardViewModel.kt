package com.holyware.coinsdash

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holyware.coinsdash.data.DashboardRepository
import com.holyware.coinsdash.data.DashboardSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val snapshot: DashboardSnapshot? = null,
    val loading: Boolean = false,
    val authLoading: Boolean = false,
    val signedInEmail: String? = null,
    val authError: String? = null,
    val connectionError: String? = null,
    val lastSuccessfulRefresh: Long? = null,
    val consecutiveFailures: Int = 0,
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DashboardRepository()
    private val auth = GoogleAuthManager(application)
    private val mutableState = MutableStateFlow(DashboardUiState(signedInEmail = auth.currentEmail))
    val state: StateFlow<DashboardUiState> = mutableState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                refresh()
            }
        }
    }

    fun refresh() {
        if (mutableState.value.signedInEmail == null || mutableState.value.loading) return
        mutableState.value = mutableState.value.copy(loading = true, connectionError = null)
        viewModelScope.launch {
            runCatching {
                val token = auth.idToken()
                withContext(Dispatchers.IO) { repository.fetchDashboard(token) }
            }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        snapshot = it, loading = false, connectionError = null,
                        lastSuccessfulRefresh = System.currentTimeMillis(),
                        consecutiveFailures = 0,
                    )
                }
                .onFailure {
                    mutableState.value = mutableState.value.copy(
                        loading = false,
                        connectionError = it.message ?: "연결 실패",
                        consecutiveFailures = mutableState.value.consecutiveFailures + 1,
                    )
                }
        }
    }

    fun signIn(context: Context) {
        if (mutableState.value.authLoading) return
        mutableState.value = mutableState.value.copy(authLoading = true, authError = null)
        viewModelScope.launch {
            runCatching { GoogleAuthManager(context).signIn() }
                .onSuccess { email ->
                    mutableState.value = mutableState.value.copy(
                        signedInEmail = email,
                        authLoading = false,
                        authError = null,
                        connectionError = null,
                        consecutiveFailures = 0,
                    )
                    refresh()
                }
                .onFailure {
                    mutableState.value = mutableState.value.copy(authLoading = false, authError = it.message ?: "Google 로그인 실패")
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { auth.signOut() }
            mutableState.value = DashboardUiState()
        }
    }

    suspend fun updateKeys(accessKey: String, secretKey: String): Result<Unit> = runCatching {
        val token = auth.idToken()
        withContext(Dispatchers.IO) { repository.updateUpbitKeys(token, accessKey, secretKey) }
        refresh()
    }
}
