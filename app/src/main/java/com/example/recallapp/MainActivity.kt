package com.example.recallapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recallapp.ui.AppNavHost
import com.example.recallapp.ui.theme.RecallTheme

/** Single activity that hosts every Compose screen. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RecallApplication
        setContent { RecallTheme { AppNavHost(auth = app.auth, tokens = app.tokens) } }
    }
}
