package com.holyware.coinsdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.holyware.coinsdash.ui.CoinSDashApp
import com.holyware.coinsdash.ui.theme.CoinSDashTheme

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CoinSDashTheme { CoinSDashApp(dashboardViewModel) } }
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel.onForeground()
    }
}
