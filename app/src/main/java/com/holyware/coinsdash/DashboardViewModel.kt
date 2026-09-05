package com.holyware.coinsdash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holyware.coinsdash.data.ConnectionSettings
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
    val settings: ConnectionSettings = ConnectionSettings(),
    val loading: Boolean = false,
    val connectionError: String? = null,
    val lastSuccessfulRefresh: Long? = null,
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DashboardRepository(application)
    private val mutableState = MutableStateFlow(DashboardUiState(settings = repository.loadSettings()))
    val state: StateFlow<DashboardUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(5_000)
            }
        }
    }

    fun saveSettings(settings: ConnectionSettings) {
        repository.saveSettings(settings)
        mutableState.value = mutableState.value.copy(settings = repository.loadSettings(), connectionError = null)
        refresh()
    }

    fun refresh() {
        val settings = mutableState.value.settings
        if (settings.baseUrl.isBlank() || settings.dashboardToken.isBlank() || mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true)
            runCatching { withContext(Dispatchers.IO) { repository.fetchDashboard(settings) } }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        snapshot = it, loading = false, connectionError = null,
                        lastSuccessfulRefresh = System.currentTimeMillis(),
                    )
                }
                .onFailure { mutableState.value = mutableState.value.copy(loading = false, connectionError = it.message ?: "연결 실패") }
        }
    }

    suspend fun updateKeys(accessKey: String, secretKey: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { repository.updateUpbitKeys(mutableState.value.settings, accessKey, secretKey) }
        refresh()
    }
}
