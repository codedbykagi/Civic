package com.civic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.civic.app.ui.navigation.CivicNavHost
import com.civic.app.ui.theme.CivicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CivicTheme {
                CivicNavHost()
            }
        }
    }
}
