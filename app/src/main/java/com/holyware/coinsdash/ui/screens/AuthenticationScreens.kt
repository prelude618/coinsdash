package com.holyware.coinsdash.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.holyware.coinsdash.DashboardUiState
import com.holyware.coinsdash.ui.theme.CoinSDashTheme
import kotlinx.coroutines.launch

@Composable
internal fun AuthenticationLoadingScreen(error: String? = null, onRetry: () -> Unit = {}, onSignOut: () -> Unit = {}) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(if (error == null) "Google 로그인 확인 중" else "사용자 정보 확인 실패")
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("다시 시도") }
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("로그아웃") }
            }
        }
    }
}

internal fun validUsernameInput(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.length in 3..20 && trimmed.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
}

@Composable
internal fun UsernameSetupScreen(
    email: String,
    onSave: suspend (String) -> Result<Unit>,
) {
    var username by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("아이디 설정", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Text("Coinsdance에서 사용할 아이디를 입력하세요. 아이디 설정은 필수이며 이후 변경할 수 없습니다.")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("아이디 (필수)") },
                supportingText = { Text("3~20자 · 한글/영문/숫자/./_/- 사용 가능") },
                singleLine = true,
                isError = username.isNotEmpty() && !validUsernameInput(username),
                enabled = !saving,
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !saving && validUsernameInput(username),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        onSave(username.trim())
                            .onFailure { error = it.message ?: "아이디를 설정하지 못했습니다." }
                        saving = false
                    }
                },
            ) { Text(if (saving) "확인 중…" else "아이디 설정 완료") }
        }
    }
}

@Composable
internal fun LoginScreen(state: DashboardUiState, onSignIn: (Context) -> Unit) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Coinsdance", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("CoinSDance 실시간 관제", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { onSignIn(context) }, modifier = Modifier.fillMaxWidth()) { Text("Google로 로그인") }
            if (!state.auth.error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(state.auth.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    CoinSDashTheme(dynamicColor = false) { LoginScreen(DashboardUiState()) {} }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun UsernameSetupScreenPreview() {
    CoinSDashTheme(dynamicColor = false) { UsernameSetupScreen("preview@coinsdance.app") { Result.success(Unit) } }
}
