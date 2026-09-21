package com.holyware.coinsdash.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.holyware.coinsdash.DashboardUiState
import com.holyware.coinsdash.ui.preview.PreviewData
import com.holyware.coinsdash.ui.theme.CoinSDashTheme
import kotlinx.coroutines.launch

@Composable
internal fun SettingsScreen(
    state: DashboardUiState,
    onSignOut: () -> Unit,
    onUpdateKeys: suspend (String, String) -> Result<Unit>,
) {
    var showKeys by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Google 계정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { Text("아이디: ${state.auth.username.orEmpty()}", fontWeight = FontWeight.Bold) }
        item { Text(state.auth.email.orEmpty(), fontWeight = FontWeight.Bold) }
        item { OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("로그아웃") } }
        item { HorizontalDivider() }
        item { Text("업비트 API 키", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Text("키는 앱에 저장하지 않고 HTTPS로 서버에 한 번 전달합니다. 서버는 새 키를 검증한 후 교체해야 합니다.", style = MaterialTheme.typography.bodySmall) }
        item { OutlinedButton(onClick = { showKeys = true }, modifier = Modifier.fillMaxWidth()) { Text("API 키 갱신") } }
    }
    if (showKeys) KeyDialog(onUpdateKeys) { showKeys = false }
}

@Composable
private fun KeyDialog(onUpdateKeys: suspend (String, String) -> Result<Unit>, dismiss: () -> Unit) {
    var access by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!saving) dismiss() },
        title = { Text("업비트 API 키 갱신") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(access, { access = it }, label = { Text("Access Key") }, singleLine = true)
                OutlinedTextField(secret, { secret = it }, label = { Text("Secret Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(enabled = !saving && access.isNotBlank() && secret.isNotBlank(), onClick = {
                saving = true
                scope.launch {
                    onUpdateKeys(access, secret).onSuccess { access = ""; secret = ""; dismiss() }.onFailure { error = it.message }
                    saving = false
                }
            }) { Text(if (saving) "검증 중…" else "검증 후 교체") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text("취소") } },
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun SettingsScreenPreview() {
    CoinSDashTheme(dynamicColor = false) { SettingsScreen(PreviewData.state, {}, { _, _ -> Result.success(Unit) }) }
}
