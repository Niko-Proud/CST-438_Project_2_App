package com.example.recallapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recallapp.ui.AppNavHost
import com.example.recallapp.ui.theme.RecallTheme
import com.example.recallapp.RecallApplication

/**
 * The app's single Android activity.
 *
 * Hosts the Compose UI and passes the application's authentication
 * dependencies to the navigation graph.
 */
class MainActivity : ComponentActivity() {

    /**
     * Creates the activity's UI.
     *
     * Uses one setContent call so the real login and navigation flow
     * cannot be replaced by a second UI entry point.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allows screens to draw behind the system bars.
        // Individual screens handle their own safe-area padding.
        enableEdgeToEdge()

        // Reuses the authentication provider and token store
        // owned by the application.
        val app = application as RecallApplication

        setContent {
            // Applies the same colors, typography, and shapes to all screens.
            RecallTheme {
                AppNavHost(
                    auth = app.auth,
                    tokens = app.tokens,
                )
            }
        }
    }
}