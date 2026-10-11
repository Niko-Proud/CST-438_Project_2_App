package com.example.recallapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recallapp.ui.login.LoginScreen
import com.example.recallapp.ui.theme.RecallTheme

/** Single activity. For now it shows the landing (login) screen UI. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RecallTheme { LoginScreen(loading = false, error = null, onSignInClick = {}) } }
    }
}
